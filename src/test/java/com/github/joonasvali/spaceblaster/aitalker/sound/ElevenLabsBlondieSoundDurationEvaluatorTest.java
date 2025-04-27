package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsBlondieSoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsMimiSoundDurationEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElevenLabsBlondieSoundDurationEvaluatorTest {
  SoundDurationEvaluator evaluator = new ElevenLabsBlondieSoundDurationEvaluator();

  @Test
  public void testA() {
    long duration = evaluator.evaluateDurationInMs(
        """
            Heyyy everyone! Welcome back to, like, the super cool... Space Invader thingy!
            So, um, we’re about to go on this big trip... in space? Like, with stars and stuff. It’s gonna be so cute.
            We’re starting on, like, the first part — it’s the easy one, thank goodness, ‘cause hard stuff is, like, stressful, you know?
            Our player — they’re super brave and probably, like, really good at this — has to beat, like, FIVE whole levels. That’s, like, a lot, omg.
            So don’t go get snacks yet, ‘cause it’s about to start!
            Let’s gooo space people!! Wooo!!
            """
    );
    int expected = 41000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }

  @Test
  public void testB() {
    long duration = evaluator.evaluateDurationInMs(
        """
            "Oh-em-gee, you guys, that was, like, sooo intense!
            Player One totally took down that bad guy — like, boom, pew pew — and they hit, like, 89% of their shots? Which sounds, like, really good honestly.
            Buuut, like, even though they fought, like, super hard — it’s, um, game over. Sad face!
            It was, like, 19 seconds of nonstop craziness, I swear it went by faster than doing my mascara.
            Player One, you were, like, amazing, and we cannot wait for you to come back and, like, totally crush it again!! Woo!!
            """
    );
    int expected = 39000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }

  @Test
  public void testC() {
    long duration = evaluator.evaluateDurationInMs(
        """
            OMG, you guys, Player One totally got the first bad guy — like, bye! — and they hit, like, 89% of their shots, which is, like, basically an A+, right?!
            """
    );
    int expected = 12000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }

}
