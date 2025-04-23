package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class TextToSpeechResponse {
  boolean success;
  String requestId;
  int characterCost;
  String audioBase64;
  String audioBase64Wav;
  char[] characters;
  float[] characterStartTimesSeconds;
  float[] characterEndTimesSeconds;
  String errorMessage;
  String errorStatus;

  public String getAudioBase64Wav() {
    return audioBase64Wav;
  }

  public void setAudioBase64(String audioBase64) {
    this.audioBase64 = audioBase64;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public void setCharacterCost(int characterCost) {
    this.characterCost = characterCost;
  }

  public void setCharacters(char[] characters) {
    this.characters = characters;
  }

  public void setCharacterStartTimes(float[] characterStartTimesSeconds) {
    this.characterStartTimesSeconds = characterStartTimesSeconds;
  }

  public void setCharacterEndTimes(float[] characterEndTimesSeconds) {
    this.characterEndTimesSeconds = characterEndTimesSeconds;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void setErrorMessage(String s) {
    this.errorMessage = s;
  }

  public void setErrorStatus(String s) {
    this.errorStatus = s;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getRequestId() {
    return requestId;
  }

  public int getCharacterCost() {
    return characterCost;
  }

  public String getAudioBase64() {
    return audioBase64;
  }

  public char[] getCharacters() {
    return characters;
  }

  public float[] getCharacterStartTimesSeconds() {
    return characterStartTimesSeconds;
  }

  public float[] getCharacterEndTimesSeconds() {
    return characterEndTimesSeconds;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorStatus() {
    return errorStatus;
  }


  public void setAudioBase64Wav(String converted) {
    this.audioBase64Wav = converted;
  }

  public AudioInputStream openAudioStream() {
    try {
      byte[] audioBytesWav = Base64.getDecoder().decode(audioBase64Wav);
      ByteArrayInputStream baisWav = new ByteArrayInputStream(audioBytesWav);
      return AudioSystem.getAudioInputStream(baisWav);
    } catch (IOException e) {
      // Should not happen as there's no IO.
      throw new RuntimeException(e);
    } catch (UnsupportedAudioFileException e) {
      // Should not happen as the input is wav.
      throw new RuntimeException(e);
    }
  }

  public long getDurationMs() {
    try (AudioInputStream ais = openAudioStream()) {
      AudioFormat fmt = ais.getFormat();
      long frames = ais.getFrameLength();        // total frames in the stream
      float frameRate = fmt.getFrameRate();          // frames per second

      double durationSec = frames / frameRate;
      return (long) (durationSec * 1_000);            // ms
    } catch (Exception e) {
      throw new RuntimeException("Cannot compute duration", e);
    }
  }

}
