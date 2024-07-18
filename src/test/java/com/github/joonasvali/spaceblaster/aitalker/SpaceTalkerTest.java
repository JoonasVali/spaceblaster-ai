package com.github.joonasvali.spaceblaster.aitalker;

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
import java.util.List;
import java.util.UUID;

public class SpaceTalkerTest {
  @TempDir
  public static Path tempDir;

  public void runTest(String eventFilePath, List<String> llmAnswers, List<TestTextToSpeechClient.Entry> ttsAnswers, String expectedOutput, long finalDurationMs) throws IOException {
    List<Event> events = getEvents(eventFilePath);

    List<String> answers = new ArrayList<>(llmAnswers);

    TestTextToSpeechClient speech = new TestTextToSpeechClient();
    speech.setAnswers(ttsAnswers);

    LLMClient llmClient = new TestLLMClient(answers);
    SpaceTalker spaceTalker = new SpaceTalker(speech, llmClient, tempDir);
    spaceTalker.addListener(new SpaceTalkListener() {

      @Override
      public void onCommentaryFailed(String lastOutputMessage, int attempt, long timeSinceEventMs) {
        speech.nextAnswer();
      }

      @Override
      public void onFailToShortenSpeech(String lastOutputMessage, int attempt, long timeSinceEventMs) {
      }

      @Override
      public void onPeriodProcessingCompleted(String result, int periodIndex, long timeSinceEventMs) {
        speech.nextAnswer();
      }

      @Override
      public void onResoluteShorteningMessage(String result, long duration, long limitDuration, int attempt, long timeSinceEventMs) {

      }

      @Override
      public void onAbandonShortenSpeech(String output, int attempt, long timeSinceEventMs) {

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
    List<AudioTrackBuilder.TimedVoice> list = talk.voices();

    StringBuilder strb = new StringBuilder();
    list.forEach(timedVoice -> {
      strb.append(timedVoice.startTime()).append(" ");
      strb.append(timedVoice.voiceDuration()).append(" ");
      strb.append(timedVoice.voiceCutoffMs()).append(" ");
      strb.append(timedVoice.text()).append(" ");
      strb.append(timedVoice.soundFile()).append("\n");
    });

    long firstEventTimestamp = events.get(0).eventTimestamp;
    Assertions.assertEquals(list.size(), periods.size());
    for (int i = 0; i < list.size(); i++) {
      AudioTrackBuilder.TimedVoice voice = list.get(i);
      long periodStart = periods.get(i).getEvent().eventTimestamp - firstEventTimestamp;
      Assertions.assertTrue(voice.startTime() >= periodStart, "Voice start time " + voice.startTime() + " is before period start " + periodStart);
    }

    Path outputFile = tempDir.resolve(projectName);
    Assertions.assertEquals(expectedOutput.replace("${PROJECT_DIR}", outputFile.toAbsolutePath().toString()), strb.toString());

    Assertions.assertEquals(finalDurationMs, WavDuration.getDuration(talk.soundFile()));

  }

  @Test
  public void testSpaceTalk1() throws IOException, URISyntaxException {

    List<String> llmAnswers = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      llmAnswers.add("LLM answer " + i);
    }

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(20000, 20000),
        new TestTextToSpeechClient.Entry(4095, 4095),
        new TestTextToSpeechClient.Entry(3000, 3000),
        new TestTextToSpeechClient.Entry(4000, 4000),
        new TestTextToSpeechClient.Entry(15000, 15000)
    );

    String expected = """
        0 20000 23899 LLM answer 0 ${PROJECT_DIR}\\0 - 0.wav
        23899 4095 4095 LLM answer 1 ${PROJECT_DIR}\\1 - 23899.wav
        27994 3000 5604 LLM answer 2 ${PROJECT_DIR}\\2 - 27994.wav
        33598 4000 5665 LLM answer 3 ${PROJECT_DIR}\\3 - 33598.wav
        39263 15000 20000 LLM answer 4 ${PROJECT_DIR}\\4 - 39263.wav
        """;

    runTest("./short-run/short-run.yml", llmAnswers, ttsAnswers, expected, 59263);
  }

  @Test
  public void testSpaceTalk2() throws IOException {
    final int fails = 3 * 3;
    List<String> llmAnswers = new ArrayList<>();
    for (int i = 0; i < 18 + fails; i++) {
      llmAnswers.add("LLM answer " + i);
    }

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(16000, 16000),
        new TestTextToSpeechClient.Entry(10000, 10000),
        new TestTextToSpeechClient.Entry(3000, 3000),
        new TestTextToSpeechClient.Entry(14000, 14000),
        new TestTextToSpeechClient.Entry(4000, 4000),
        new TestTextToSpeechClient.Entry(14000, 14000),
        new TestTextToSpeechClient.Entry(3000, 3000),
        new TestTextToSpeechClient.Entry(3000, 3000),
        new TestTextToSpeechClient.Entry(2000, 2000),
        new TestTextToSpeechClient.Entry(3000, 3000),
        new TestTextToSpeechClient.Entry(2000, 2000),
        new TestTextToSpeechClient.Entry(8000, 8000),
        // 11th
        new TestTextToSpeechClient.Entry(6000, 6000),
        new TestTextToSpeechClient.Entry(6000, 6000),
        new TestTextToSpeechClient.Entry(6000, 6000),
        new TestTextToSpeechClient.Entry(6000, 6000),
        // end of 11th

        new TestTextToSpeechClient.Entry(4000, 4000),
        new TestTextToSpeechClient.Entry(6000, 6000),
        new TestTextToSpeechClient.Entry(2000, 2000),
        new TestTextToSpeechClient.Entry(3000, 3000),
        new TestTextToSpeechClient.Entry(14000, 14000)

    );

    String expected = """
        0 16000 23906 LLM answer 0 ${PROJECT_DIR}\\0 - 0.wav
        23906 10000 18942 LLM answer 1 ${PROJECT_DIR}\\1 - 23906.wav
        42848 3000 3000 LLM answer 2 ${PROJECT_DIR}\\2 - 42848.wav
        45848 14000 18865 LLM answer 3 ${PROJECT_DIR}\\3 - 45848.wav
        64713 4000 10285 LLM answer 4 ${PROJECT_DIR}\\4 - 64713.wav
        74998 14000 20234 LLM answer 5 ${PROJECT_DIR}\\5 - 74998.wav
        95232 3000 3000 LLM answer 6 ${PROJECT_DIR}\\6 - 95232.wav
        98232 3000 3000 LLM answer 7 ${PROJECT_DIR}\\7 - 98232.wav
        101232 2000 3900 LLM answer 8 ${PROJECT_DIR}\\8 - 101232.wav
        103232 3000 11466 LLM answer 9 ${PROJECT_DIR}\\9 - 103232.wav
        114698 2000 9418 LLM answer 10 ${PROJECT_DIR}\\10 - 114698.wav
        124116 6000 5550 LLM answer 14 ${PROJECT_DIR}\\11 - 124116.wav
        129666 6000 10065 LLM answer 15 ${PROJECT_DIR}\\12 - 129666.wav
        139731 4000 4000 LLM answer 16 ${PROJECT_DIR}\\13 - 139731.wav
        143731 6000 9151 LLM answer 17 ${PROJECT_DIR}\\14 - 143731.wav
        152882 2000 2000 LLM answer 18 ${PROJECT_DIR}\\15 - 152882.wav
        154882 3000 7132 LLM answer 19 ${PROJECT_DIR}\\16 - 154882.wav
        162014 14000 20000 LLM answer 20 ${PROJECT_DIR}\\17 - 162014.wav
        """;

    runTest("./long-run/long-run.yml", llmAnswers, ttsAnswers, expected, 183914);
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


  private class TestLLMClient extends BaseLLMClient {
    private final ArrayDeque<String> answers = new ArrayDeque<>();

    public TestLLMClient(List<String> answers) {
      this.answers.addAll(answers);
    }

    @Override
    public Response run(Text instruction) {
      if (answers.isEmpty()) {
        throw new RuntimeException("No more answers");
      }
      uncommittedHistory.add(new Message(MessageType.REQUEST, instruction));
      uncommittedHistory.add(new Message(MessageType.RESPONSE, new Text(answers.getFirst(), "")));
      return new Response(instruction, answers.removeFirst());
    }

    @Override
    public SpaceTalkListener getSpaceTalkListener() {
      return null;
    }
  };

  private class TestTextToSpeechClient implements TextToSpeechClient {
    private List<Entry> answers;

    public void setAnswers(List<Entry> entries) {
      answers = new ArrayList<>(entries);
    }

    private record Entry (long nextEstimatedDurationMs, long nextDurationMs) { }

    public void nextAnswer() {
      answers.removeFirst();
    }



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
      if (answers.isEmpty()) {
        throw new RuntimeException("No more answers");
      }
      Entry e = answers.getFirst();
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

