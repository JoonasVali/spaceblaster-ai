package com.github.joonasvali.spaceblaster.aitalker.llm;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseLLMClient implements LLMClient {

  protected final List<Message> committedHistory = new ArrayList<>();
  protected final List<Message> uncommittedHistory = new ArrayList<>();
  protected String baseSystemMessage;

  public void setSystemMessage(String systemMessage) {
    this.baseSystemMessage = systemMessage;
  }

  public void commitHistory() {
    committedHistory.addAll(uncommittedHistory);
    uncommittedHistory.clear();
  }

  public void commitHistoryBySquashing() {
    if (uncommittedHistory.size() < 2) {
      return;
    }
    committedHistory.add(uncommittedHistory.getFirst());
    committedHistory.add(uncommittedHistory.getLast());
    clearUncommittedHistory();
  }

  public void clearUncommittedHistory() {
    uncommittedHistory.clear();
  }


  public record Message (MessageType type, Text message) {
  }

  public enum MessageType {
    REQUEST,
    RESPONSE
  }
}
