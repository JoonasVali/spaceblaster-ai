package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsOxleySoundDurationEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElevenLabsOxleySoundDurationEvaluatorTest {
  SoundDurationEvaluator evaluator = new ElevenLabsOxleySoundDurationEvaluator();

  @Test
  public void testA() {
    long duration = evaluator.evaluateDurationInMs(
        """
            Power missed; enemy falls and formation shifts. Our cannon endures.
            """
    );
    int expected = 7000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }

  @Test
  public void testB() {
    long duration = evaluator.evaluateDurationInMs(
        """
            Ladies and gentlemen, welcome to the intergalactic spectacle known as Space Blaster! Today's episode brings
            us to the default setting of this thrilling cosmic adventure. With the game difficulty set to EASY and five
            levels awaiting our brave players, we are on the cusp of witnessing some out-of-this-world action. Stay
            tuned as we embark on a journey through the vast expanse of space, facing challenges and foes at every
            turn. Get ready to blast off into a gaming experience like no other!
            """
    );
    int expected = 49000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);
  }


  @Test
  public void testC() {
    long duration = evaluator.evaluateDurationInMs(
        """
            Ladies and gentlemen, welcome back to our thrilling space invaders showdown! We are about to embark on a
            journey through the cosmos in this game experience, starting at the default episode with an easy game
            difficulty. Our brave player will be facing a total of 5 challenging levels in their quest for victory.
            Stay with us as the action is about to begin! Let the space invasion madness commence!
            """
    );

    int expected = 41000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);
  }
}
