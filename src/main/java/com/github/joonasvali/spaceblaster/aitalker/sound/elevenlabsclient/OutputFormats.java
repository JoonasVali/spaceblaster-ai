package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabsclient;

/**
 * <a href="https://elevenlabs.io/docs/api-reference/text-to-speech/convert-with-timestamps#request.query.output_format.output_format">...</a>
 */
public enum OutputFormats implements OutputFormat {

  mp3_22050_32("mp3_22050_32"),
  mp3_44100_32("mp3_44100_32"),
  mp3_44100_64("mp3_44100_64"),
  mp3_44100_96("mp3_44100_96"),
  mp3_44100_128("mp3_44100_128"),
  mp3_44100_192("mp3_44100_192"),
  pcm_8000("pcm_8000"),
  pcm_16000("pcm_16000"),
  pcm_22050("pcm_22050"),
  pcm_24000("pcm_24000"),
  pcm_44100("pcm_44100"),
  ulaw_8000("ulaw_8000"),
  alaw_8000("alaw_8000"),
  opus_48000_32("opus_48000_32"),
  opus_48000_64("opus_48000_64"),
  opus_48000_96("opus_48000_96"),
  opus_48000_128("opus_48000_128"),
  opus_48000_192("opus_48000_192");


  private final String value;

  OutputFormats(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }
}
