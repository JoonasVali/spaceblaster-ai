package com.github.joonasvali.spaceblaster.aitalker.elevenlabsclient;

import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.ElevenLabsClient;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.TextToSpeech;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.TextToSpeechResponse;
import com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient.voices.Callum;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TextToSpeechTest {

  private MockWebServer mockWebServer;
  private ElevenLabsClient client;

  @BeforeEach
  public void setup() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = mockWebServer.url("/v1/text-to-speech/N2lVS1w4EtoT3dr4eOWO/with-timestamps").toString();
    client = new ElevenLabsClient(baseUrl);
  }

  @AfterEach
  public void teardown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void testTextToSpeechSuccess() throws IOException, LineUnavailableException, InterruptedException {
    Path jsonFile = Path.of("src", "test", "resources", "elevenlabsclient", "text_to_speech_response.1.json");
    String cannedResponse = Files.readString(jsonFile, StandardCharsets.UTF_8);

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setHeader("request-id", "xsGame123")
        .setHeader("character-cost", "321")
        .setBody(cannedResponse)
    );

    String text = "Hello, this is a test.";
    Callum voice = new Callum("myVoice");
    TextToSpeech tts = client.createTextToSpeech("eleven_multilingual_v2", voice);

    TextToSpeechResponse response = tts.textToSpeech(text, new String[0]);
    assertNotNull(response, "Response should not be null");

    AudioInputStream audioInputStream = response.openAudioStream();
    assertNotNull(audioInputStream, "Audio stream should not be null");
    Clip clip = AudioSystem.getClip();
    clip.open(audioInputStream);

    Assertions.assertEquals(1_906_938, clip.getMicrosecondLength());
    Assertions.assertEquals(1906, response.getDurationMs());
    clip.close();

    assertTrue(response.isSuccess(), "Response should be successful");
    assertEquals("xsGame123", response.getRequestId(), "Request ID should match");
    assertEquals(321, response.getCharacterCost(), "Character cost should match");
    assertEquals(text.length(), response.getCharacterStartTimesSeconds().length, "Character start times should be text length");
    assertEquals(text.length(), response.getCharacterEndTimesSeconds().length, "Character end times should be text length");
    assertEquals(text.length(), response.getCharacters().length, "Characters should be text length");
  }


  @Test
  public void testTextToSpeechBadVoiceId() throws IOException {
    Path jsonFile = Path.of("src", "test", "resources", "elevenlabsclient", "text_to_speech_response.error.json");
    String cannedResponse = Files.readString(jsonFile, StandardCharsets.UTF_8);

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(400)
        .setHeader("Content-Type", "application/json")
        .setBody(cannedResponse)
    );

    String text = "Hello, this is a test.";
    Callum voice = new Callum("myVoice");
    TextToSpeech tts = client.createTextToSpeech("eleven_multilingual_v2", voice);

    TextToSpeechResponse response = tts.textToSpeech(text, new String[0]);

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccess());
    assertEquals("A voice with the voice_id XYZ was not found.", response.getErrorMessage());
    assertEquals("voice_not_found", response.getErrorStatus());
    assertNull(response.getRequestId());
    assertNull(response.getCharacters());
    assertEquals(0, response.getCharacterCost());
    assertNull(response.getCharacterStartTimesSeconds());
    assertNull(response.getCharacterEndTimesSeconds());
  }


  @Test
  public void testTextToSpeechInvalidKey() throws IOException {
    Path jsonFile = Path.of("src", "test", "resources", "elevenlabsclient", "text_to_speech_response.apikey_error.json");
    String cannedResponse = Files.readString(jsonFile, StandardCharsets.UTF_8);

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(400)
        .setHeader("Content-Type", "application/json")
        .setBody(cannedResponse)
    );

    String text = "Hello, this is a test.";
    Callum voice = new Callum("myVoice");
    TextToSpeech tts = client.createTextToSpeech("eleven_multilingual_v2", voice);

    TextToSpeechResponse response = tts.textToSpeech(text, new String[0]);

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccess());
    assertEquals("Invalid API key", response.getErrorMessage());
    assertEquals("invalid_api_key", response.getErrorStatus());
    assertNull(response.getRequestId());
    assertNull(response.getCharacters());
    assertEquals(0, response.getCharacterCost());
    assertNull(response.getCharacterStartTimesSeconds());
    assertNull(response.getCharacterEndTimesSeconds());
  }
}