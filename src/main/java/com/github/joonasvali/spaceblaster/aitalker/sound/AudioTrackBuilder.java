package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion.WavAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;

public class AudioTrackBuilder {
  private final ArrayDeque<TimedVoice> voices = new ArrayDeque<>();
  private final int sampleRate;

  private boolean alignFirstSoundToEndOfPeriod;

  /**
   *
   * @param sampleRate
   * @param alignFirstSoundToEndOfPeriod if the first sound is shorter than its intended period, then it's aligned to
   *                                  the back of the period.
   */
  public AudioTrackBuilder(int sampleRate, boolean alignFirstSoundToEndOfPeriod) {
    this.sampleRate = sampleRate;
    this.alignFirstSoundToEndOfPeriod = alignFirstSoundToEndOfPeriod;
  }

  /**
   * @param text the text contained in the sound file
   * @param startTime in milliseconds from the start of the track (0)
   * @param soundFile the sound file to play
   */
  public void addVoice(String text, long startTime, long voiceDuration, long voiceCutOffMs, Path soundFile) {
    voices.add(new TimedVoice(text, startTime, voiceDuration, voiceCutOffMs, soundFile));
  }

  public long getLastVoiceEndTime() {
    if (voices.isEmpty()) {
      return 0;
    }
    return voices.getLast().startTime + Math.min(voices.getLast().voiceDuration, voices.getLast().voiceCutoffMs);
  }

  public List<TimedVoice> produce(Path outputFile) throws IOException {
    WavAppender appender = new WavAppender(outputFile, sampleRate);
    for (TimedVoice voice : voices.stream().sorted(Comparator.comparingLong(a -> a.startTime)).toList()) {
      if (alignFirstSoundToEndOfPeriod && voice.startTime() == 0) {
        if (voice.voiceCutoffMs > voice.voiceDuration) {
          appender.addSilence(voice.voiceCutoffMs - voice.voiceDuration);
        }
        appender.addWavFile(voice.soundFile, voice.voiceDuration, voice.voiceCutoffMs);
      } else {
        appender.addWavFile(voice.soundFile, voice.voiceDuration, voice.voiceCutoffMs);
        if (voice.voiceCutoffMs > voice.voiceDuration) {
          appender.addSilence(voice.voiceCutoffMs - voice.voiceDuration);
        }
      }
    }
    appender.create();

    return List.copyOf(voices);
  }

  public void validateLastTimestampAfter(long relativeTimestamp) {
    var last = voices.getLast();
    if (last.startTime < relativeTimestamp) {
      throw new IllegalStateException("Last voice end time is before the relative timestamp: " + getLastVoiceEndTime() + " < " + relativeTimestamp);
    }
  }

  public long getLastVoiceStartTime() {
    if (voices.isEmpty()) {
      return 0;
    }
    return voices.getLast().startTime;
  }

  public TimedVoice getLastVoice() {
    return voices.getLast();
  }


  public record TimedVoice(String text, long startTime, long voiceDuration, long voiceCutoffMs, Path soundFile) {
  }
}
