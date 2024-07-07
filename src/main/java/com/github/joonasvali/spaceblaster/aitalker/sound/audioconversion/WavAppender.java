package com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class WavAppender {
  private final Path output;
  private final List<Entry> entryList = new ArrayList<>();
  private final int sampleRate;

  public WavAppender(Path output, int sampleRate) {
    this.output = output;
    this.sampleRate = sampleRate;
  }

  public void addWavFile(Path wavFile, long realDuration, long millisecondsToCutoff) {
    entryList.add(new AudioEntry(wavFile, realDuration, millisecondsToCutoff));
  }

  public void addWavFile(Path wavFile, long realDuration) {
    addWavFile(wavFile, realDuration, Long.MAX_VALUE);
  }

  public void addSilence(long milliseconds) {
    entryList.add(new SilenceEntry(milliseconds));
  }

  public void create() throws IOException {
    Enumeration<AudioInputStream> enumeration = Collections.enumeration(entryList.stream()
        .map(entry -> {
          try {
            return entry.getStream();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toList()));
    try {
      long totalFrameLength = entryList.stream()
          .mapToLong(Entry::getDuration)
          .sum() * sampleRate / 1000;

      AudioInputStream appendedFiles =
          new AudioInputStream(
              new SequenceInputStream(enumeration),
              new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                  sampleRate,
                  16,
                  1, // Mono
                  2, // A 16-bit sample is 2 bytes (since 1 byte = 8 bits), and since we're working with mono audio, there's only one sample per frame. Therefore, the frameSize is 2 bytes.
                  sampleRate,
                  false),
              totalFrameLength);

      AudioSystem.write(appendedFiles,
          AudioFileFormat.Type.WAVE,
          output.toFile()
      );
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }

  private interface Entry {
    long getDuration();

    AudioInputStream getStream() throws IOException;
  }

  private record AudioEntry(Path wavFile, long realDuration, Long millisecondsToCutoff) implements Entry {

    @Override
    public long getDuration() {
      return Math.min(millisecondsToCutoff, realDuration);
    }

    @Override
    public AudioInputStream getStream() throws IOException {
      try {
        return new LimitedAudioInputStream(AudioSystem.getAudioInputStream(wavFile.toFile()), getDuration());
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }

  private record SilenceEntry(long milliseconds) implements Entry {

    @Override
    public long getDuration() {
      return milliseconds;
    }

    @Override
    public AudioInputStream getStream() throws IOException {
      return SilentWav.createSilentStream(milliseconds);
    }
  }

  public static class LimitedAudioInputStream extends AudioInputStream {
    private long bytesToRead;
    private long bytesRead = 0;

    public LimitedAudioInputStream(AudioInputStream stream, long durationMs) {
      super(stream, stream.getFormat(), stream.getFrameLength());
      AudioFormat format = stream.getFormat();
      long frameSize = format.getFrameSize();
      float frameRate = format.getFrameRate();
      // Calculate total number of bytes to read based on durationMs
      this.bytesToRead = (long) ((durationMs / 1000.0) * frameRate * frameSize);
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= bytesToRead) {
        return -1; // End of stream
      }
      int result = super.read();
      if (result != -1) {
        bytesRead++;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytesRead >= bytesToRead) {
        return -1; // End of stream
      }
      long bytesRemaining = bytesToRead - bytesRead;
      int bytesToReadNow = (int) Math.min(len, bytesRemaining);
      int result = super.read(b, off, bytesToReadNow);
      if (result != -1) {
        bytesRead += result;
      }
      return result;
    }

    @Override
    public long skip(long n) throws IOException {
      long bytesRemaining = bytesToRead - bytesRead;
      long bytesToSkip = Math.min(n, bytesRemaining);
      long skipped = super.skip(bytesToSkip);
      bytesRead += skipped;
      return skipped;
    }

    @Override
    public int available() throws IOException {
      return (int) Math.min(super.available(), bytesToRead - bytesRead);
    }

    @Override
    public void close() throws IOException {
      super.close();
    }
  }
}


