package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;


public class ElevenLabsOxleyVoiceSettings extends SpaceBlasterVoiceSettings {
  private final String name;
  public ElevenLabsOxleyVoiceSettings(String name) {
    super(name, "3SF4rB1fGBMXU9xRM7pz", 0.58, 0.42, 0, true);
    this.name = name;
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsOxleySoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return "Your name is " + name + ". You are commenting as a total creep. You are evil. Make it seem like you are keeping the player as a prisoner, and you are whispering in their ear, but keep the topic mostly about the game. Here's an example of how you talk: \"You thought you were alone. You pressed the switch; the light flickered—and yet I was there, two seconds before the darkness fell. I admired the way your hand trembled as it brushed the cool metal. So human. So fragile. I catalog every micro-expression, every micro-gesture. Your memory may betray you; your body does not. And right now, it tells me you regret coming.\"";
  }

  @Override
  public String getModelId() {
    return "eleven_flash_v2_5";
  }
}