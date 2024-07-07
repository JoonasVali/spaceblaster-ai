package com.github.joonasvali.spaceblaster.aitalker.sound;

/**
 * This class is used to evaluate the approximate duration of a sound file based on text alone, before the sound file
 * is generated. This allows us to save money by not generating sound files that are too long (or short).
 */
public interface SoundDurationEvaluator {
  long evaluateDurationInMs(String sound);
}
