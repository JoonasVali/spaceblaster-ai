package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.llm.LLMClient;
import com.github.joonasvali.spaceblaster.aitalker.llm.Response;
import com.github.joonasvali.spaceblaster.aitalker.llm.Text;
import com.github.joonasvali.spaceblaster.aitalker.sound.AudioTrackBuilder;
import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.EventType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpaceTalker {
  public static final String SYSTEM_MESSAGE = """
      Space Blaster is a modern space invaders clone where player controls a spaceship and shoots enemies. 
      The game has multiple levels and the player can collect power-ups, which randomly gives the player a new weapon. 
      Player sometimes does not want a power-up, because it might be worse than their current weapon.
      The initial weapon the player has is always a cannon. The enemies are similar, but they have different weapons. 
      They also need to be hit with a different amount of damage to be destroyed. If player dies, 
      then the player is invincible for a short period and their weapon is defaulted back to cannon.
      Space Blaster episode consists of multiple levels, you are commenting on a single episode. The gameplay is continuous, 
      once the player completes a level, the next level starts soon as enemies are born, without a pause in the game.

      In a context of this game, you are commenting based on the events from the game to the spectators. 
      (Your written commentary will be later synthesized into a voice and spectators will see the game from a video).
      Write all your commentary without quotes, do not use any style indicators or other indicators which would not 
      render well in a voice synthesis.""";
  public static final String RESOLUTE_SHORTER_MESSAGE_INSTRUCTION = "WRITE MUCH MUCH SHORTER MESSAGE!!!";

  public static final String OUTPUT_SOUND_FILE_SUFFIX = ".wav";
  public static final int SHORTENING_FAILURES_ALLOWED = 3;
  public static final int SHORTENING_RESOLUTE_THRESHOLD = 2;
  public static final String OUTPUT_SOUND_FILE_NAME = "final";
  public static final long LATENCY_THRESHOLD_MS = 300;
  public static final long MEDIUM_PERIOD_THRESHOLD_MS = 4000;
  private final SoundDurationEvaluator soundDurationEvaluator;
  private final LLMClient llmClient;
  private final TextToSpeechClient textToSpeechClient;
  private final AudioTrackBuilder audioTrackBuilder;
  private final Path outputRootDirectory;
  private final ArrayList<SpaceTalkListener> listeners = new ArrayList<>();

  public SpaceTalker(TextToSpeechClient textToSpeechClient, LLMClient llmClient, Path outputRootDirectory) {
    this.outputRootDirectory = outputRootDirectory;
    this.llmClient = llmClient;
    this.soundDurationEvaluator = textToSpeechClient.getSoundDurationEvaluator();
    this.textToSpeechClient = textToSpeechClient;
    this.audioTrackBuilder = new AudioTrackBuilder(textToSpeechClient.getOutputSettings().getSampleRate(), true);
    if (textToSpeechClient.getSpaceTalkListener() != null) {
      this.listeners.add(textToSpeechClient.getSpaceTalkListener());
    }
    if (llmClient.getSpaceTalkListener() != null) {
      this.listeners.add(llmClient.getSpaceTalkListener());
    }
  }

  public SpaceTalk run(List<Period> periods, String playerName, String projectName) throws IOException {
    llmClient.setSystemMessage(SYSTEM_MESSAGE + "\n" + textToSpeechClient.getCommentatorDescription());

    Period period = periods.getFirst();

    Text input;
    VoiceCommentaryRepository commentaryRepository;
    {
      Event firstEvent = period.getEvent();
      input = getGameIntroductionInstructions(firstEvent, playerName, period.getDuration());

      commentaryRepository = new VoiceCommentaryRepository(period.getEvent().eventTimestamp, projectName);

      CommentaryContext context = new CommentaryContext(period, 0, input, 0);
      Commentary commentary = produceCommentary(commentaryRepository, context);
      llmClient.commitHistoryBySquashing();
      notifyPeriodCompletedListeners(commentary.text, 0);
    }

    long latency = 0;

    for (int i = 1; i < periods.size(); i++) {
      Period lastPeriod = period;
      List<Event> secondaryEventsFromLastPeriod = lastPeriod.getSecondaryEvents();
      period = periods.get(i);
      long latencyReduction = 0;
      if (latency > LATENCY_THRESHOLD_MS && period.getDuration() > MEDIUM_PERIOD_THRESHOLD_MS) {
        // replace period with time debt adjusted one to reduce latency.
        latencyReduction = Math.min(period.getDuration() / 4, latency);
        long newPeriodDuration = period.getDuration() - latencyReduction;
        period = new Period(period.getEvent(), new ArrayList<>(period.getSecondaryEvents()), newPeriodDuration);
      }

      input = getEventInstructions(msToSeconds(lastPeriod.getPeriodDuration() + latency), period, secondaryEventsFromLastPeriod, latency);

      CommentaryContext context = new CommentaryContext(period, periods.get(i), i, input, latency, latencyReduction);
      Commentary commentary = produceCommentary(commentaryRepository, context);
      llmClient.commitHistoryBySquashing();
      notifyPeriodCompletedListeners(commentary.text, i);

      latency = Math.max(latency - latencyReduction + commentary.duration - period.getPeriodDuration(), 0);
    }

    Path finalSoundFile = commentaryRepository.soundOutputDir.resolve(OUTPUT_SOUND_FILE_NAME + OUTPUT_SOUND_FILE_SUFFIX);
    var voices = audioTrackBuilder.produce(finalSoundFile);
    return new SpaceTalk(finalSoundFile, voices);
  }

  private static Long msToSeconds(Long duration) {
    return (long) Math.round(duration / 1000f);
  }

  private record Commentary(String text, Long duration) {
  }

  private Commentary produceCommentary(VoiceCommentaryRepository commentaryRepository, CommentaryContext context) throws IOException {
    Response response = llmClient.run(context.instructions);
    long evaluatedDuration = soundDurationEvaluator.evaluateDurationInMs(response.outputMessage());

    Period period = context.period;
    long latency = context.latency;

    String lastOutputMessage = response.outputMessage();
    long eventTimeStamp = period.getEvent().getEventTimestamp();
    long limitDuration = period.getDuration();

    long audioFileDuration = 0;
    if (evaluatedDuration <= limitDuration) {
      audioFileDuration = commentaryRepository.addSoundConditionally(lastOutputMessage, eventTimeStamp + latency, period.getDuration(), period.getPeriodDuration() - latency);
    }

    int failsToShorten = 0;

    while (failsToShorten < SHORTENING_FAILURES_ALLOWED && (evaluatedDuration > limitDuration || audioFileDuration > limitDuration)) {
      notifyCommentaryFailedListeners(lastOutputMessage, failsToShorten);
      context.addRejectedCommentary(lastOutputMessage, evaluatedDuration, audioFileDuration);

      Response anotherResponse;
      if (failsToShorten >= SHORTENING_RESOLUTE_THRESHOLD) {
        notifyResoluteShorteningMessage(lastOutputMessage, evaluatedDuration, limitDuration, failsToShorten);
        anotherResponse = llmClient.run(new Text(RESOLUTE_SHORTER_MESSAGE_INSTRUCTION, ""));
      } else {
        anotherResponse = getShorteningMessage(Math.max(evaluatedDuration, audioFileDuration), limitDuration);
      }

      lastOutputMessage = anotherResponse.outputMessage();
      evaluatedDuration = soundDurationEvaluator.evaluateDurationInMs(anotherResponse.outputMessage());

      boolean exceedsEvaluatedDuration = evaluatedDuration > limitDuration;

      if (!exceedsEvaluatedDuration) {
        audioFileDuration = commentaryRepository.addSoundConditionally(lastOutputMessage, eventTimeStamp + latency, period.getDuration(), period.getPeriodDuration() - latency);
      }

      if (exceedsEvaluatedDuration || audioFileDuration > period.getDuration()) {
        failsToShorten++;
        notifyFailToShortenSpeechListeners(lastOutputMessage, failsToShorten);
      }
    }

    if (failsToShorten >= SHORTENING_FAILURES_ALLOWED) {
      notifyAbandonShortenSpeechListeners(lastOutputMessage, failsToShorten);
      var result = commentaryRepository.addSound(lastOutputMessage, eventTimeStamp + latency, period.getDuration(), period.getPeriodDuration() - latency);
      audioFileDuration = result.soundDurationInTrack;
    }

    return new Commentary(lastOutputMessage, audioFileDuration);
  }

  private void notifyFailToShortenSpeechListeners(String output, int attempt) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      long timeSinceEvent = System.currentTimeMillis() - time;
      s.onFailToShortenSpeech(output, attempt, timeSinceEvent);
    });
  }

  private void notifyAbandonShortenSpeechListeners(String output, int attempt) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      long timeSinceEvent = System.currentTimeMillis() - time;
      s.onAbandonShortenSpeech(output, attempt, timeSinceEvent);
    });
  }


  private void notifyPeriodCompletedListeners(String output, int periodIndex) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      long timeSinceEvent = System.currentTimeMillis() - time;
      s.onPeriodProcessingCompleted(output, periodIndex, timeSinceEvent);
    });
  }

  private void notifyCommentaryFailedListeners(String output, int attempt) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      long timeSinceEvent = System.currentTimeMillis() - time;
      s.onCommentaryFailed(output, attempt, timeSinceEvent);
    });
  }

  private void notifyResoluteShorteningMessage(String output, long duration, long limitDuration, int attempt) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      long timeSinceEvent = System.currentTimeMillis() - time;
      s.onResoluteShorteningMessage(output, duration, limitDuration, attempt, timeSinceEvent);
    });
  }

  public void addListener(SpaceTalkListener listener) {
    listeners.add(listener);
  }

  public void removeListener(SpaceTalkListener listener) {
    listeners.remove(listener);
  }

  private class VoiceCommentaryRepository {
    private final Path soundOutputDir;
    private int index;
    private final long startTime;
    private final List<String> requestIds = new ArrayList<>();

    public VoiceCommentaryRepository(Long startTime, String folderName) throws IOException {
      soundOutputDir = outputRootDirectory.resolve(folderName);
      this.startTime = startTime;
      Files.createDirectories(soundOutputDir);
    }

    private void trimRequestIds() {
      while (requestIds.size() > 3) {
        requestIds.removeFirst();
      }
    }

    public Long addSoundConditionally(String text, Long speechStartTime, long limitDuration, long periodDuration) throws IOException {
      long relativeTimestamp = speechStartTime - startTime;
      Path outputFile = soundOutputDir.resolve(index + " - " + relativeTimestamp + OUTPUT_SOUND_FILE_SUFFIX);
      TextToSpeechClient.TextToSpeechResponse response = textToSpeechClient.produce(text, requestIds.toArray(new String[0]), outputFile);
      long duration = response.durationMs();
      if (duration <= limitDuration) {
        audioTrackBuilder.addVoice(text, relativeTimestamp, duration,
            // Avoid creating a wait time if limitDuration has been artificially increased to a larger window,
            // but duration is smaller than that window.
            periodDuration < limitDuration ? Math.max(duration, periodDuration) : limitDuration,
            outputFile
        );
        requestIds.add(response.requestId());
        trimRequestIds();
        index++;
      } else {
        Files.delete(outputFile);
      }
      return duration;
    }

    public AddSoundResult addSound(String text, Long speechStartTime, long limitDuration, long periodDuration) throws IOException {
      long relativeTimestamp = speechStartTime - startTime;
      Path outputFile = soundOutputDir.resolve(index + " - " + relativeTimestamp + OUTPUT_SOUND_FILE_SUFFIX);
      TextToSpeechClient.TextToSpeechResponse response = textToSpeechClient.produce(text, requestIds.toArray(new String[0]), outputFile);
      long duration = response.durationMs();
      // Avoid creating a wait time if limitDuration has been artificially increased to a larger window,
      // but duration is smaller than that window.
      long cutoff = periodDuration < limitDuration ? Math.max(duration, periodDuration) : limitDuration;
      audioTrackBuilder.addVoice(text, relativeTimestamp, duration,
          cutoff,
          outputFile);
      index++;
      requestIds.add(response.requestId());
      trimRequestIds();

      return new AddSoundResult(duration, Math.min(cutoff, duration));
    }

  }

  private record AddSoundResult(Long soundFileDuration, Long soundDurationInTrack) {  }

  private Response getShorteningMessage(long durationMs, long limitDurationMs) {
    long durationSeconds = msToSeconds(durationMs);
    long limitDurationSeconds = msToSeconds(limitDurationMs);

    if (limitDurationSeconds != durationSeconds) {
      return llmClient.run(
          new Text("Your message was too long. It took " + durationSeconds + "s, but only " + limitDurationSeconds + "s is allowed.", "")
      );
    } else {
      return llmClient.run(
          new Text("Your message was a bit too long. Shorten it. It needs to fit into " + limitDurationSeconds + " second(s)", "")
      );
    }
  }

  private Text getGameIntroductionInstructions(Event firstEvent, String playerName, long durationMs) {
    String longTerm = """
        The game screen has loaded and is on pause. 
        """;

    String shortTerm = String.format("""
        You now have %d seconds to make an introduction. The event data follows:
        Player name: %s
        Game name: Space Blaster
        Episode name: %s
        Game difficulty: %s
        Total levels: %d
        """, (int) Math.floor(durationMs / 1000d), playerName, firstEvent.episodeName, firstEvent.gameDifficulty.toString(), firstEvent.totalRoundsCount
    );

    return new Text(longTerm, shortTerm);
  }

  private Text getEventInstructions(long timePassedSeconds, Period period, List<Event> secondaryEventsFromLastPeriod, long latency) {
    String secondaryEvents =
        !secondaryEventsFromLastPeriod.isEmpty() ?
        "While you were commenting there happened some minor events: " + stringifySecondaryEvents(secondaryEventsFromLastPeriod) + "\n\n" :
        "";

    String instruction = !secondaryEventsFromLastPeriod.isEmpty() ?
        String.format("You have %d second window to comment on the following event and/or on the minor events above.", msToSeconds(period.getDuration())) :
        String.format("You have %d second window to comment on the following event.", msToSeconds(period.getDuration()));


    long latencySeconds = msToSeconds(latency);
    if (latencySeconds == 0) {
      String longTerm = String.format("""
          %s

          %s
          There is an event of type: %s.
          """, secondaryEvents, instruction, period.getEvent().getType());

      String shortTerm = String.format("""
          Event data follows: %s
          Seconds passed from previous event: %ds
          """, EventSerializer.serialize(period.getEvent()), timePassedSeconds);

      return new Text(longTerm, shortTerm);
    } else {

      String longTerm = String.format("""
          %s

          %s
          There was an event of type: %s %d second(s) ago.
          """, secondaryEvents, instruction, period.getEvent().getType(), latencySeconds
      );
      String shortTerm = String.format("""
          Event data follows: %s
          Seconds passed from previous event: %ds
          """, EventSerializer.serialize(period.getEvent()), timePassedSeconds
      );

      return new Text(longTerm, shortTerm);
    }
  }

  private String stringifySecondaryEvents(List<Event> secondaryEventsFromLastPeriod) {
    Map<EventType, Integer> eventTypeCountMap = new HashMap<>();

    for (Event event : secondaryEventsFromLastPeriod) {
      eventTypeCountMap.put(event.getType(), eventTypeCountMap.getOrDefault(event.getType(), 0) + 1);
    }

    StringBuilder sb = new StringBuilder();
    for (Map.Entry<EventType, Integer> entry : eventTypeCountMap.entrySet()) {
      sb.append(entry.getValue()).append(" x ").append(entry.getKey()).append(", ");
    }
    if (!sb.isEmpty()) {
      sb.delete(sb.length() - 2, sb.length());
    }
    return sb.toString();
  }
  public record SpaceTalk(Path soundFile, List<AudioTrackBuilder.TimedVoice> voices) {
  }

  private static class CommentaryContext {
    private final Period period;
    private final Period originalPeriod;
    private final Text instructions;
    private final long latency;

    private final long latencyReduction;

    private final int periodIndex;

    private final List<RejectedCommentary> rejectedCommentaries = new ArrayList<>();

    public CommentaryContext(Period period, int periodIndex, Text instructions, long latency) {
      this.period = period;
      this.instructions = instructions;
      this.latency = latency;
      this.periodIndex = periodIndex;
      this.originalPeriod = period;
      this.latencyReduction = 0;
    }

    public CommentaryContext(Period period, Period originalPeriod, int periodIndex, Text instructions, long latency, long latencyReduction) {
      this.period = period;
      this.originalPeriod = originalPeriod;
      this.instructions = instructions;
      this.latency = latency;
      this.periodIndex = periodIndex;
      this.latencyReduction = latencyReduction;
    }

    public void addRejectedCommentary(String value, Long estimatedSoundDuration, Long soundDuration) {
      rejectedCommentaries.add(new RejectedCommentary(value, soundDuration, estimatedSoundDuration));
    }

    public List<RejectedCommentary> getRejectedCommentaries() {
      return rejectedCommentaries;
    }

    private record RejectedCommentary(String value, Long soundDuration, Long estimatedSoundDuration) {
    }
  }

}
