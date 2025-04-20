package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

/**
 * <a href="https://elevenlabs.io/docs/api-reference/text-to-speech/convert-with-timestamps#request.query.output_format.output_format">...</a>
 */
public enum OutputFormats implements OutputFormat {

  mp3_22050_32("mp3_22050_32"),
  mp3_44100_32("mp3_44100_32"),
  mp3_44100_64("mp3_44100_64"),
  mp3_44100_96("mp3_44100_96"),
  mp3_44100_128("mp3_44100_128"), // DEFAULT
  mp3_44100_192("mp3_44100_192"); // Creator tier or above

  private final String value;

  OutputFormats(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
