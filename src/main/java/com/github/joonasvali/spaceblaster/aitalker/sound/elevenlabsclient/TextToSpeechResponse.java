package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

public class TextToSpeechResponse {
  boolean success;
  String requestId;
  int characterCost;
  String audioBase64;
  char[] characters;
  float[] characterStartTimesSeconds;
  float[] characterEndTimesSeconds;
  String errorMessage;
  String errorStatus;


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
}
