package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;


import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.Voice;

public abstract class SpaceBlasterVoiceSettings extends Voice {
  public SpaceBlasterVoiceSettings(String name, String voiceId, double similiarityBoost, double stability, double style, boolean useSpeakerBoost) {
    super(name, voiceId, similiarityBoost, stability, style, useSpeakerBoost);
  }

  public abstract String getCommentatorDescription();
}
