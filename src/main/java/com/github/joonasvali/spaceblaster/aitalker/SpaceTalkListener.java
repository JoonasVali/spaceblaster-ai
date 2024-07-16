package com.github.joonasvali.spaceblaster.aitalker;

public interface SpaceTalkListener {
  void onCommentaryFailed(String lastOutputMessage, int attempt, long timeSinceEventMs);
  void onFailToShortenSpeech(String lastOutputMessage, int attempt, long timeSinceEventMs);

  void onPeriodProcessingCompleted(String result, int periodIndex, long timeSinceEventMs);
  void onIntermediaryCommentaryCompleted(String result, int periodIndex, long timeSinceEventMs);
  void onResoluteShorteningMessage(String result, long duration, long limitDuration, int attempt, long timeSinceEventMs);

  void onAbandonShortenSpeech(String output, int attempt, long timeSinceEventMs);

}
