package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.event.AbandonShortenSpeechEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.CommentaryFailedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingCompletedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.PeriodProcessingStartedEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.ResoluteShorteningMessageEvent;
import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;
import com.github.joonasvali.spaceblaster.aitalker.llm.OpenAIClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsLiamRoastVoiceSettings;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsMp3Output;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.SpaceBlasterVoiceSettings;
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
  private static final String SOUND_OUTPUT_DIRECTORY_ROOT = "K:\\spaceblaster-projects\\1";

  /**
   * Path to the event data file. Change as needed.
   */
  public static final String EVENT_DATA_PATH = "K:\\spaceblaster-projects\\1\\events-1718825797443.yml";
  public static final SpaceBlasterVoiceSettings VOICE_SETTINGS = new ElevenLabsLiamRoastVoiceSettings();

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
      public void onCommentaryFailed(CommentaryFailedEvent event) {
        logger.debug("Failed to generate commentary. (" + event.attempt() + ")");
      }

      @Override
      public void onPeriodProcessingStarted(PeriodProcessingStartedEvent event) {

      }

      @Override
      public void onPeriodProcessingCompleted(PeriodProcessingCompletedEvent event) {
        long audioEnd = event.generatedAudioRelativeStartTime() + event.generatedAudioDurationMs();
        String silence = "";
        if (audioEnd < event.periodRelativeStartTime() + event.periodDuration()) {
          if (event.periodIndex() != 0) {
            silence = "Silence: " + audioEnd + " -> " + (event.periodRelativeStartTime() + event.periodDuration()) + " (" + (event.periodRelativeStartTime() + event.periodDuration() - audioEnd) + "ms) ";
          }
        }

        logger.info(
            event.periodIndex() + ": period " + event.periodRelativeStartTime() + " -> " +
                (event.periodRelativeStartTime() + event.periodDuration()) + " completed " +
                (event.retryAttempts() > 0 ? ("(in " + event.retryAttempts() + " attempts)"): "") +
            " Audio: " + event.generatedAudioDurationMs() + "ms, starting at: " + event.generatedAudioRelativeStartTime() + "ms. " +
                (event.inputLatency() > 0 ? event.inputLatency() + "ms latency. " : "") +
                silence +
                "Result \"" + event.result() + "\""
            );


        if (logger.isDebugEnabled()) {
          logger.debug("Prompt: " + event.inputText());
        }
      }

      @Override
      public void onResoluteShorteningMessage(ResoluteShorteningMessageEvent event) {
        logger.debug("Trying resolutely force a shorter message. (" + event.attempt() + ")");
      }

      @Override
      public void onAbandonShortenSpeech(AbandonShortenSpeechEvent event) {
        logger.debug("Abandoning shortening speech. (" + event + ")");
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