package com.github.joonasvali.spaceblaster.aitalker.event;

public record ResoluteShorteningMessageEvent (
    int periodIndex,
    String result,
    long duration,
    long limitDuration,
    int attempt,
    long eventTime
) {
}
