package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;
import com.github.joonasvali.spaceblaster.aitalker.llm.LLMClient;
import com.github.joonasvali.spaceblaster.aitalker.llm.OpenAIClient;
import com.github.joonasvali.spaceblaster.aitalker.llm.Text;
import com.github.joonasvali.spaceblaster.aitalker.openai.ImageAnalysis;
import com.github.joonasvali.spaceblaster.aitalker.openai.ProcessingResult;
import com.github.joonasvali.spaceblaster.aitalker.sound.AudioTrackBuilder;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsCallumRoastVoiceSettings;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsTextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.TextToSpeech;
import com.github.joonasvali.spaceblaster.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SpaceTalker2 {
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

  public static final String OUTPUT_SOUND_FILE_SUFFIX = ".wav";
  public static final String OUTPUT_SOUND_FILE_NAME = "final";

  private final LLMClient llmClient;
  private final TextToSpeechClient textToSpeechClient;
  private final AudioTrackBuilder audioTrackBuilder;
  private final Path outputRootDirectory;
  private final ArrayList<SpaceTalkListener> listeners = new ArrayList<>();
  private Long extraTime = 0L;
  private final Path screenshotFolder;

  public SpaceTalker2(TextToSpeechClient textToSpeechClient, LLMClient llmClient, Path outputRootDirectory, Path screenshotFolder) {
    this.screenshotFolder = screenshotFolder;
    this.outputRootDirectory = outputRootDirectory;
    this.llmClient = llmClient;
    this.textToSpeechClient = textToSpeechClient;
    this.audioTrackBuilder = new AudioTrackBuilder(textToSpeechClient.getSampleRate(), true);

    if (llmClient.getSpaceTalkListener() != null) {
      this.listeners.add(llmClient.getSpaceTalkListener());
    }
  }

  public SpaceTalker.SpaceTalk run(List<Event> events, String playerName, String projectName) throws IOException {
    if (events.isEmpty()) {
      logger.warn("No events to process");
      return null;
    }
    Event firstEvent = events.get(0);
    String firstInstructions = getGameIntroductionInstructions(firstEvent, playerName, firstEvent.eventTimestamp - extraTime);
    System.out.println(events);


    return null;
  }

  public void addListener(SpaceTalkListener spaceTalkListener) {
    // TODO
  }

  public BufferedImage getEventScreenshot(Event event) {
    Path screenshotPath = screenshotFolder.resolve(event.eventTimestamp + ".png");
    BufferedImage screenshot = null;
    if (Files.exists(screenshotPath)) {
      try {
        screenshot = ImageIO.read(screenshotPath.toFile());
      } catch (IOException e) {
        logger.error("Failed to read screenshot: " + screenshotPath, e);
        return null;
      }
    }
    return screenshot;
  }

  private String getGameIntroductionInstructions(Event firstEvent, String playerName, long durationMs) {
    String description = null;
    BufferedImage screenshot = getEventScreenshot(firstEvent);
    if (screenshot != null) {
      ImageAnalysis analysis = new ImageAnalysis("Describe this screenshot from SpaceBlaster game (a space invaders clone). The current game state is important.");
      try {
        ProcessingResult<String> imageDescription = analysis.process(screenshot);
        description = imageDescription.content();
        System.out.println("DEBUG IMAGE DESC: " + imageDescription.content());
      } catch (IOException e) {
        logger.error("Can not get image description from openAI", e);
      }
    }

    String shortTerm = String.format("""
        The game screen has loaded and is on pause.
        You now have %d seconds to make an introduction. The event data follows:
        Player name: %s
        Game name: Space Blaster
        Episode name: %s
        Game difficulty: %s
        Total levels: %d
        
        description: %s
        """, (int) Math.floor(durationMs / 1000d), playerName, firstEvent.episodeName, firstEvent.gameDifficulty.toString(), firstEvent.totalRoundsCount, description
    );

    return shortTerm;
  }
}
