package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs.ElevenLabsProtossSoundDurationEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElevenLabsProtossSoundDurationEvaluatorTest {
  SoundDurationEvaluator evaluator = new ElevenLabsProtossSoundDurationEvaluator();

  @Test
  public void testA() {
    long duration = evaluator.evaluateDurationInMs(
        """
            From the shadows we rise, unseen and unbound. The light has forsaken this path, but we walk it still—with blade, with silence, with purpose. We are vengeance made flesh. Let the void be our guide, and fear be our herald. They will never see us… only feel the end.
            """
    );
    int expected = 21000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }

  @Test
  public void testB() {
    long duration = evaluator.evaluateDurationInMs(
        """
            The shadows are my armor. The void, my blade. You won’t see me… only the end.
            """
    );
    int expected = 8000;
    Assertions.assertTrue(Math.abs(duration - expected) < expected / 10f, duration + " != " + expected);

  }


}
