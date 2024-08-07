package com.github.joonasvali.spaceblaster.aitalker.event;

public record CommentaryFailedEvent(
    int attempt,
    String lastOutputMessage,
    long inputLatency,
    long periodDuration,
    int periodIndex,
    long periodRelativeStartTime,
    long eventTime
) {}