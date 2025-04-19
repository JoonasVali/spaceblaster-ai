package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;
import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechOutput;
import net.andrewcpu.elevenlabs.ElevenLabs;
import net.andrewcpu.elevenlabs.builders.SpeechGenerationBuilder;
import net.andrewcpu.elevenlabs.builders.impl.tts.TextToSpeechFileBuilder;
import net.andrewcpu.elevenlabs.enums.ElevenLabsVoiceModel;
import net.andrewcpu.elevenlabs.enums.GeneratedAudioOutputFormat;
import net.andrewcpu.elevenlabs.enums.StreamLatencyOptimization;
import net.andrewcpu.elevenlabs.model.voice.VoiceSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ElevenLabsClient implements TextToSpeechClient {

  private final String elevenLabsApiKey;
  private final SpaceBlasterVoiceSettings voiceSettings;

  private final TextToSpeechOutput textToSpeechOutput;

  private final GeneratedAudioOutputFormat generatedAudioOutputFormat;

  public ElevenLabsClient(SpaceBlasterVoiceSettings voiceSettings, TextToSpeechOutput textToSpeechOutput) {
    this.textToSpeechOutput = textToSpeechOutput;
    elevenLabsApiKey = System.getenv("ELEVENLABS_API_KEY");
    this.voiceSettings = voiceSettings;
    generatedAudioOutputFormat = lookUpAudioFormat(textToSpeechOutput.getSampleRate(), textToSpeechOutput.getBitRate());
    ElevenLabs.setApiKey(elevenLabsApiKey);
  }

  private static GeneratedAudioOutputFormat lookUpAudioFormat(int sampleRate, int bitRate) {
    return Arrays.stream(GeneratedAudioOutputFormat.values()).filter(
        format -> format.toString().equals("MP3_" + sampleRate + "_" + bitRate)
    ).findFirst().orElseThrow(
        () -> new IllegalArgumentException("No audio format found for sample rate " + sampleRate + " and bit rate " + bitRate)
    );
  }

  @Override
  public String getCommentatorDescription() {
    return voiceSettings.getCommentatorDescription();
  }

  @Override
  public TextToSpeechOutput getOutputSettings() {
    return textToSpeechOutput;
  }

  /**
   * @param text The text to generate audio from.
   * @return duration of the audio in milliseconds
   *
   */
  @Override
  public TextToSpeechResponse produce(String text, String[] previousRequestIds, Path outputFile) throws IOException {
    String voiceId = voiceSettings.getVoiceId();

    String requestId;

    // Keep the text clean from asterisks, as elevenlabs pronounces them out.
    text = text.replaceAll("\\*", "");
    TextToSpeechFileBuilder builder = SpeechGenerationBuilder.textToSpeech()
        .file()
        .setText(text)
        .setGeneratedAudioOutputFormat(generatedAudioOutputFormat)
        .setVoiceId(voiceId)
        .setVoiceSettings(new VoiceSettings(voiceSettings.getStability(), voiceSettings.getSimilarityBoost(), voiceSettings.getStyle(), voiceSettings.isUseSpeakerBoost()))
        .setModel(ElevenLabsVoiceModel.ELEVEN_MONOLINGUAL_V1)
        .setLatencyOptimization(StreamLatencyOptimization.NORMAL);

    if (previousRequestIds.length > 0) {
      builder.setPreviousRequestIds(previousRequestIds);
    }

    Path path = Paths.get(builder.build().getCanonicalPath());
    requestId = builder.getReceivedLastRequestId();

    textToSpeechOutput.convertResultingFileToWav(path, outputFile);
    return new TextToSpeechResponse(textToSpeechOutput.getDurationInMs(path), requestId);
  }

  @Override
  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return voiceSettings.getSoundDurationEvaluator();
  }

  @Override
  public SpaceTalkListener getSpaceTalkListener() {
    return null;
  }
}
