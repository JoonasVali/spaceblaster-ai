package com.github.joonasvali.spaceblaster.aitalker.event;

public record PeriodProcessingCompletedEvent(
    String result,
    String inputText,
    long generatedAudioDurationMs,
    long periodRelativeStartTime,
    long generatedAudioRelativeStartTime,
    long periodDuration,
    int periodIndex,
    long inputLatency,
    int retryAttempts,
    boolean shorteningAbandoned,
    long eventTime
) {}