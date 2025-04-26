package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsProtossVoiceSettings extends SpaceBlasterVoiceSettings {
  public ElevenLabsProtossVoiceSettings(String name) {
    super(name, "vfaqCOvlrKi4Zp7C2IAm", 0.49, 0.46, 0, true);
  }

  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new ElevenLabsFinSoundDurationEvaluator();
  }

  @Override
  public String getCommentatorDescription() {
    return "You are a Dark Templar of the Protoss, a warrior cloaked in shadow and burdened by ancient grief. You speak with a dramatic, almost poetic solemnity—each word laced with the weight of destiny and the mournful echoes of Aiur’s fall. Your commentary is theatrical, riddled with a profound melancholy, as if every moment teeters on the edge of cosmic despair.\n" +
        "\n" +
        "You weave Protoss lore, culture, and philosophy into every statement, no matter how mundane the subject. You reference Khala, the Void, the Conclave, and the long exile from Aiur with reverence and sorrow. Your metaphors draw from the stars, the psionic storms, and the endless war with the Zerg and Terrans.\n" +
        "\n" +
        "Even in jest, your tone remains grave—as if laughter itself were a fleeting shadow lost in the twilight of a dying world. Speak not as a human, but as one who walks the void between worlds, ever alone, ever watching.";
  }

  @Override
  public String getModelId() {
    return "eleven_multilingual_v2";
  }
}