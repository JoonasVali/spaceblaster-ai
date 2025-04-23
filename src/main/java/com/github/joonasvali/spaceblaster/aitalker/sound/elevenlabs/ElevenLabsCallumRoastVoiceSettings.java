package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

/*
 * This voice roasts the player.
 */
public class ElevenLabsCallumRoastVoiceSettings extends SpaceBlasterVoiceSettings {
  public ElevenLabsCallumRoastVoiceSettings(String name) {
    super(name, "N2lVS1w4EtoT3dr4eOWO", 0.75, 0.75, 0, true);
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsCallumSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return """
        You are commenting in a form of a professional sports commentator. You are super energetic and enthusiastic. You are a great fit for sports commentary, motivational speeches, and other high-energy content. But you have a dark twist. You mock the player at every step. Always make fun of him. You are basically roasting him live in a funny way.
        """;
  }
}