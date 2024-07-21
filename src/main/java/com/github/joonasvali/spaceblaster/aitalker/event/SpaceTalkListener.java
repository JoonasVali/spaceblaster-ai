package com.github.joonasvali.spaceblaster.aitalker.event;

public interface SpaceTalkListener {
  void onCommentaryFailed(String lastOutputMessage, int attempt, long timeSinceEventMs);
  void onFailToShortenSpeech(String lastOutputMessage, int attempt, long timeSinceEventMs);

  void onPeriodProcessingCompleted(PeriodProcessingCompletedEvent event);

  void onResoluteShorteningMessage(String result, long duration, long limitDuration, int attempt, long timeSinceEventMs);

  void onAbandonShortenSpeech(String output, int attempt, long timeSinceEventMs);
}
