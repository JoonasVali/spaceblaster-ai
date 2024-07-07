package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import net.andrewcpu.elevenlabs.model.voice.VoiceSettings;

public abstract class SpaceBlasterVoiceSettings extends VoiceSettings {
  public abstract String getVoiceId();

  public abstract SoundDurationEvaluator getSoundDurationEvaluator();

  public abstract String getCommentatorDescription();
}
