package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.TextToSpeechOutput;
import com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion.ConvertMp3ToWav;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.JavaLayerException;

import java.io.IOException;
import java.nio.file.Path;

public class ElevenLabsMp3Output implements TextToSpeechOutput {
  @Override
  public int getSampleRate() {
    return 44100;
  }

  @Override
  public int getBitRate() {
    return 128;
  }

  @Override
  public void convertResultingFileToWav(Path input, Path output) throws IOException {
    try {
      ConvertMp3ToWav.convert(input, output);
    } catch (JavaLayerException e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  @Override
  public long getDurationInMs(Path path) throws IOException {
    try {
      Mp3File file = new Mp3File(path);
      return file.getLengthInMilliseconds();
    } catch (UnsupportedTagException e) {
      throw new IOException(e);
    } catch (InvalidDataException e) {
      throw new IOException(e);
    }
  }
}
