package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsFinSoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsMimiSoundDurationEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// TODO - divider of 7 is kind of imprecise.
public class ElevenLabsFinSoundDurationEvaluatorTest {
  public static final float PRECISION_DIVIDER = 7f;
  SoundDurationEvaluator evaluator = new ElevenLabsFinSoundDurationEvaluator();

  @Test
  public void testA() {
    long duration = evaluator.evaluateDurationInMs(
        """
        And we’re off! The player starts with their trusty laser cannon at the bottom of the screen. They’re moving
        left and right, taking shots at the descending invaders. Look at that precision! Every shot counts here.
        """
    );
    int expected = 15000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / PRECISION_DIVIDER,  duration + " != " + expected);

  }

  @Test
  public void testB() {
    long duration = evaluator.evaluateDurationInMs(
        """
        What an intense finish, dear viewers! Player One managed to down the first enemy and kept a remarkable 89% hit 
        rate. Despite a tenacious fight, it's game over. An incredible 19 seconds of action-packed gameplay. Player One, we look forward to your return!
        """
    );
    int expected = 23000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / PRECISION_DIVIDER,  duration + " != " + expected);

  }

  @Test
  public void testC() {
    long duration = evaluator.evaluateDurationInMs(
        """
        And we’re off! Enemy down!
        """
    );
    int expected = 2500;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / PRECISION_DIVIDER,  duration + " != " + expected);

  }

  @Test
  public void testD() {
    long duration = evaluator.evaluateDurationInMs(
        """
        Player One managed to down the first enemy and kept a remarkable 89% hit rate.
        """
    );
    int expected = 6500;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / PRECISION_DIVIDER,  duration + " != " + expected);

  }

}
