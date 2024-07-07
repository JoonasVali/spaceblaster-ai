package com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WavDuration {
  public static long getDuration(Path soundFile) throws IOException {
    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(soundFile)))) {
      AudioFormat format = audioInputStream.getFormat();
      long frames = audioInputStream.getFrameLength();
      return (long) Math.ceil((frames + 0.0) / format.getFrameRate() * 1000);
    } catch (UnsupportedAudioFileException e) {
      throw new IOException(e);
    }
  }
}
