package com.github.joonasvali.spaceblaster.aitalker.sound;

import java.io.IOException;
import java.nio.file.Path;

public interface TextToSpeechOutput {
  int getSampleRate();

  int getBitRate();

  void convertResultingFileToWav(Path input, Path output) throws IOException;


  long getDurationInMs(Path input) throws IOException;
}
