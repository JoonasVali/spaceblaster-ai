package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

/**
 * <a href="https://elevenlabs.io/docs/api-reference/text-to-speech/convert-with-timestamps">API doc</a>
 */
public class TextToSpeech {
  private final Voice voice;
  private final String apiKey;
  private OutputFormat outputFormat;
  private String baseURL;

  public TextToSpeech(String baseURL, String apiKey, Voice voice) {
    this.voice = voice;
    this.apiKey = apiKey;

    String strippedBaseURL = baseURL;
    if (strippedBaseURL.endsWith("/")) {
      strippedBaseURL = strippedBaseURL.substring(0, strippedBaseURL.length() - 1);
    }
    this.baseURL = strippedBaseURL;
  }

  public OutputFormat getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
  }

  public TextToSpeechResponse textToSpeech(String text, String[] previousIds) {
    return textToSpeech(text, previousIds, 1.0);
  }

  public TextToSpeechResponse textToSpeech(String text, String[] previousRequestIds, double speed) {
    if (speed < 0.7 || speed > 1.2) {
      throw new IllegalArgumentException("Speed must be between 0.7 and 1.2");
    }
    OkHttpClient client = new OkHttpClient();

    String json = composeJSONPayload(text, keepLastThree(previousRequestIds), speed);
    MediaType mediaType = MediaType.get("application/json; charset=utf-8");
    RequestBody body = RequestBody.create(json, mediaType);

    String queryParam = "";
    if (outputFormat != null) {
      queryParam = "?output_format=" + outputFormat.getValue();
    }

    Request request = new Request.Builder()
        .url(baseURL + "/v1/text-to-speech/"  + voice.getVoiceId() + "/with-timestamps" + queryParam)
        .post(body)
        .header("xi-api-key", apiKey)
        .build();

    String requestId = null;
    int characterCost = 0;
    IOException exception = null;
    TextToSpeechResponseRaw successfulResponseData = null;
    TextToSpeechError errorResponseData = null;
    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body().string();
      if (response.isSuccessful() && response.body() != null) {
        JSONObject obj = new JSONObject(responseBody);
        requestId = response.header("request-id");
        characterCost = Integer.parseInt(Objects.requireNonNull(response.header("character-cost")), 10);
        JSONObject alignment = obj.getJSONObject("alignment");
        String audioBase64 = obj.getString("audio_base64");
        JSONArray characterArray = alignment.getJSONArray("characters");
        char[] characters = new char[characterArray.length()];
        for (int i = 0; i < characters.length; i++) {
          characters[i] = characterArray.getString(i).charAt(0);
        }
        float[] characterStartTimes = new float[alignment.getJSONArray("character_start_times_seconds").length()];
        float[] characterEndTimes = new float[alignment.getJSONArray("character_end_times_seconds").length()];
        for (int i = 0; i < characterStartTimes.length; i++) {
          characterStartTimes[i] = alignment.getJSONArray("character_start_times_seconds").getFloat(i);
        }
        for (int i = 0; i < characterEndTimes.length; i++) {
          characterEndTimes[i] = alignment.getJSONArray("character_end_times_seconds").getFloat(i);
        }
        successfulResponseData = new TextToSpeechResponseRaw();
        successfulResponseData.audio_base64 = audioBase64;
        successfulResponseData.alignment = new Alignment();
        successfulResponseData.alignment.characters = characters;
        successfulResponseData.alignment.character_start_times_seconds = characterStartTimes;
        successfulResponseData.alignment.character_end_times_seconds = characterEndTimes;
      } else {
        JSONObject obj = new JSONObject(responseBody);
        JSONObject detail = obj.getJSONObject("detail");
        errorResponseData = new TextToSpeechError();
        errorResponseData.detail = new TextToSpeechErrorDetail();
        errorResponseData.detail.status = detail.getString("status");
        errorResponseData.detail.message = detail.getString("message");
      }
    } catch (IOException e) {
      exception = e;
      System.out.println("Request failed: " + e.getMessage());
    }

    TextToSpeechResponse response = new TextToSpeechResponse();
    if (successfulResponseData == null) {
      response.setSuccess(false);
      response.setErrorMessage(errorResponseData != null ? errorResponseData.detail.message : (exception.getMessage() != null ? exception.getMessage() : "Unknown error"));
      response.setErrorStatus(errorResponseData != null ? errorResponseData.detail.status : "Unknown status");

      return response;
    }

    response.setAudioBase64(successfulResponseData.audio_base64);
    response.setRequestId(requestId);
    response.setCharacterCost(characterCost);
    response.setCharacters(successfulResponseData.alignment.characters);
    response.setCharacterStartTimes(successfulResponseData.alignment.character_start_times_seconds);
    response.setCharacterEndTimes(successfulResponseData.alignment.character_end_times_seconds);
    response.setSuccess(true);
    return response;
  }

  private String[] keepLastThree(String[] previousRequestIds) {
    if (previousRequestIds == null) {
      return null;
    }
    int length = previousRequestIds.length;
    if (length > 3) {
      String[] newArray = new String[3];
      System.arraycopy(previousRequestIds, length - 3, newArray, 0, 3);
      return newArray;
    }
    return previousRequestIds;
  }

  private String composeJSONPayload(String text, String[] previousRequestIds, double speed) {
    JSONObject object = new JSONObject();
    JSONObject voiceSettings = new JSONObject();
    voiceSettings.put("stability", voice.getStability());
    voiceSettings.put("similarity_boost", voice.getSimiliarityBoost());
    voiceSettings.put("style", voice.getStyle());
    voiceSettings.put("use_speaker_boost", voice.isUseSpeakerBoost());
    voiceSettings.put("speed", speed);
    object.put("text", text);
    if (previousRequestIds != null && previousRequestIds.length > 0) {
      object.put("previous_request_ids", previousRequestIds);
    }
    object.put("voice_settings", voiceSettings);
    object.put("model_id", "eleven_multilingual_v2");
    return object.toString();
  }

  private static class TextToSpeechResponseRaw {
    String audio_base64;
    Alignment alignment;
  }

  private static class Alignment {
    char[] characters;
    float[] character_start_times_seconds;
    float[] character_end_times_seconds;
  }


  private static class TextToSpeechError {
    private TextToSpeechErrorDetail detail;
  }

  private static class TextToSpeechErrorDetail {
    private String status;
    private String message;
  }
}
