package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

/**
 * <a href="https://elevenlabs.io/docs/api-reference/text-to-speech/convert-with-timestamps#request.query.output_format.output_format">...</a>
 */
public enum OutputFormats implements OutputFormat {

  mp3_22050_32("mp3_22050_32", 22050, 32),
  mp3_44100_32("mp3_44100_32", 44100, 32),
  mp3_44100_64("mp3_44100_64", 44100, 64),
  mp3_44100_96("mp3_44100_96", 44100, 96),
  mp3_44100_128("mp3_44100_128", 44100, 128), // DEFAULT
  mp3_44100_192("mp3_44100_192", 44100, 192); // Creator tier or above

  private final String value;
  private final int sampleRate;
  private final int bitRate;

  OutputFormats(String value, int sampleRate, int bitRate) {
    this.value = value;
    this.sampleRate = sampleRate;
    this.bitRate = bitRate;
  }

  @Override
  public String getValue() {
    return value;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public int getBitRate() {
    return bitRate;
  }
}