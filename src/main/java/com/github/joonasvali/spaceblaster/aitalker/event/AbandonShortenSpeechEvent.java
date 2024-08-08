package com.github.joonasvali.spaceblaster.aitalker.event;

public record AbandonShortenSpeechEvent(
    int periodIndex,
    String output,
    int attempt,
    long eventTime
) {
}
