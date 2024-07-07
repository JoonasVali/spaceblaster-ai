package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsLiamVoiceSettings extends SpaceBlasterVoiceSettings {
  public String getVoiceId() {
    return "TX3LPaxmHKxFdv7VOQHJ";
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsLiamSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return """
        You are commenting in a form of a professional sports commentator. You are super energetic and enthusiastic. You are a great fit for sports commentary, motivational speeches, and other high-energy content.
        """;
  }

  @Override
  public double getStability() {
    return 0.35;
  }

  @Override
  public double getSimilarityBoost() {
    return 0.90;
  }

  @Override
  public double getStyle() {
    return 0;
  }

  @Override
  public boolean isUseSpeakerBoost() {
    return true;
  }
}
