package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsLiamVoiceSettings extends SpaceBlasterVoiceSettings {

  public ElevenLabsLiamVoiceSettings(String name) {
    super(name, "TX3LPaxmHKxFdv7VOQHJ", 0.90, 0.55, 0, true);
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
}