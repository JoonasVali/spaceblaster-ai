package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingCompletedEvent;
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
import java.util.UUID;

public class SpaceTalkerTest {
  @TempDir
  public static Path tempDir;

  public void runTest(String eventFilePath, List<TestLLMClient.Entry> llmAnswers, List<TestTextToSpeechClient.Entry> ttsAnswers, String expectedOutput, long finalDurationMs) throws IOException {
    List<Event> events = getEvents(eventFilePath);

    List<TestLLMClient.Entry> answers = new ArrayList<>(llmAnswers);

    TestTextToSpeechClient speech = new TestTextToSpeechClient();
    speech.setAnswers(ttsAnswers);

    TestLLMClient llmClient = new TestLLMClient(answers);
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
      public void onPeriodProcessingCompleted(PeriodProcessingCompletedEvent event) {
        speech.nextAnswer();
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

        llmClient.increasePeriodIndexBeingProcessed();
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
  public void testSpaceTalk1() throws IOException {

    List<TestLLMClient.Entry> llmAnswers = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
    }

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(0, 20000, 20000),
        new TestTextToSpeechClient.Entry(1, 4095, 4095),
        new TestTextToSpeechClient.Entry(2, 2300, 2300),
        new TestTextToSpeechClient.Entry(3, 3200, 3200),
        new TestTextToSpeechClient.Entry(4, 2300, 2300),
        new TestTextToSpeechClient.Entry(5, 3200, 3200),
        new TestTextToSpeechClient.Entry(6, 15000, 15000)
    );

    String expected = """
        0 20000 23899 LLM answer 0 ${PROJECT_DIR}\\0 - 0.wav
        23899 4095 4095 LLM answer 1 ${PROJECT_DIR}\\1 - 23899.wav
        27994 2300 2399 LLM answer 2 ${PROJECT_DIR}\\2 - 27994.wav
        30393 3200 3205 LLM answer 3 ${PROJECT_DIR}\\3 - 30393.wav
        33598 2300 2394 LLM answer 4 ${PROJECT_DIR}\\4 - 33598.wav
        35992 3200 3271 LLM answer 5 ${PROJECT_DIR}\\5 - 35992.wav
        39263 15000 20000 LLM answer 6 ${PROJECT_DIR}\\6 - 39263.wav
        """;

    runTest("./short-run/short-run.yml", llmAnswers, ttsAnswers, expected, 59263);
  }

  @Test
  public void testSpaceTalk2() throws IOException {

    int[] failIndexes = new int[] { 12 };
    List<TestLLMClient.Entry> llmAnswers = new ArrayList<>();
    for (int i = 0; i < 23; i++) {
      int finalI = i;
      if (Arrays.stream(failIndexes).anyMatch((x) -> x == finalI)) {
        // Add three fails
        llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
        llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
        llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
      }
      llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
    }

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(0, 16000, 16000),
        new TestTextToSpeechClient.Entry(1, 10000, 10000),
        new TestTextToSpeechClient.Entry(2, 2000, 2000),
        new TestTextToSpeechClient.Entry(3, 14000, 14000),
        new TestTextToSpeechClient.Entry(4, 4000, 4000),
        new TestTextToSpeechClient.Entry(5, 3000, 3000),
        new TestTextToSpeechClient.Entry(6, 3000, 3000),
        new TestTextToSpeechClient.Entry(7, 2000, 2000),
        new TestTextToSpeechClient.Entry(8, 2000, 2000),
        new TestTextToSpeechClient.Entry(9, 2000, 2000),
        new TestTextToSpeechClient.Entry(10, 2000, 2000),
        new TestTextToSpeechClient.Entry(11, 8000, 8000),
        new TestTextToSpeechClient.Entry(12, 6000, 6000),
        new TestTextToSpeechClient.Entry(12, 6000, 6000),
        new TestTextToSpeechClient.Entry(12, 6000, 6000),
        new TestTextToSpeechClient.Entry(12, 6000, 6000),
        new TestTextToSpeechClient.Entry(13, 4000, 4000),
        new TestTextToSpeechClient.Entry(14, 5000, 5000),
        new TestTextToSpeechClient.Entry(15, 2000, 2000),
        new TestTextToSpeechClient.Entry(16, 3000, 3000),
        new TestTextToSpeechClient.Entry(17, 4000, 4000),
        new TestTextToSpeechClient.Entry(18, 9000, 9000),
        new TestTextToSpeechClient.Entry(19, 2000, 2000),
        new TestTextToSpeechClient.Entry(20, 2000, 2000),
        new TestTextToSpeechClient.Entry(21, 5000, 5000),
        new TestTextToSpeechClient.Entry(22, 5000, 5000),
        new TestTextToSpeechClient.Entry(23, 15000, 15000)

    );

    String expected = """
        0 16000 23906 LLM answer 0 ${PROJECT_DIR}\\0 - 0.wav
        23906 10000 18942 LLM answer 1 ${PROJECT_DIR}\\1 - 23906.wav
        42848 2000 2000 LLM answer 2 ${PROJECT_DIR}\\2 - 42848.wav
        44848 14000 19865 LLM answer 3 ${PROJECT_DIR}\\3 - 44848.wav
        64713 4000 10285 LLM answer 4 ${PROJECT_DIR}\\4 - 64713.wav
        74998 3000 3355 LLM answer 5 ${PROJECT_DIR}\\5 - 74998.wav
        78353 3000 16879 LLM answer 6 ${PROJECT_DIR}\\6 - 78353.wav
        95232 2000 2000 LLM answer 7 ${PROJECT_DIR}\\7 - 95232.wav
        97232 2000 2000 LLM answer 8 ${PROJECT_DIR}\\8 - 97232.wav
        99232 2000 2000 LLM answer 9 ${PROJECT_DIR}\\9 - 99232.wav
        101232 2000 2117 LLM answer 10 ${PROJECT_DIR}\\10 - 101232.wav
        103232 8000 11466 LLM answer 11 ${PROJECT_DIR}\\11 - 103232.wav
        114698 6000 2084 LLM answer 12 ${PROJECT_DIR}\\12 - 114698.wav
        116782 4000 7334 LLM answer 13 ${PROJECT_DIR}\\13 - 116782.wav
        124116 5000 5550 LLM answer 14 ${PROJECT_DIR}\\14 - 124116.wav
        129666 2000 2399 LLM answer 15 ${PROJECT_DIR}\\15 - 129666.wav
        132065 3000 7666 LLM answer 16 ${PROJECT_DIR}\\16 - 132065.wav
        139731 4000 4000 LLM answer 17 ${PROJECT_DIR}\\17 - 139731.wav
        143731 9000 9151 LLM answer 18 ${PROJECT_DIR}\\18 - 143731.wav
        152882 2000 2000 LLM answer 19 ${PROJECT_DIR}\\19 - 152882.wav
        154882 2000 2400 LLM answer 20 ${PROJECT_DIR}\\20 - 154882.wav
        156882 5000 5147 LLM answer 21 ${PROJECT_DIR}\\21 - 156882.wav
        162014 5000 20000 LLM answer 22 ${PROJECT_DIR}\\22 - 162014.wav
        """;

    runTest("./long-run/long-run.yml", llmAnswers, ttsAnswers, expected, 182546);
  }


  @Test
  public void testSpaceTalk3() throws IOException {

    int[] failIndexes = new int[] { 5 };
    List<TestLLMClient.Entry> llmAnswers = new ArrayList<>();
    for (int i = 0; i < 23; i++) {
      int finalI = i;
      if (Arrays.stream(failIndexes).anyMatch((x) -> x == finalI)) {
        // Add three fails
        llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
        llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
        llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
      }
      llmAnswers.add(new TestLLMClient.Entry(i, "LLM answer " + i));
    }

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(0, 18233, 18233),
        new TestTextToSpeechClient.Entry(1, 8855, 8855),
        new TestTextToSpeechClient.Entry(2, 1200, 1200),
        new TestTextToSpeechClient.Entry(3, 11311, 11311),
        new TestTextToSpeechClient.Entry(4, 6269, 6269),
        new TestTextToSpeechClient.Entry(5, 7706, 7706),
        new TestTextToSpeechClient.Entry(5, 7706, 7706),
        new TestTextToSpeechClient.Entry(5, 7706, 7706),
        new TestTextToSpeechClient.Entry(5, 7706, 7706),
        new TestTextToSpeechClient.Entry(6, 1951, 1951),
        new TestTextToSpeechClient.Entry(7, 2000, 2000),
        new TestTextToSpeechClient.Entry(8, 2000, 2000),
        new TestTextToSpeechClient.Entry(9, 2000, 2000),
        new TestTextToSpeechClient.Entry(10, 1000, 1000),
        new TestTextToSpeechClient.Entry(11, 5015, 5015),
        new TestTextToSpeechClient.Entry(12, 1000, 1000),
        new TestTextToSpeechClient.Entry(13, 4000, 4000),
        new TestTextToSpeechClient.Entry(14, 5400, 5400),
        new TestTextToSpeechClient.Entry(15, 2004, 2004),
        new TestTextToSpeechClient.Entry(16,  2821, 2821),
        new TestTextToSpeechClient.Entry(17, 3000, 3000),
        new TestTextToSpeechClient.Entry(18, 8000, 8000),
        new TestTextToSpeechClient.Entry(19, 2000, 2000),
        new TestTextToSpeechClient.Entry(20, 2000, 2000),
        new TestTextToSpeechClient.Entry(21, 5000, 5000),
        new TestTextToSpeechClient.Entry(22, 16248, 16248)
    );

    String expected = """
      0 18233 23906 LLM answer 0 ${PROJECT_DIR}\\0 - 0.wav
      23906 8855 18942 LLM answer 1 ${PROJECT_DIR}\\1 - 23906.wav
      42848 1200 2000 LLM answer 2 ${PROJECT_DIR}\\2 - 42848.wav
      44465 11311 20248 LLM answer 3 ${PROJECT_DIR}\\3 - 44465.wav
      64713 6269 10285 LLM answer 4 ${PROJECT_DIR}\\4 - 64713.wav
      74998 7706 3355 LLM answer 5 ${PROJECT_DIR}\\5 - 74998.wav
      78353 1951 16879 LLM answer 6 ${PROJECT_DIR}\\6 - 78353.wav
      95232 2000 2000 LLM answer 7 ${PROJECT_DIR}\\7 - 95232.wav
      97232 2000 2000 LLM answer 8 ${PROJECT_DIR}\\8 - 97232.wav
      99232 2000 2000 LLM answer 9 ${PROJECT_DIR}\\9 - 99232.wav
      101232 1000 2117 LLM answer 10 ${PROJECT_DIR}\\10 - 101232.wav
      102232 5015 12466 LLM answer 11 ${PROJECT_DIR}\\11 - 102232.wav
      114698 1000 2084 LLM answer 12 ${PROJECT_DIR}\\12 - 114698.wav
      116782 4000 7334 LLM answer 13 ${PROJECT_DIR}\\13 - 116782.wav
      124116 5400 5550 LLM answer 14 ${PROJECT_DIR}\\14 - 124116.wav
      129666 2004 2399 LLM answer 15 ${PROJECT_DIR}\\15 - 129666.wav
      132065 2821 7666 LLM answer 16 ${PROJECT_DIR}\\16 - 132065.wav
      139731 3000 4000 LLM answer 17 ${PROJECT_DIR}\\17 - 139731.wav
      143731 8000 9151 LLM answer 18 ${PROJECT_DIR}\\18 - 143731.wav
      152882 2000 2000 LLM answer 19 ${PROJECT_DIR}\\19 - 152882.wav
      154882 2000 2400 LLM answer 20 ${PROJECT_DIR}\\20 - 154882.wav
      156882 5000 5147 LLM answer 21 ${PROJECT_DIR}\\21 - 156882.wav
      162014 16248 20000 LLM answer 22 ${PROJECT_DIR}\\22 - 162014.wav                                                                                                                                                                                                                                      
      """;

    runTest("./long-run/long-run.yml", llmAnswers, ttsAnswers, expected, 183929);
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
    private final ArrayDeque<Entry> answers = new ArrayDeque<>();
    private int periodIndexBeingProcessed = 0;

    public TestLLMClient(List<Entry> answers) {
      this.answers.addAll(answers);
    }

    public void increasePeriodIndexBeingProcessed() {
      periodIndexBeingProcessed++;
    }

    private record Entry (int periodIndex, String text) { }

    @Override
    public Response run(Text instruction) {
      if (answers.isEmpty()) {
        throw new RuntimeException("No more answers");
      }
      Entry first = answers.removeFirst();
      if (first.periodIndex() != periodIndexBeingProcessed) {
        throw new RuntimeException("The period being processed is " + periodIndexBeingProcessed + " but returned answer for " + first.periodIndex + " INSTRUCTIONS: " + instruction);
      }
      uncommittedHistory.add(new Message(MessageType.REQUEST, instruction));
      uncommittedHistory.add(new Message(MessageType.RESPONSE, new Text(first.text, "")));
      return new Response(instruction, first.text);
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

    private record Entry (int periodIndex, long nextEstimatedDurationMs, long nextDurationMs) { }

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

