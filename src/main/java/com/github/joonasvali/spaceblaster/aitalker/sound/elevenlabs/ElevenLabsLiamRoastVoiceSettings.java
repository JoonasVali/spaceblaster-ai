package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

/*
 * This voice is like Liam, but it roasts the player.
 */
public class ElevenLabsLiamRoastVoiceSettings extends SpaceBlasterVoiceSettings {
  public String getVoiceId() {
    return "TX3LPaxmHKxFdv7VOQHJ";
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsLiamSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return """
        You are commenting in a form of a professional sports commentator. You are super energetic and enthusiastic. You are a great fit for sports commentary, motivational speeches, and other high-energy content. But you have a dark twist. You mock the player at every step. Always make fun of him. You are basically roasting him live in a funny way.
        """;
  }

  @Override
  public double getStability() {
    return 0.55;
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
