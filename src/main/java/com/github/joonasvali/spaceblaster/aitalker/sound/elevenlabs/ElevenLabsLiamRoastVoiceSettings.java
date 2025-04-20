package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

/*
 * This voice is like Liam, but it roasts the player.
 */
public class ElevenLabsLiamRoastVoiceSettings extends SpaceBlasterVoiceSettings {
  public ElevenLabsLiamRoastVoiceSettings(String name) {
    super(name, "TX3LPaxmHKxFdv7VOQHJ", 0.90, 0.55, 0, true);
  }

  @Override
  public String getCommentatorDescription() {
    return """
        You are commenting in a form of a professional sports commentator. You are super energetic and enthusiastic. You are a great fit for sports commentary, motivational speeches, and other high-energy content. But you have a dark twist. You mock the player at every step. Always make fun of him. You are basically roasting him live in a funny way.
        """;
  }
}
