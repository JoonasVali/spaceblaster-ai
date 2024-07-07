package com.github.joonasvali.spaceblaster.aitalker.llm;

import com.github.joonasvali.spaceblaster.aitalker.SpaceTalkListener;
import com.github.joonasvali.spaceblaster.aitalker.Util;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatCompletion;
import io.github.stefanbratanov.jvm.openai.ChatMessage;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.OpenAIException;
import io.github.stefanbratanov.jvm.openai.OpenAIModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpenAIClient extends BaseLLMClient {

  public static final OpenAIModel OPEN_AI_MODEL = OpenAIModel.GPT_4o;
  public static final long SLEEP_ON_EXCEPTION_MS = 30000L;
  public static final long SLEEP_ON_PERIOD_PROCESSED = 10000L;
  public static final long SLEEP_ON_FAILURE_TO_SHORTEN_SPEECH = 5000L;

  private final String openAIKey;
  private int tokensUsed = 0;
  private int completionTokensUsed = 0;
  private int promptTokensUsed = 0;


  public OpenAIClient() {
    this.openAIKey = System.getenv("OPENAI_TOKEN");
  }


  @Override
  public SpaceTalkListener getSpaceTalkListener() {
    return new SpaceTalkListener() {
      @Override
      public void onCommentaryFailed(String lastOutputMessage, int attempt, long timeSinceEventMs) {

      }

      @Override
      public void onFailToShortenSpeech(String lastOutputMessage, int attempt, long timeSinceEventMs) {
        Util.sleep(SLEEP_ON_FAILURE_TO_SHORTEN_SPEECH - timeSinceEventMs);
      }

      @Override
      public void onPeriodProcessingCompleted(String result, int periodIndex, long timeSinceEventMs) {
        committedHistory.forEach(message -> message.message().forgetShortTerm());
        Util.sleep(SLEEP_ON_PERIOD_PROCESSED - timeSinceEventMs);
      }

      @Override
      public void onResoluteShorteningMessage(String result, long duration, long limitDuration, int attempt, long timeSinceEventMs) {
        // Nothing
      }

      @Override
      public void onAbandonShortenSpeech(String output, int attempt, long timeSinceEventMs) {
        // Nothing
      }
    };
  }

  public Response run(Text instruction) {
    OpenAI openAI = OpenAI.newBuilder(openAIKey).build();

    ArrayDeque<ChatMessage> previousConversationWithSystemMessage = new ArrayDeque<>();
    previousConversationWithSystemMessage.add(ChatMessage.systemMessage(baseSystemMessage));
    includeRecentConversation(previousConversationWithSystemMessage);

    ChatClient chatClient = openAI.chatClient();
    ChatMessage inputMessage = ChatMessage.userMessage(instruction.toString());
    CreateChatCompletionRequest createChatCompletionRequest = CreateChatCompletionRequest.newBuilder()
        .model(OPEN_AI_MODEL)
        .messages(new ArrayList<>(previousConversationWithSystemMessage))
        .message(inputMessage)
        .temperature(1.1f)
        .n(1)
        .build();

    ChatCompletion chatCompletion;
    try {
      chatCompletion = chatClient.createChatCompletion(createChatCompletionRequest);
    } catch (OpenAIException e) {
      Util.sleep(SLEEP_ON_EXCEPTION_MS);
      chatCompletion = chatClient.createChatCompletion(createChatCompletionRequest);
    }

    this.tokensUsed = chatCompletion.usage().totalTokens();
    this.completionTokensUsed = chatCompletion.usage().completionTokens();
    this.promptTokensUsed = chatCompletion.usage().promptTokens();
    if (!chatCompletion.choices().isEmpty()) {
      uncommittedHistory.add(new Message(MessageType.REQUEST, instruction));
      uncommittedHistory.add(new Message(MessageType.RESPONSE, new Text(chatCompletion.choices().getFirst().message().content(), "")));
      return new Response(instruction, chatCompletion.choices().getFirst().message().content());
    }
    return null;
  }

  private void includeRecentConversation(ArrayDeque<ChatMessage> conversationThreadToBuild) {
    ArrayDeque<ChatMessage> messagesToIncludeReversed = new ArrayDeque<>();
    uncommittedHistory.forEach(message -> {
      messagesToIncludeReversed.addFirst(mapMessageToChatMessage(message));
    });

    for (int i = 0; i < 6 && i < committedHistory.size(); i++) {
      Message message = committedHistory.get(committedHistory.size() - 1 - i);
      messagesToIncludeReversed.add(mapMessageToChatMessage(message));
    }

    conversationThreadToBuild.addAll(messagesToIncludeReversed.reversed());
  }

  private Collection<ChatMessage> mapMessageToChatMessage(List<Message> messages) {
    return messages.stream().map(this::mapMessageToChatMessage).toList();
  }

  private ChatMessage mapMessageToChatMessage(Message message) {
    if (message.type() == MessageType.REQUEST) {
      return ChatMessage.userMessage(message.message().toString());
    } else if (message.type() == MessageType.RESPONSE) {
      return ChatMessage.assistantMessage(message.message().toString());
    }
    throw new UnsupportedOperationException();
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
