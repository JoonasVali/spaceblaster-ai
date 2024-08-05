package com.github.joonasvali.spaceblaster.aitalker.llm;

import com.github.joonasvali.spaceblaster.aitalker.event.SpaceTalkListener;

public interface LLMClient {
  Response run(Text instruction);

  void commitHistory();

  void commitHistoryBySquashing();

  void clearUncommittedHistory();

  void setSystemMessage(String systemMessage);

  SpaceTalkListener getSpaceTalkListener();

}
