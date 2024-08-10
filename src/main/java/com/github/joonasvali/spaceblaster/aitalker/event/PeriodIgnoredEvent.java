package com.github.joonasvali.spaceblaster.aitalker.event;

public record PeriodIgnoredEvent(
    int periodIndex,
    long periodRelativeStartTime,
    long periodDuration,
    long latency,
    long eventTime
) {
}
