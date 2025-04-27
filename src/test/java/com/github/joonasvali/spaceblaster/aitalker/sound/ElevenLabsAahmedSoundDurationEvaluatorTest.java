package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsAahmedSoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsCallumSoundDurationEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElevenLabsAahmedSoundDurationEvaluatorTest {
  SoundDurationEvaluator evaluator = new ElevenLabsAahmedSoundDurationEvaluator();

  @Test
  public void testA() {
    long duration = evaluator.evaluateDurationInMs(
        """
            YO YO YO, LISTEN UP, LEGENDS IN THE MAKING!!
            Strap in, slap on your backwards caps, and crush that energy drink, 'cause we’re about to BLAST OFF into the most insane SPACE SMACKDOWN of your life, bro!!
            We’re kicking things off straight savage mode — Episode 1, difficulty easier than Coach’s pop quizzes, but don’t sleep on it, dude, 'cause it’s about to get real spicy!
            Our absolute UNIT of a player is gonna throw hands across FIVE hardcore levels, each one hotter than a two-a-day practice in July, my dudes.
            SO STAY LOCKED IN, FLEX THOSE GAMER MUSCLES, 'cause the SPACE INVASION is about to go full SEND, and it’s gonna be STUPID LIT, BROOOOO!! LET’S GOOOOOO!!!
            """
    );
    int expected = 36000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }


  @Test
  public void testB() {
    long duration = evaluator.evaluateDurationInMs(
        """
            First enemy down! Nine left as our hero scores 150 points. The action intensifies!
            """
    );
    int expected = 5000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);
  }
}
