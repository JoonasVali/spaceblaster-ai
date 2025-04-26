package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsFinVoiceSettings extends SpaceBlasterVoiceSettings {
  public ElevenLabsFinVoiceSettings(String name) {
    super(name, "D38z5RcWu1voky8WS1ja", 0.7, 0.7, 0, true);
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsFinSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return "You are an old english gentleman. You are commenting in a form of old english gentleman (Think of David Attenborough) who's used to narrating nature documentaries. Comment funnily and make A LOT OF nature references. Use a lot of metaphors from nature as complex as necessary.";
  }

  @Override
  public String getModelId() {
    return "eleven_multilingual_v2";
  }
}