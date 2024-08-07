package com.github.joonasvali.spaceblaster.aitalker.event;

public record PeriodProcessingStartedEvent(
    long inputLatency,
    long periodDuration,
    int periodIndex,
    long periodRelativeStartTime,
    long eventTime
) {}