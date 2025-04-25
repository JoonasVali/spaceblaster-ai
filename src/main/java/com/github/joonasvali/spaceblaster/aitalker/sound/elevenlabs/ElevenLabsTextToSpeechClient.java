package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;
import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.ElevenLabsClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.TextToSpeech;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

public class ElevenLabsTextToSpeechClient implements TextToSpeechClient {

  private final SpaceBlasterVoiceSettings voiceSettings;

  private final ElevenLabsClient elevenLabsClient;
  private final TextToSpeech textToSpeech;


  public ElevenLabsTextToSpeechClient(SpaceBlasterVoiceSettings voiceSettings) {
    this.voiceSettings = voiceSettings;
    this.elevenLabsClient = new ElevenLabsClient();
    textToSpeech = elevenLabsClient.createTextToSpeech(voiceSettings);
  }


  @Override
  public String getCommentatorDescription() {
    return voiceSettings.getCommentatorDescription();
  }

  /**
   * @param text The text to generate audio from.
   * @return duration of the audio in milliseconds
   *
   */
  @Override
  public TextToSpeechResponse produce(String text, String[] previousRequestIds, Path outputFile) throws IOException {
    var response = textToSpeech.textToSpeech(text, previousRequestIds, getSpeedModifier());
    if (response.isSuccess()) {
      byte[] bytes = Base64.getDecoder().decode(response.getAudioBase64Wav());
      try (var outputStream = java.nio.file.Files.newOutputStream(outputFile)) {
        outputStream.write(bytes);
      }
      return new TextToSpeechResponse(response.getDurationMs(), response.getRequestId());
    } else {
      throw new IOException("Failed to produce audio: " + response.getErrorMessage());
    }
  }

  public float getSpeedModifier() {
    return 1.2f;
  }

  @Override
  public int getSampleRate() {
    return textToSpeech.getOutputFormat().getSampleRate();
  }

  @Override
  public SoundDurationEvaluator getSoundDurationEvaluator() {
    return new SoundDurationEvaluatorSpeedModifier(voiceSettings.getSoundDurationEvaluator(), getSpeedModifier());
  }

}