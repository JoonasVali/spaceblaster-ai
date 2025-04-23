package com.github.joonasvali.spaceblaster.aitalker.sound;

import java.io.IOException;
import java.nio.file.Path;

public interface TextToSpeechClient {
  String getCommentatorDescription();

  TextToSpeechResponse produce(String text, String[] previousRequestIds, Path outputFile) throws IOException;

  int getSampleRate();

  SoundDurationEvaluator getSoundDurationEvaluator();

  record TextToSpeechResponse(long durationMs, String requestId) {
  }
}