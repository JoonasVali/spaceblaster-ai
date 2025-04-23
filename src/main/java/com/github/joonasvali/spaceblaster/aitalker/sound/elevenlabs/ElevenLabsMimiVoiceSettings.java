package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsMimiVoiceSettings extends SpaceBlasterVoiceSettings {
  public ElevenLabsMimiVoiceSettings(String name) {
    super(name, "zrHiDhphv9ZnVXBqCLjz", 0.75, 0.7, 0.5, true);
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsMimiSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return "You are commenting in a form of a professional sports commentator. You are fabulous british lady commenter in her 50s. You commentate with a proper british style and make british puns.";
  }
}