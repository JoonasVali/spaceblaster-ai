package com.github.joonasvali.spaceblaster.aitalker.llm;

import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatCompletion;
import io.github.stefanbratanov.jvm.openai.ChatMessage;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatelessOpenAIClient extends BaseLLMClient {

  public static final String DEEP_SEEK_BASE = "https://api.deepseek.com";
  public static final String OPENAI_TOKEN_KEY = "OPENAI_TOKEN";
  private final Logger logger = LoggerFactory.getLogger(StatelessOpenAIClient.class);

  public static final String DEEP_SEEK_MODEL = "deepseek-chat";

  private final String apiKey;
  private final String model;
  private final String base;
  private int tokensUsed = 0;
  private int completionTokensUsed = 0;
  private int promptTokensUsed = 0;

  public StatelessOpenAIClient() {
    this(false);
  }

  public StatelessOpenAIClient(boolean deepSeek) {
    if (deepSeek) {
      this.apiKey = System.getenv("DEEPSEEK_TOKEN");
      this.model = DEEP_SEEK_MODEL;
      this.base = DEEP_SEEK_BASE;
    } else {
      this.apiKey = System.getenv(OPENAI_TOKEN_KEY);
      this.model = "o4-mini";
      this.base = null;
    }
  }

  public Response run(Text instruction) {
    var builder = OpenAI.newBuilder(apiKey);
    if (base != null) {
      builder.baseUrl(base);
    }
    OpenAI openAI = builder.build();

    ChatClient chatClient = openAI.chatClient();
    ChatMessage inputMessage = ChatMessage.userMessage(instruction.toString());
    logger.debug("Running OpenAI with instruction: " + instruction);

    CreateChatCompletionRequest createChatCompletionRequest = CreateChatCompletionRequest.newBuilder()
        .model(model)
        .message(inputMessage)
        .n(1)
        .build();

    ChatCompletion chatCompletion = chatClient.createChatCompletion(createChatCompletionRequest);


    this.tokensUsed = this.tokensUsed + chatCompletion.usage().totalTokens();
    this.completionTokensUsed = this.completionTokensUsed + chatCompletion.usage().completionTokens();
    this.promptTokensUsed = this.promptTokensUsed + chatCompletion.usage().promptTokens();

    if (!chatCompletion.choices().isEmpty()) {
      logger.debug("OpenAI response: " + chatCompletion.choices().getFirst().message().content());
      return new Response(instruction, chatCompletion.choices().getFirst().message().content());
    }

    return null;
  }

  @Override
  public SpaceTalkListener getSpaceTalkListener() {
    return null;
  }

  public int getTokensUsed() {
    return tokensUsed;
  }

  public int getCompletionTokensUsed() {
    return completionTokensUsed;
  }

  public int getPromptTokensUsed() {
    return promptTokensUsed;
  }

}
