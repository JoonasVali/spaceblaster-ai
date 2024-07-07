package com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class SilentWav {
  private final Path outputDirectory;

  public SilentWav(Path outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public Path createSilentFile(long milliseconds) throws IOException {
    AudioInputStream audioInputStream = createSilentStream(milliseconds);

    Path outputFile = outputDirectory.resolve("silence-" + milliseconds + ".wav");

    // Write the silent audio data to the output file
    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile.toFile());
    audioInputStream.close();
    return outputFile;

  }

  public static AudioInputStream createSilentStream(long milliseconds) throws IOException {

    float sampleRate = 44100;
    int sampleSizeInBits = 16;
    int channels = 1; // Mono
    boolean signed = true; // Signed PCM data
    boolean bigEndian = false; // Little-endian byte order

    AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

    int numFrames = (int) ((milliseconds / 1000.0) * sampleRate);

    byte[] silentData = new byte[numFrames * (sampleSizeInBits / 8) * channels];

    ByteArrayInputStream bais = new ByteArrayInputStream(silentData);

    return new AudioInputStream(bais, format, numFrames);
  }
}
