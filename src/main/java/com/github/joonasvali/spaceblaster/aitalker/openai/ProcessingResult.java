package com.github.joonasvali.spaceblaster.aitalker.openai;

public record ProcessingResult<T>(T content, long promptTokens, long completionTokens, long totalTokens) {
}
