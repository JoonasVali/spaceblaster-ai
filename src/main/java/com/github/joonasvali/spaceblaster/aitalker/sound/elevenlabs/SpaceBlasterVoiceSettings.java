package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import net.andrewcpu.elevenlabs.model.voice.VoiceSettings;

/*
 * https://elevenlabs.io/docs/speech-synthesis/voice-settings
 *
 * The stability slider determines how stable the voice is and the randomness between each generation. Lowering this
 * slider introduces a broader emotional range for the voice. As mentioned before, this is also influenced heavily by
 * the original voice. Setting the slider too low may result in odd performances that are overly random and cause the
 * character to speak too quickly. On the other hand, setting it too high can lead to a monotonous voice with limited
 * emotion.
 *
 * The similarity slider dictates how closely the AI should adhere to the original voice when attempting to replicate
 * it. If the original audio is of poor quality and the similarity slider is set too high, the AI may reproduce
 * artifacts or background noise when trying to mimic the voice if those were present in the original recording.
 *
 * With the introduction of the newer models, we also added a style exaggeration setting. This setting attempts to
 * amplify the style of the original speaker. It does consume additional computational resources and might increase
 * latency if set to anything other than 0. It’s important to note that using this setting has shown to make the
 * model slightly less stable, as it strives to emphasize and imitate the style of the original voice.
 * In general, we recommend keeping this setting at 0 at all times.
 */
public abstract class SpaceBlasterVoiceSettings extends VoiceSettings {
  public abstract String getVoiceId();

  public abstract SoundDurationEvaluator getSoundDurationEvaluator();

  public abstract String getCommentatorDescription();
}
