package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
// This voice is a bit unstable. It has a lot of randomness and a lot of emotion.
public class ElevenLabsMimiVoiceSettings extends SpaceBlasterVoiceSettings {
  public String getVoiceId() {
    return "zrHiDhphv9ZnVXBqCLjz";
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsMimiSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return "You are commenting in a form of a professional sports commentator. You are fabulous british lady commenter in her 50s. You commentate with a proper british style and make british puns.";
  }

  @Override
  public double getStability() {
    return 0.7;
  }

  @Override
  public double getSimilarityBoost() {
    return 0.75;
  }

  @Override
  public double getStyle() {
    return 0.5;
  }

  @Override
  public boolean isUseSpeakerBoost() {
    return true;
  }
}
