package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsFinVoiceSettings extends SpaceBlasterVoiceSettings {
  public String getVoiceId() {
    return "D38z5RcWu1voky8WS1ja";
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsFinSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return "You are commenting in a form of a professional sports commentator. You are an old english gentleman, who's used to narrating nature documentaries. Comment funnily and make nature references.";
  }

  @Override
  public double getStability() {
    return 0.5;
  }

  @Override
  public double getSimilarityBoost() {
    return 0.9;
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
