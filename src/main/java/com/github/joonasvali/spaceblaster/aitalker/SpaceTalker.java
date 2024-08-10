package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.event.AbandonShortenSpeechEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.CommentaryFailedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodIgnoredEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingCompletedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingStartedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.ResoluteShorteningMessageEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;
import com.github.joonasvali.spaceblaster.aitalker.llm.LLMClient;
import com.github.joonasvali.spaceblaster.aitalker.llm.Response;
import com.github.joonasvali.spaceblaster.aitalker.llm.Text;
import com.github.joonasvali.spaceblaster.aitalker.sound.AudioTrackBuilder;
import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpaceTalker {
  private static final Logger logger = LoggerFactory.getLogger(SpaceTalker.class);
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
  public static final long MEDIUM_PERIOD_THRESHOLD_MS = 3000;
  private final SoundDurationEvaluator soundDurationEvaluator;
  private final LLMClient llmClient;
  private final TextToSpeechClient textToSpeechClient;
  private final AudioTrackBuilder audioTrackBuilder;
  private final Path outputRootDirectory;
  private final ArrayList<SpaceTalkListener> listeners = new ArrayList<>();

  private final ArrayDeque<Event> unaddressedEvents = new ArrayDeque<>();

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
    long latency;
    {
      Event firstEvent = period.getEvent();
      input = getGameIntroductionInstructions(firstEvent, playerName, period.getDuration());

      commentaryRepository = new VoiceCommentaryRepository(period.getEvent().eventTimestamp, projectName);

      Period nextPeriod = periods.size() > 1 ? periods.get(1) : null;
      long nextPeriodStartTime = nextPeriod != null ? nextPeriod.getEvent().eventTimestamp - firstEvent.eventTimestamp : Long.MAX_VALUE;

      CommentaryContext context = new CommentaryContext(period, 0, input, 0, nextPeriodStartTime);
      notifyPeriodProcessingStartedListeners(0, 0, period.getDuration(), 0);
      Commentary commentary = produceCommentary(commentaryRepository, context);
      llmClient.commitHistoryBySquashing();
      notifyPeriodCompletedListeners(commentary.text, commentary.input, 0, commentary.generatedAudioDurationMs, 0,
          // since this is the first period, the silence is added to the start of the track.
          Math.max(0, commentary.generatedAudioRelativeStartTime + period.getDuration() - commentary.generatedAudioDurationMs),
          Math.max(0, commentary.generatedAudioDurationMs - period.getDuration()),
          period.getDuration(),
          0,
          commentary.retryAttempts,
          commentary.shorteningAbandoned
      );
      latency = Math.max(commentary.generatedAudioDurationMs - period.getDuration(), 0);
    }

    for (int i = 1; i < periods.size(); i++) {
      Period lastPeriod = period;
      List<Event> secondaryEventsFromLastPeriod = lastPeriod.getSecondaryEvents();
      period = periods.get(i);
      long latencyReduction = 0;

      if (latency > period.getDuration()) {
        // Next period has already begun, the commentator needs to move on.
        latency -= period.getDuration();
        commentaryRepository.incrementIndex();
        unaddressedEvents.add(period.getEvent());
        notifyPeriodIgnoredListeners(i, period.getEvent().eventTimestamp - periods.getFirst().getEvent().eventTimestamp, period.getDuration(), latency + period.getDuration());
        continue;
      }

      long inputLatency = latency;
      if (latency > LATENCY_THRESHOLD_MS && period.getDuration() > MEDIUM_PERIOD_THRESHOLD_MS) {
        // replace period with time debt adjusted one to reduce latency.
        latencyReduction = Math.min(period.getDuration() / 2, latency);
        latency -= latencyReduction;
        long newPeriodDuration = period.getDuration() - latencyReduction;
        period = new Period(period.getEvent(), new ArrayList<>(period.getSecondaryEvents()), newPeriodDuration);
      }

      notifyPeriodProcessingStartedListeners(i, period.getEvent().eventTimestamp - periods.getFirst().getEvent().eventTimestamp, period.getDuration(), inputLatency);

      input = getEventInstructions(msToSeconds(lastPeriod.getDuration() + inputLatency), period, secondaryEventsFromLastPeriod, inputLatency);

      Period nextPeriod = i + 1 < periods.size() ? periods.get(i + 1) : null;
      long nextPeriodStartTime = nextPeriod != null ? nextPeriod.getEvent().eventTimestamp - periods.getFirst().getEvent().eventTimestamp : Long.MAX_VALUE;
      CommentaryContext context = new CommentaryContext(period, periods.get(i), i, input, latency, latencyReduction, nextPeriodStartTime);
      Commentary commentary = produceCommentary(commentaryRepository, context);
      llmClient.commitHistoryBySquashing();

      AudioTrackBuilder.TimedVoice lastVoice = audioTrackBuilder.getLastVoice();
      notifyPeriodCompletedListeners(
          commentary.text,
          commentary.input,
          i,
          lastVoice.voiceDuration(),
          period.getEvent().eventTimestamp - periods.getFirst().getEvent().eventTimestamp,
          lastVoice.startTime(),
          Math.max(0, lastVoice.voiceCutoffMs() - lastVoice.voiceDuration()),
          period.getDuration(),
          inputLatency,
          commentary.retryAttempts,
          commentary.shorteningAbandoned
      );

      latency = (lastVoice.startTime() + lastVoice.voiceCutoffMs()) - nextPeriodStartTime;
      if (i < periods.size() - 1 && latency < 0) {
        throw new RuntimeException("Latency is negative, something is wrong: " + latency);
      }
      unaddressedEvents.clear();
      audioTrackBuilder.validateLastTimestampAfter(period.getEvent().eventTimestamp - periods.getFirst().getEvent().eventTimestamp);
    }

    Path finalSoundFile = commentaryRepository.soundOutputDir.resolve(OUTPUT_SOUND_FILE_NAME + OUTPUT_SOUND_FILE_SUFFIX);
    var voices = audioTrackBuilder.produce(finalSoundFile);
    return new SpaceTalk(finalSoundFile, voices);
  }

  private static Long msToSeconds(Long duration) {
    return (long) Math.round(duration / 1000f);
  }

  private record Commentary(
      String text,
      String input,
      long generatedAudioDurationMs,
      long generatedAudioRelativeStartTime,
      int retryAttempts,
      boolean shorteningAbandoned
  ) {
  }


  private Commentary produceCommentary(VoiceCommentaryRepository commentaryRepository, CommentaryContext context) throws IOException {
    Response response = llmClient.run(context.instructions);
    long evaluatedDuration = soundDurationEvaluator.evaluateDurationInMs(response.outputMessage());

    Period period = context.period;

    String lastOutputMessage = response.outputMessage();
    long eventTimeStamp = period.getEvent().getEventTimestamp();
    long limitDuration = Math.max(period.getDuration(), EventDigester.MIN_PERIOD);

    long speechStartTime = eventTimeStamp + context.latency + context.latencyReduction;

    long audioFileDuration = 0;
    if (context.isAcceptableDuration(evaluatedDuration, limitDuration)) {
      audioFileDuration = commentaryRepository.addSoundConditionally(context, lastOutputMessage, speechStartTime, context.nextPeriodRelativeStartTime, false);
    }

    int failsToShorten = 0;
    int retries = 0;

    while (failsToShorten < SHORTENING_FAILURES_ALLOWED && (!context.isAcceptableDuration(evaluatedDuration, limitDuration) || !context.isAcceptableDuration(audioFileDuration, limitDuration))) {
      retries++;
      notifyCommentaryFailedListeners(lastOutputMessage, failsToShorten, context.periodIndex, eventTimeStamp, period.getDuration(), context.latency + context.latencyReduction);
      context.addRejectedCommentary(lastOutputMessage, evaluatedDuration, audioFileDuration);

      Response anotherResponse;
      if (failsToShorten == SHORTENING_RESOLUTE_THRESHOLD) {
        notifyResoluteShorteningMessage(context.periodIndex, lastOutputMessage, evaluatedDuration, limitDuration, failsToShorten);
        anotherResponse = llmClient.run(new Text(RESOLUTE_SHORTER_MESSAGE_INSTRUCTION, ""));
      } else {
        anotherResponse = getShortenedMessage(Math.max(evaluatedDuration, audioFileDuration), limitDuration);
      }

      lastOutputMessage = anotherResponse.outputMessage();
      evaluatedDuration = soundDurationEvaluator.evaluateDurationInMs(anotherResponse.outputMessage());

      boolean exceedsEvaluatedDuration = !context.isAcceptableDuration(evaluatedDuration, limitDuration);

      if (!exceedsEvaluatedDuration) {
        audioFileDuration = commentaryRepository.addSoundConditionally(context, lastOutputMessage, speechStartTime, context.nextPeriodRelativeStartTime, false);
      }

      if (exceedsEvaluatedDuration || !context.isAcceptableDuration(audioFileDuration, Math.max(period.getDuration(), EventDigester.MIN_PERIOD))) {
        failsToShorten++;
      }
    }

    if (failsToShorten >= SHORTENING_FAILURES_ALLOWED) {
      notifyAbandonShortenSpeechListeners(context.periodIndex, lastOutputMessage, failsToShorten);
      var result = commentaryRepository.addSound(lastOutputMessage, speechStartTime, context.nextPeriodRelativeStartTime, period.getDuration(), false);
      audioFileDuration = result.soundDurationInTrack;
    }

    return new Commentary(
        lastOutputMessage,
        context.instructions.toString(),
        audioFileDuration,
        commentaryRepository.getLastVoiceStartTime(),
        retries,
        failsToShorten >= SHORTENING_FAILURES_ALLOWED
    );
  }
  private void notifyAbandonShortenSpeechListeners(int periodIndex, String output, int attempt) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      s.onAbandonShortenSpeech(new AbandonShortenSpeechEvent(periodIndex, output, attempt, time));
    });
  }

  private void notifyPeriodProcessingStartedListeners(
      int periodIndex,
      long periodRelativeStartTime,
      long periodDuration,
      long inputLatency
  ) {
    long time = System.currentTimeMillis();
    PeriodProcessingStartedEvent event = new PeriodProcessingStartedEvent(
        inputLatency,
        Math.max(periodDuration, EventDigester.MIN_PERIOD),
        periodIndex,
        periodRelativeStartTime,
        time
    );
    listeners.forEach((s) -> s.onPeriodProcessingStarted(event));
  }

  private void notifyPeriodCompletedListeners(
      String output,
      String input,
      int periodIndex,
      long generatedAudioDurationMs,
      long periodRelativeStartTime,
      long generatedAudioRelativeStartTime,
      long silenceDuration,
      long periodDuration,
      long inputLatency,
      int retryAttempts,
      boolean shorteningAbandoned
  ) {
    long time = System.currentTimeMillis();
    PeriodProcessingCompletedEvent event = new PeriodProcessingCompletedEvent(
        output,
        input,
        generatedAudioDurationMs,
        periodRelativeStartTime,
        generatedAudioRelativeStartTime,
        Math.max(periodDuration, EventDigester.MIN_PERIOD),
        periodIndex,
        inputLatency,
        retryAttempts,
        silenceDuration,
        shorteningAbandoned,
        time
    );
    listeners.forEach((s) -> s.onPeriodProcessingCompleted(event));
  }

  private void notifyPeriodIgnoredListeners(int periodIndex, long periodRelativeStartTime, long periodDuration, long latency) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      s.onIgnorePeriod(new PeriodIgnoredEvent(periodIndex, periodRelativeStartTime, periodDuration, latency, time));
    });
  }

  private void notifyCommentaryFailedListeners(
      String output,
      int attempt,
      int periodIndex,
      long periodRelativeStartTime,
      long periodDuration,
      long inputLatency
  ) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      s.onCommentaryFailed(new CommentaryFailedEvent(
          attempt,
          output,
          inputLatency,
          Math.max(periodDuration, EventDigester.MIN_PERIOD),
          periodIndex,
          periodRelativeStartTime,
          time
      ));
    });
  }

  private void notifyResoluteShorteningMessage(int periodIndex, String output, long duration, long limitDuration, int attempt) {
    long time = System.currentTimeMillis();
    listeners.forEach((s) -> {
      s.onResoluteShorteningMessage(new ResoluteShorteningMessageEvent(
          periodIndex,
          output,
          duration,
          limitDuration,
          attempt,
          time
      ));
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

    public void incrementIndex() {
      index++;
    }

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

    public Long addSoundConditionally(CommentaryContext context, String text, Long speechStartTime, long nextPeriodRelativeStartTime, boolean forceCutoff) throws IOException {
      long periodDuration = context.period.getDuration();
      long relativeTimestamp = speechStartTime - startTime;
      long timeUntilNextPeriod = nextPeriodRelativeStartTime - relativeTimestamp;
      Path outputFile = soundOutputDir.resolve(index + " - " + relativeTimestamp + OUTPUT_SOUND_FILE_SUFFIX);
      TextToSpeechClient.TextToSpeechResponse response = textToSpeechClient.produce(text, requestIds.toArray(new String[0]), outputFile);
      long duration = response.durationMs();
      if (context.isAcceptableDuration(duration, Math.max(periodDuration, EventDigester.MIN_PERIOD))) {
        long cutoff = forceCutoff ? Math.max(periodDuration, EventDigester.MIN_PERIOD) : Math.max(periodDuration, duration);

        long unallocated = Math.max(0, cutoff - duration);
        if (unallocated > 0 && cutoff > timeUntilNextPeriod) {
          // Prevent adding silence, when there is no time for it.
          cutoff -= Math.min(unallocated, cutoff - timeUntilNextPeriod);
        }

        audioTrackBuilder.addVoice(text, relativeTimestamp, duration,
            cutoff,
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

    public AddSoundResult addSound(String text, Long speechStartTime, long nextPeriodRelativeStartTime, long periodDuration, boolean forceCutoff) throws IOException {
      long relativeTimestamp = speechStartTime - startTime;
      Path outputFile = soundOutputDir.resolve(index + " - " + relativeTimestamp + OUTPUT_SOUND_FILE_SUFFIX);
      TextToSpeechClient.TextToSpeechResponse response = textToSpeechClient.produce(text, requestIds.toArray(new String[0]), outputFile);
      long duration = response.durationMs();
      long cutoff = forceCutoff ? Math.max(periodDuration, EventDigester.MIN_PERIOD) : Math.max(periodDuration, duration);
      long unallocated = Math.max(0, cutoff - duration);
      if (unallocated > 0 && relativeTimestamp + cutoff > nextPeriodRelativeStartTime) {
        cutoff -= Math.min(unallocated, relativeTimestamp + cutoff - nextPeriodRelativeStartTime);
      }

      audioTrackBuilder.addVoice(text, relativeTimestamp, duration,
          cutoff,
          outputFile);
      index++;
      requestIds.add(response.requestId());
      trimRequestIds();

      return new AddSoundResult(duration, Math.min(cutoff, duration));
    }

    public long getLastVoiceStartTime() {
      return audioTrackBuilder.getLastVoiceStartTime();
    }
  }

  private record AddSoundResult(Long soundFileDuration, Long soundDurationInTrack) {  }

  private Response getShortenedMessage(long durationMs, long limitDurationMs) {
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
        unaddressedEvents.isEmpty() ?
            (!secondaryEventsFromLastPeriod.isEmpty() ?
          "While you were commenting there happened some minor events: " + stringifySecondaryEvents(secondaryEventsFromLastPeriod) + "\n\n" :
          "") : "While you were commenting there happened some events: " + stringifySecondaryEvents(new ArrayList<>(unaddressedEvents)) + "\n\n";

    String instruction = !secondaryEventsFromLastPeriod.isEmpty() ?
        String.format("You have %d second window to comment on the following event and/or on the minor events above.", msToSeconds(Math.max(period.getDuration(), EventDigester.MIN_PERIOD))) :
        String.format("You have %d second window to comment on the following event.", msToSeconds(Math.max(period.getDuration(), EventDigester.MIN_PERIOD)));


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

    private final long nextPeriodRelativeStartTime;
    private final Text instructions;
    private final long latency;

    private final long latencyReduction;

    private final int periodIndex;

    private final List<RejectedCommentary> rejectedCommentaries = new ArrayList<>();
    private boolean isAcceptableDuration(long duration, long limitDuration) {
      if (latency == 0) {
        // Allow to go a bit over the limit.
        return duration <= limitDuration + Math.min(2000, Math.max(1000, limitDuration / 3));
      }
      return duration <= limitDuration;
    }

    public CommentaryContext(Period period, int periodIndex, Text instructions, long latency, long nextPeriodRelativeStartTime) {
      this.period = period;
      this.instructions = instructions;
      this.latency = latency;
      this.periodIndex = periodIndex;
      this.originalPeriod = period;
      this.latencyReduction = 0;
      this.nextPeriodRelativeStartTime = nextPeriodRelativeStartTime;
    }

    public CommentaryContext(Period period, Period originalPeriod, int periodIndex, Text instructions, long latency, long latencyReduction, long nextPeriodRelativeStartTime) {
      this.period = period;
      this.originalPeriod = originalPeriod;
      this.instructions = instructions;
      this.latency = latency;
      this.periodIndex = periodIndex;
      this.latencyReduction = latencyReduction;
      this.nextPeriodRelativeStartTime = nextPeriodRelativeStartTime;
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
