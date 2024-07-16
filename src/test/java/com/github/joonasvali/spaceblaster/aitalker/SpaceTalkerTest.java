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
      public void onIntermediaryCommentaryCompleted(String result, int periodIndex, long timeSinceEventMs) {
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
    llmAnswers.add("Welcome to the game.");
    llmAnswers.add("Extra time 1.");
    llmAnswers.add("Game starts.");
    llmAnswers.add("Extra time 2.");
    llmAnswers.add("Player dies.");
    llmAnswers.add("Player born.");
    llmAnswers.add("Player no longer invincible.");
    llmAnswers.add("Player dies.");
    llmAnswers.add("Player born.");
    llmAnswers.add("Enemy killed!");
    llmAnswers.add("Game over!");
    llmAnswers.add("Game over!");

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(21865, 21865),
        new TestTextToSpeechClient.Entry(2000, 2000),
        new TestTextToSpeechClient.Entry(2926, 2926),
        new TestTextToSpeechClient.Entry(1000, 1000),
        new TestTextToSpeechClient.Entry(1150, 1150),
        new TestTextToSpeechClient.Entry(2587, 2587),
        new TestTextToSpeechClient.Entry(1489, 1489),
        new TestTextToSpeechClient.Entry(1698, 1698),
        new TestTextToSpeechClient.Entry(2352, 2352),
        new TestTextToSpeechClient.Entry(967, 967),
        new TestTextToSpeechClient.Entry(15909, 15909),
        new TestTextToSpeechClient.Entry(3000, 3000)
    );

    String expected = """
        0 21865 23899 Welcome to the game. ${PROJECT_DIR}\\0 - 0.wav
        23899 2926 4095 Game starts. ${PROJECT_DIR}\\1 - 23899.wav
        27994 1150 2399 Player dies. ${PROJECT_DIR}\\2 - 27994.wav
        30393 2587 3202 Player born. ${PROJECT_DIR}\\3 - 30393.wav
        33595 1489 1489 Player no longer invincible. ${PROJECT_DIR}\\4 - 33595.wav
        35084 1698 1698 Player dies. ${PROJECT_DIR}\\5 - 35084.wav
        36782 2352 2352 Player born. ${PROJECT_DIR}\\6 - 36782.wav
        39134 967 967 Enemy killed! ${PROJECT_DIR}\\7 - 39134.wav
        40101 15909 18324 Game over! ${PROJECT_DIR}\\8 - 40101.wav
        """;

    runTest("./short-run/short-run.yml", llmAnswers, ttsAnswers, expected, 58425);
  }

  @Test
  public void testSpaceTalk2() throws IOException {
    final int fails = 4;
    final int intermediary = 1;
    List<String> llmAnswers = new ArrayList<>();
    for (int i = 0; i < 49 + fails + intermediary; i++) {
      llmAnswers.add("LLM answer " + i);
    }

    List<TestTextToSpeechClient.Entry> ttsAnswers = List.of(
        new TestTextToSpeechClient.Entry(23906, 23906),

        // 1
        new TestTextToSpeechClient.Entry(5982, 5982),
        new TestTextToSpeechClient.Entry(4892, 4892),
        // End 1

        // 2
        new TestTextToSpeechClient.Entry(4075, 4075),
        new TestTextToSpeechClient.Entry(3001, 3001),
        // End 2

        new TestTextToSpeechClient.Entry(2273, 2273),
        new TestTextToSpeechClient.Entry(2168, 2168),

        // Intermediary period of 4
        new TestTextToSpeechClient.Entry(1000, 1000),
        // End of Intermediary period of 4

        new TestTextToSpeechClient.Entry(3129, 3129),
        new TestTextToSpeechClient.Entry(2735, 2735),
        new TestTextToSpeechClient.Entry(1384, 1384),
        new TestTextToSpeechClient.Entry(2730, 2730),
        new TestTextToSpeechClient.Entry(2598, 2598),
        new TestTextToSpeechClient.Entry(2774, 2774),
        new TestTextToSpeechClient.Entry(6945, 6945),
        new TestTextToSpeechClient.Entry(4366, 4366),
        new TestTextToSpeechClient.Entry(7150, 7150),
        new TestTextToSpeechClient.Entry(2783, 2783),
        new TestTextToSpeechClient.Entry(2250, 2250),
        new TestTextToSpeechClient.Entry(4669, 4669),
        new TestTextToSpeechClient.Entry(4315, 4315),
        new TestTextToSpeechClient.Entry(2137, 2137),
        new TestTextToSpeechClient.Entry(2848, 2848),
        new TestTextToSpeechClient.Entry(4015, 4015),
        new TestTextToSpeechClient.Entry(1384, 1384),
        new TestTextToSpeechClient.Entry(1071, 1071),
        new TestTextToSpeechClient.Entry(1544, 1544),
        new TestTextToSpeechClient.Entry(2117, 2117),
        new TestTextToSpeechClient.Entry(2250, 2250),
        new TestTextToSpeechClient.Entry(2753, 2753),
        new TestTextToSpeechClient.Entry(2081, 2081),
        new TestTextToSpeechClient.Entry(3816, 3816),
        new TestTextToSpeechClient.Entry(2450, 2450),
        new TestTextToSpeechClient.Entry(2084, 2084),
        new TestTextToSpeechClient.Entry(4499, 4499),
        new TestTextToSpeechClient.Entry(2835, 2835),
        new TestTextToSpeechClient.Entry(2467, 2467),
        new TestTextToSpeechClient.Entry(2216, 2216),
        new TestTextToSpeechClient.Entry(1332, 1332),
        new TestTextToSpeechClient.Entry(1934, 1934),
        new TestTextToSpeechClient.Entry(3202, 3202),
        new TestTextToSpeechClient.Entry(3048, 3048),
        new TestTextToSpeechClient.Entry(1933, 1933),
        new TestTextToSpeechClient.Entry(3483, 3483),
        new TestTextToSpeechClient.Entry(5000, 5000),
        new TestTextToSpeechClient.Entry(4151, 4151),
        new TestTextToSpeechClient.Entry(1585, 1585),
        new TestTextToSpeechClient.Entry(2400, 2400),
        new TestTextToSpeechClient.Entry(2698, 2698),
        new TestTextToSpeechClient.Entry(2033, 2033),
        new TestTextToSpeechClient.Entry(1280, 1280),

        // 49
        new TestTextToSpeechClient.Entry(24000, 24000),
        new TestTextToSpeechClient.Entry(23000, 23000),
        new TestTextToSpeechClient.Entry(19000, 19000)
        // End 49
    );

    String expected = """
        0 23906 23906 LLM answer 0 ${PROJECT_DIR}\\0 - 0.wav
        23906 4075 4558 LLM answer 3 ${PROJECT_DIR}\\1 - 23906.wav
        28464 3001 3119 LLM answer 4 ${PROJECT_DIR}\\2 - 28464.wav
        31583 2273 3366 LLM answer 5 ${PROJECT_DIR}\\3 - 31583.wav
        34949 2168 2667 LLM answer 6 ${PROJECT_DIR}\\4 - 34949.wav
        37616 3129 3917 LLM answer 7 ${PROJECT_DIR}\\5 - 37616.wav
        41533 2735 2735 LLM answer 8 ${PROJECT_DIR}\\6 - 41533.wav
        44268 1384 1384 LLM answer 9 ${PROJECT_DIR}\\7 - 44268.wav
        45652 2730 2730 LLM answer 10 ${PROJECT_DIR}\\8 - 45652.wav
        48382 2598 2598 LLM answer 11 ${PROJECT_DIR}\\9 - 48382.wav
        50980 2774 2774 LLM answer 12 ${PROJECT_DIR}\\10 - 50980.wav
        53754 6945 6945 LLM answer 13 ${PROJECT_DIR}\\11 - 53754.wav
        60699 4366 4366 LLM answer 14 ${PROJECT_DIR}\\12 - 60699.wav
        65065 7150 7150 LLM answer 15 ${PROJECT_DIR}\\13 - 65065.wav
        72215 2783 2783 LLM answer 16 ${PROJECT_DIR}\\14 - 72215.wav
        74998 2250 2250 LLM answer 17 ${PROJECT_DIR}\\15 - 74998.wav
        77248 4669 4669 LLM answer 18 ${PROJECT_DIR}\\16 - 77248.wav
        81917 4315 4315 LLM answer 19 ${PROJECT_DIR}\\17 - 81917.wav
        86232 2137 2137 LLM answer 20 ${PROJECT_DIR}\\18 - 86232.wav
        88369 2848 2848 LLM answer 21 ${PROJECT_DIR}\\19 - 88369.wav
        91217 4015 4015 LLM answer 22 ${PROJECT_DIR}\\20 - 91217.wav
        95232 1384 1384 LLM answer 23 ${PROJECT_DIR}\\21 - 95232.wav
        96616 1071 1071 LLM answer 24 ${PROJECT_DIR}\\22 - 96616.wav
        97687 1544 1544 LLM answer 25 ${PROJECT_DIR}\\23 - 97687.wav
        99231 2117 2117 LLM answer 26 ${PROJECT_DIR}\\24 - 99231.wav
        101348 2250 2250 LLM answer 27 ${PROJECT_DIR}\\25 - 101348.wav
        103598 2753 2753 LLM answer 28 ${PROJECT_DIR}\\26 - 103598.wav
        106351 2081 2081 LLM answer 29 ${PROJECT_DIR}\\27 - 106351.wav
        108432 3816 3816 LLM answer 30 ${PROJECT_DIR}\\28 - 108432.wav
        112248 2450 2450 LLM answer 31 ${PROJECT_DIR}\\29 - 112248.wav
        114698 2084 2084 LLM answer 32 ${PROJECT_DIR}\\30 - 114698.wav
        116782 4499 4499 LLM answer 33 ${PROJECT_DIR}\\31 - 116782.wav
        121281 2835 2835 LLM answer 34 ${PROJECT_DIR}\\32 - 121281.wav
        124116 2467 2467 LLM answer 35 ${PROJECT_DIR}\\33 - 124116.wav
        126583 2216 2216 LLM answer 36 ${PROJECT_DIR}\\34 - 126583.wav
        128799 1332 1332 LLM answer 37 ${PROJECT_DIR}\\35 - 128799.wav
        130131 1934 1934 LLM answer 38 ${PROJECT_DIR}\\36 - 130131.wav
        132065 3202 3202 LLM answer 39 ${PROJECT_DIR}\\37 - 132065.wav
        135267 3048 3048 LLM answer 40 ${PROJECT_DIR}\\38 - 135267.wav
        138315 1933 1933 LLM answer 41 ${PROJECT_DIR}\\39 - 138315.wav
        140248 3483 3483 LLM answer 42 ${PROJECT_DIR}\\40 - 140248.wav
        143731 5000 5000 LLM answer 43 ${PROJECT_DIR}\\41 - 143731.wav
        148731 4151 4151 LLM answer 44 ${PROJECT_DIR}\\42 - 148731.wav
        152882 1585 1585 LLM answer 45 ${PROJECT_DIR}\\43 - 152882.wav
        154467 2400 2400 LLM answer 46 ${PROJECT_DIR}\\44 - 154467.wav
        156867 2698 2698 LLM answer 47 ${PROJECT_DIR}\\45 - 156867.wav
        159565 2033 2033 LLM answer 48 ${PROJECT_DIR}\\46 - 159565.wav
        161598 1280 1280 LLM answer 49 ${PROJECT_DIR}\\47 - 161598.wav
        162878 19000 19000 LLM answer 52 ${PROJECT_DIR}\\48 - 162878.wav
        """;

    runTest("./long-run/long-run.yml", llmAnswers, ttsAnswers, expected, 181878);
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

