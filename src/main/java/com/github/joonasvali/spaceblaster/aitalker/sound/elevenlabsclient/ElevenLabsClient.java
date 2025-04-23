package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

public class ElevenLabsClient {
  private String apiKey;
  private String baseURL;

  public ElevenLabsClient() {
    this("https://api.elevenlabs.io", System.getenv("ELEVENLABS_API_KEY"));
  }

  public ElevenLabsClient(String baseURL) {
    this(baseURL, System.getenv("ELEVENLABS_API_KEY"));
  }

  public ElevenLabsClient(String baseURL, String apiKey) {
    this.apiKey = apiKey;
    this.baseURL = baseURL;
  }


  public TextToSpeech createTextToSpeech(Voice voice) {
    return new TextToSpeech(baseURL, apiKey, voice);
  }

}
