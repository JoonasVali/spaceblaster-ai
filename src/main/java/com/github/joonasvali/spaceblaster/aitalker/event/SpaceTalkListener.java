package com.github.joonasvali.spaceblaster.aitalker.event;

public interface SpaceTalkListener {
  void onCommentaryFailed(CommentaryFailedEvent event);

  void onPeriodProcessingStarted(PeriodProcessingStartedEvent event);
  void onPeriodProcessingCompleted(PeriodProcessingCompletedEvent event);

  void onResoluteShorteningMessage(ResoluteShorteningMessageEvent event);

  void onAbandonShortenSpeech(AbandonShortenSpeechEvent event);

  void onIgnorePeriod(PeriodIgnoredEvent event);
}
