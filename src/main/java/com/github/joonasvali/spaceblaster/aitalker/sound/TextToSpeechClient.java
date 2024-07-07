package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.SpaceTalkListener;

import java.io.IOException;
import java.nio.file.Path;

public interface TextToSpeechClient {
  String getCommentatorDescription();
  TextToSpeechOutput getOutputSettings();

  long produce(String text, Path outputFile) throws IOException;

  SoundDurationEvaluator getSoundDurationEvaluator();

  SpaceTalkListener getSpaceTalkListener();
}
