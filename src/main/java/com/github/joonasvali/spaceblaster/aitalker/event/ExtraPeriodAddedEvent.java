package com.github.joonasvali.spaceblaster.aitalker.event;

public record ExtraPeriodAddedEvent(
    long periodDuration,
    int periodIndex,
    long periodRelativeStartTime,
    long eventTime
) {}
