package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class SoundDurationEvaluatorSpeedModifier implements SoundDurationEvaluator {
  private final SoundDurationEvaluator baseEvaluator;

  private final float speedModifier;

  public SoundDurationEvaluatorSpeedModifier(SoundDurationEvaluator baseEvaluator, float speedModifier) {
    this.baseEvaluator = baseEvaluator;
    this.speedModifier = speedModifier;
  }

  @Override
  public long evaluateDurationInMs(String sound) {
    long baseDuration = baseEvaluator.evaluateDurationInMs(sound);
    return Math.round(baseDuration / speedModifier);
  }
}
