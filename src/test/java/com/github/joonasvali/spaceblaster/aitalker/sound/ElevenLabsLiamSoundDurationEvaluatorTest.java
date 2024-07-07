package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsLiamSoundDurationEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElevenLabsLiamSoundDurationEvaluatorTest {
  SoundDurationEvaluator evaluator = new ElevenLabsLiamSoundDurationEvaluator();

  @Test
  public void testA() {
    long duration = evaluator.evaluateDurationInMs(
        """
        Ladies and Gentlemen, welcome to another thrilling episode of "Space Blaster"! Today, we're diving into 
        the "Default" episode, set at an approachable EASY difficulty—perfect for newcomers and a warm-up for 
        seasoned veterans alike. With 5 challenging levels ahead, our players are poised for an epic 
        showdown that promises intergalactic excitement. Prepare yourselves, because the adventure is about 
        to commence!
        """
    );
    int expected = 26000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);

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
    int expected = 30000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);
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

    int expected = 25000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);
  }

  @Test
  public void testD() {
    long duration = evaluator.evaluateDurationInMs(
        """
        First enemy down! Nine left as our hero scores 150 points. The action intensifies!
        """
    );
    int expected = 6000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);
  }

  @Test
  public void testE() {
    long duration = evaluator.evaluateDurationInMs(
        """
        First enemy down! Nine left. The action intensifies!
        """
    );
    int expected = 4000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);

  }


  @Test
  public void testF() {
    long duration = evaluator.evaluateDurationInMs(
        """
        Five down, five to go! Score: 650!
        """
    );
    int expected = 4000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);

  }

  @Test
  public void testG() {
    long duration = evaluator.evaluateDurationInMs(
        """
        Enemy hit!
        """
    );
    int expected = 1000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f,  duration + " != " + expected);

  }



}
