package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.event.AbandonShortenSpeechEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.CommentaryFailedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingCompletedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingStartedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.ResoluteShorteningMessageEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;
import com.github.joonasvali.spaceblaster.aitalker.llm.BaseLLMClient;
import com.github.joonasvali.spaceblaster.aitalker.llm.LLMClient;
import com.github.joonasvali.spaceblaster.aitalker.llm.Response;
import com.github.joonasvali.spaceblaster.aitalker.llm.Text;
import com.github.joonasvali.spaceblaster.aitalker.sound.AudioTrackBuilder;
import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechOutput;
import com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion.SilentWav;
import com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion.WavDuration;
import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.EventReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SpaceTalkerTest {
  @TempDir
  public static Path tempDir;

  private interface TestController {
    long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt);
    long getSoundRealDuration(int periodIndex, long periodDuration, int attempt);
  }
  private void runTest(String eventFilePath, TestController testController) throws IOException {
    List<Event> events = getEvents(eventFilePath);

    TestTextToSpeechClient speech = new TestTextToSpeechClient();
    TestLLMClient llmClient = new TestLLMClient();

    SpaceTalker spaceTalker = new SpaceTalker(speech, llmClient, tempDir);
    spaceTalker.addListener(new SpaceTalkListener() {

      @Override
      public void onCommentaryFailed(CommentaryFailedEvent event) {
        llmClient.setAnswer(event.periodIndex(), event.periodDuration());
        speech.setAnswer(event.periodIndex(), testController.getSoundEvaluatedDuration(event.periodIndex(), event.periodDuration(), event.attempt() + 1), testController.getSoundRealDuration(event.periodIndex(), event.periodDuration(), event.attempt() + 1));
      }

      @Override
      public void onPeriodProcessingStarted(PeriodProcessingStartedEvent event) {
        llmClient.setAnswer(event.periodIndex(), event.periodDuration());
        speech.setAnswer(event.periodIndex(), testController.getSoundEvaluatedDuration(event.periodIndex(), event.periodDuration(), 0), testController.getSoundRealDuration(event.periodIndex(), event.periodDuration(), 0));
      }

      @Override
      public void onPeriodProcessingCompleted(PeriodProcessingCompletedEvent event) {
        long audioEnd = event.generatedAudioRelativeStartTime() + event.generatedAudioDurationMs();
        String silence = "";
        if (audioEnd < event.periodRelativeStartTime() + event.periodDuration()) {
          silence = "Silence: " + audioEnd + " -> " + (event.periodRelativeStartTime() + event.periodDuration()) + " (" + (event.periodRelativeStartTime() + event.periodDuration() - audioEnd) + "ms) ";
        }

        System.out.println(
            event.periodIndex() + ": period " + event.periodRelativeStartTime() + " -> " +
                (event.periodRelativeStartTime() + event.periodDuration()) + " completed " +
                (event.retryAttempts() > 0 ? ("(in " + event.retryAttempts() + " attempts)"): "") +
                " Audio: " + event.generatedAudioDurationMs() + "ms, starting at: " + event.generatedAudioRelativeStartTime() + "ms. " +
                (event.inputLatency() > 0 ? event.inputLatency() + "ms latency. " : "") +
                silence +
                "Result \"" + event.result() + "\""
        );

        if (event.generatedAudioRelativeStartTime() < event.periodRelativeStartTime()) {
          throw new RuntimeException("Audio start time is before period start time");
        }

        if (event.inputLatency() > 5000) {
          // This is a very high latency, it should be avoided.
          throw new RuntimeException("Latency is too high");
        }
      }

      @Override
      public void onResoluteShorteningMessage(ResoluteShorteningMessageEvent event) {

      }

      @Override
      public void onAbandonShortenSpeech(AbandonShortenSpeechEvent event) {

      }
    });


    EventDigester eventDigester = new EventDigester(events, true);

    List<Period> periods = new ArrayList<>();
    while (eventDigester.hasNextPeriod()) {
      Period period = eventDigester.getNextPeriod();
      periods.add(period);
    }

    String projectName = "myproject";

    SpaceTalker.SpaceTalk talk = spaceTalker.run(periods, "Player one", projectName);
    List<AudioTrackBuilder.TimedVoice> voices = talk.voices();

    long firstPeriodTimestamp = periods.getFirst().getEvent().eventTimestamp;
    for (int i = 0; i < voices.size(); i++) {
      AudioTrackBuilder.TimedVoice voice = voices.get(i);
      Period period = periods.get(i);
      long relativeTimestamp = period.getEvent().eventTimestamp - firstPeriodTimestamp;

      if (voice.startTime() < relativeTimestamp) {
        throw new RuntimeException("Voice start time is before period start time: " + i + " " + relativeTimestamp + " " + voice.startTime());
      }
    }

    long firstEventTimestamp = events.getFirst().eventTimestamp;
    Assertions.assertEquals(voices.size(), periods.size());

    long lastVoiceStartTimeTimestamp = 0;
    for (int i = 0; i < voices.size(); i++) {
      AudioTrackBuilder.TimedVoice voice = voices.get(i);
      long periodStart = periods.get(i).getEvent().eventTimestamp - firstEventTimestamp;
      Assertions.assertTrue(voice.startTime() >= periodStart, "Voice start time " + voice.startTime() + " is before period start " + periodStart);

      long currentVoiceStartTimestamp = voices.get(i).startTime();
      if (currentVoiceStartTimestamp < lastVoiceStartTimeTimestamp) {
        throw new RuntimeException("Voice start time is before previous voice start time: " + i + " " + currentVoiceStartTimestamp + " " + lastVoiceStartTimeTimestamp);
      }
      lastVoiceStartTimeTimestamp = currentVoiceStartTimestamp;
    }

    long lastPeriodTimestamp = periods.getLast().getEvent().eventTimestamp - firstEventTimestamp;

    long finalSoundFileDuration = WavDuration.getDuration(talk.soundFile());
    Assertions.assertTrue(finalSoundFileDuration > lastPeriodTimestamp + EventDigester.MIN_PERIOD, "The final sound file is shorter (" + finalSoundFileDuration + " ms) than last period timestamp + " + EventDigester.MIN_PERIOD);
  }

  @Test
  public void testSpaceTalkRetryWithEvaluatedDurationFailOnly() throws IOException {
    Random random = new Random("doggy".hashCode());
    TestController testController = new TestController() {
      @Override
      public long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 2 && attempt < 3) {
          return periodDuration + 2100;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }

      @Override
      public long getSoundRealDuration(int periodIndex, long periodDuration, int attempt) {
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }
    };

    runTest("./short-run/short-run.yml", testController);
  }

  @Test
  public void testSpaceTalkRetryWithRealDurationFailOnly() throws IOException {
    Random random = new Random("doggy".hashCode());
    TestController testController = new TestController() {
      @Override
      public long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt) {
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }

      @Override
      public long getSoundRealDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 2 && attempt < 3) {
          return periodDuration + 2100;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }
    };

    runTest("./short-run/short-run.yml", testController);
  }

  @Test
  public void testShortSpaceTalkWithSomeTalkExceedingDuration() throws IOException {
    Random random = new Random("doggy".hashCode());
    TestController testController = new TestController() {
      @Override
      public long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt) {
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }

      @Override
      public long getSoundRealDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 2) {
          return periodDuration + 2100;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f) + periodDuration / 2);
      }
    };

    runTest("./short-run/short-run.yml", testController);
  }

  @Test
  public void testLongSpaceTalkWithSomeTalkExceedingDuration() throws IOException {
    Random random = new Random("doggy".hashCode());
    TestController testController = new TestController() {
      @Override
      public long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt) {
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }

      @Override
      public long getSoundRealDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 2) {
          return periodDuration + 2100;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f) + periodDuration / 2);
      }
    };

    runTest("./long-run/long-run.yml", testController);
  }

  @Test
  public void testSpaceTalk2() throws IOException {
    Random random = new Random("doggy".hashCode());
    TestController testController = new TestController() {
      @Override
      public long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 12) {
          return periodDuration + 1000;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }

      @Override
      public long getSoundRealDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 12) {
          return periodDuration + 1000;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }
    };
    runTest("./long-run/long-run.yml", testController);
  }

  @Test
  public void testSpaceTalk3() throws IOException {
    Random random = new Random("doggy".hashCode());
    TestController testController = new TestController() {
      @Override
      public long getSoundEvaluatedDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 12) {
          return periodDuration + 1000;
        }
        if (periodIndex == 14) {
          return periodDuration + 2000;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }

      @Override
      public long getSoundRealDuration(int periodIndex, long periodDuration, int attempt) {
        if (periodIndex == 12) {
          return periodDuration + 1000;
        }
        if (periodIndex == 14) {
          return periodDuration + 2000;
        }
        return (long) (periodDuration - (random.nextFloat() * periodDuration / 2f));
      }
    };
    runTest("./long-run/long-run.yml", testController);
  }

  private List<Event> getEvents(String eventFilePath) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(eventFilePath)) {

      EventReader reader = new EventReader(inputStream);
      List<Event> events = reader.readContent();
      return events;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }


  private static class TestLLMClient extends BaseLLMClient {
    private final ArrayDeque<Entry> answers = new ArrayDeque<>();

    public void setAnswer(int periodIndex, long periodDuration) {
      answers.clear();
      answers.add(new Entry(periodIndex, periodIndex + " text with approx duration of " + periodDuration + " ms."));
    }

    private record Entry (int periodIndex, String text) { }

    @Override
    public Response run(Text instruction) {
      if (answers.isEmpty()) {
        throw new RuntimeException("No more answers");
      }
      Entry first = answers.removeFirst();
      uncommittedHistory.add(new Message(MessageType.REQUEST, instruction));
      uncommittedHistory.add(new Message(MessageType.RESPONSE, new Text(first.text, "")));
      return new Response(instruction, first.text);
    }

    @Override
    public SpaceTalkListener getSpaceTalkListener() {
      return null;
    }
  };

  private static class TestTextToSpeechClient implements TextToSpeechClient {
    private final List<Entry> answers = new ArrayList<>();

    public void setAnswer(int periodIndex, long periodEvaluatedDuration, long periodDuration) {
      answers.clear();
      answers.add(new Entry(periodIndex, periodEvaluatedDuration, periodDuration));
    }

    private record Entry (int periodIndex, long nextEstimatedDurationMs, long nextDurationMs) { }

    public TestTextToSpeechClient() throws IOException {

    }

    @Override
    public String getCommentatorDescription() {
      return "You are a test commentator.";
    }

    @Override
    public TextToSpeechOutput getOutputSettings() {
      return new TextToSpeechOutput() {

        @Override
        public int getSampleRate() {
          return 44100;
        }

        @Override
        public int getBitRate() {
          return 128;
        }

        @Override
        public void convertResultingFileToWav(Path input, Path output) throws IOException {
          Files.copy(input, output);
        }

        @Override
        public long getDurationInMs(Path input) throws IOException {
          return WavDuration.getDuration(input);
        }
      };
    }

    @Override
    public TextToSpeechResponse produce(String text, String[] previousRequestIds, Path outputFile) throws IOException {
      int outputFileIndex = Integer.parseInt(outputFile.getFileName().toString().split(" - ")[0]);
      if (answers.isEmpty()) {
        throw new RuntimeException("No more answers");
      }
      Entry e = answers.getFirst();
      if (e.periodIndex != outputFileIndex) {
        throw new RuntimeException("Error, expected: " + outputFileIndex + ", but got " + e.periodIndex);
      }
      AudioInputStream stream = SilentWav.createSilentStream(e.nextDurationMs);
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, outputFile.toFile());
      return new TextToSpeechResponse(e.nextDurationMs, UUID.randomUUID().toString());
    }

    @Override
    public SoundDurationEvaluator getSoundDurationEvaluator() {
      return sound -> {
        if (answers.isEmpty()) {
          throw new RuntimeException("No more answers");
        }
        Entry e = answers.getFirst();
        return e.nextEstimatedDurationMs;
      };
    }

    @Override
    public SpaceTalkListener getSpaceTalkListener() {
      return null;
    }
  }
}

