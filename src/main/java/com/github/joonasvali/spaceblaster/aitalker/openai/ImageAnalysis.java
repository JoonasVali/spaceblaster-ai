package com.github.joonasvali.spaceblaster.aitalker.openai;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.github.joonasvali.spaceblaster.aitalker.llm.OpenAIClient.OPENAI_TOKEN_KEY;

/*
 * The official OpenAI API client does not allow sending images, so this is a temporary workaround
 * to get image recognition.
 *
 * https://github.com/openai/openai-java/issues/191
 *
 * This class is responsible for sending an image to OpenAI's API and receiving a text response.
 *
 */
public class ImageAnalysis {
  private static final Logger logger = LoggerFactory.getLogger(ImageAnalysis.class);
  public static final String COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

  private final String prompt;
  public ImageAnalysis(String prompt) {
    this.prompt = prompt;
  }

  private static String convertBufferedImageToBase64(BufferedImage image, String format) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      boolean written = ImageIO.write(image, format, outputStream);
      if (!written) {
        throw new IOException("No appropriate writer found for format: " + format);
      }
      outputStream.flush();
      byte[] imageBytes = outputStream.toByteArray();
      return Base64.getEncoder().encodeToString(imageBytes);
    }
  }



  public ProcessingResult<String> process(BufferedImage bufferedImage) throws IOException {

    ImageResizer imageResizer = ImageResizer.getStandardOpenAIImageResizer();
    BufferedImage resizedImage = imageResizer.resizeImageToLimits(bufferedImage);

    String base64Image = convertBufferedImageToBase64(resizedImage, "png");

    if (logger.isDebugEnabled()) {
      Path tempPath = System.getProperty("java.io.tmpdir") != null ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(".");
      Path file = tempPath.resolve("image-" + base64Image.hashCode()  + ".png");
      logger.debug("Writing image to " + file);
      ImageIO.write(resizedImage, "png", file.toFile());
    }

    JSONObject jsonBody = createJsonPayload(base64Image, 1);
    String result =  sendRequestToOpenAI(jsonBody);

    if (result.startsWith("Error")) {
      throw new RuntimeException(result);
    }

    JSONObject jsonObject = new JSONObject(result);
    JSONArray choices = jsonObject.getJSONArray("choices");
    JSONObject choice = choices.getJSONObject(0);
    JSONObject message = choice.getJSONObject("message");
    JSONObject usage = jsonObject.getJSONObject("usage");
    int totalTokens = usage.getInt("total_tokens");
    int promptTokens = usage.getInt("prompt_tokens");
    int completionTokens = usage.getInt("completion_tokens");
    return new ProcessingResult<>(message.getString("content"), totalTokens, promptTokens, completionTokens);
  }

  public JSONObject createJsonPayload(String base64Image, int n) {
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("model", "chatgpt-4o-latest");
    jsonBody.put("max_tokens", 10000);
    jsonBody.put("n", n);

    JSONArray messages = new JSONArray();

    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");

    JSONArray contentArray = new JSONArray();
    contentArray.put(new JSONObject().put("type", "text").put("text", prompt));

    JSONObject imageUrlObject = new JSONObject();
    contentArray.put(new JSONObject().put("type", "image_url").put("image_url", imageUrlObject));
    imageUrlObject.put("url", "data:image/png;base64," + base64Image);
    imageUrlObject.put("detail", "high");

    userMessage.put("content", contentArray);
    messages.put(userMessage);

    jsonBody.put("messages", messages);
    return jsonBody;
  }

  public static String sendRequestToOpenAI(JSONObject jsonBody) throws IOException {
    return sendRequestToOpenAI(jsonBody, 0);
  }

  public static String sendRequestToOpenAI(JSONObject jsonBody, int retry) throws IOException {
    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build();

    RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

    Request request = new Request.Builder()
        .url(COMPLETIONS_URL)
        .header("Authorization", "Bearer " + System.getenv(OPENAI_TOKEN_KEY))
        .header("Content-Type", "application/json")
        .post(requestBody)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        return response.body().string();
      } else {
        if (response.code() == 429) {
          if (retry < 5) {
            Thread.sleep((retry + 1) * 3000L);
            return sendRequestToOpenAI(jsonBody, retry + 1);
          }
        }
        return "Error: " + response.code() + " - " + response.message();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return "Error: Unable to complete request";
  }
}
