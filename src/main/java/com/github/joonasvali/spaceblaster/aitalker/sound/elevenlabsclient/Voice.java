package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

public class Voice {
  private final String name;
  private final String voiceId;
  private final double similiarityBoost;
  private final double stability;
  private final boolean useSpeakerBoost;
  private final double style;

  public Voice(String name, String voiceId, double similiarityBoost, double stability, double style, boolean useSpeakerBoost) {
    this.name = name;
    this.voiceId = voiceId;
    this.similiarityBoost = similiarityBoost;
    this.stability = stability;
    this.useSpeakerBoost = useSpeakerBoost;
    this.style = style;
  }

  public String getName() {
    return name;
  }

  public String getVoiceId() {
    return voiceId;
  }

  public double getSimiliarityBoost() {
    return similiarityBoost;
  }

  public double getStability() {
    return stability;
  }

  public boolean isUseSpeakerBoost() {
    return useSpeakerBoost;
  }

  public double getStyle() {
    return style;
  }

  @Override
  public String toString() {
    return "Voice{" +
        "name='" + name + '\'' +
        ", voiceId='" + voiceId + '\'' +
        ", similiarityBoost=" + similiarityBoost +
        ", stability=" + stability +
        ", useSpeakerBoost=" + useSpeakerBoost +
        ", style=" + style +
        '}';
  }
}
