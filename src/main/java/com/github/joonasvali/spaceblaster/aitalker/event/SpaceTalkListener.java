package com.github.joonasvali.spaceblaster.aitalker.event;

public interface SpaceTalkListener {
  void onCommentaryFailed(CommentaryFailedEvent event);

  void onPeriodProcessingStarted(PeriodProcessingStartedEvent event);
  void onPeriodProcessingCompleted(PeriodProcessingCompletedEvent event);

  void onResoluteShorteningMessage(String result, long duration, long limitDuration, int attempt, long timeSinceEventMs);

  void onAbandonShortenSpeech(String output, int attempt, long timeSinceEventMs);
}
