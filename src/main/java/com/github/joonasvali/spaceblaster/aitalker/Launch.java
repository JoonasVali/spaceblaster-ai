package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.llm.OpenAIClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsFinVoiceSettings;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsMp3Output;
import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.EventReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Launch {
  private static final Logger logger = LoggerFactory.getLogger(Launch.class);


  /**
   * Path to the root directory where the sound files and final output will be saved. Change as needed.
   */
  private static final String SOUND_OUTPUT_DIRECTORY_ROOT = "// !!! Provide a path here //";

  /**
   * Path to the event data file. Change as needed.
   */
  public static final String EVENT_DATA_PATH = "// !!! Provide a path here //";
  public static final ElevenLabsFinVoiceSettings VOICE_SETTINGS = new ElevenLabsFinVoiceSettings();

  public static void main(String[] args) throws IOException {
    Launch main = new Launch();
    main.launch();
  }

  private void launch() throws IOException {
    List<Event> events = getEvents();

    EventDigester eventDigester = new EventDigester(events, true);

    List<Period> periods = new ArrayList<>();
    while (eventDigester.hasNextPeriod()) {
      Period period = eventDigester.getNextPeriod();
      periods.add(period);
    }

    TextToSpeechClient textToSpeechClient = new ElevenLabsClient(VOICE_SETTINGS, new ElevenLabsMp3Output());

    Path dir = Paths.get(SOUND_OUTPUT_DIRECTORY_ROOT);
    Files.createDirectories(dir);
    OpenAIClient openAIClient = new OpenAIClient();
    SpaceTalker spaceTalker = new SpaceTalker(textToSpeechClient, new OpenAIClient(), dir);
    spaceTalker.addListener(new SpaceTalkListener() {

      @Override
      public void onCommentaryFailed(String lastOutputMessage, int attempt, long timeSinceEventMs) {
        logger.info("Commentary failed (" + attempt + "): " + lastOutputMessage);
      }

      @Override
      public void onFailToShortenSpeech(String lastOutputMessage, int attempt, long timeSinceEventMs) {
        logger.info("Failed to shorten speech. (" + attempt + ")");
        logger.info("  speech: " + lastOutputMessage);
      }

      @Override
      public void onPeriodProcessingCompleted(String result, int periodIndex, long timeSinceEventMs) {
        logger.info("Period " + periodIndex + " completed: " + result);
      }

      @Override
      public void onIntermediaryCommentaryCompleted(String result, int periodIndex, long timeSinceEventMs) {
        logger.info("Intermediary commentary completed: " + result);
      }

      @Override
      public void onResoluteShorteningMessage(String result, long duration, long limitDuration, int attempt, long timeSinceEventMs) {
        logger.info("Trying resolutely force a shorter message. (" + attempt + ")");
      }

      @Override
      public void onAbandonShortenSpeech(String output, int attempt, long timeSinceEventMs) {
        logger.info("Abandoning shortening speech. (" + attempt + ")");
      }
    });

    spaceTalker.run(periods, "Player one", System.currentTimeMillis() + "-PlayerOne");

    logger.info("Tokens used: " + openAIClient.getTokensUsed());
    logger.info("Completion tokens used: " + openAIClient.getCompletionTokensUsed());
    logger.info("Prompt tokens used: " + openAIClient.getPromptTokensUsed());
  }

  public List<Event> getEvents() {
    Path path = Paths.get(EVENT_DATA_PATH);
    try (InputStream inputStream = new FileInputStream(path.toFile())) {

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
}