package com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public class ConvertMp3ToWav {
  public static void convert(Path input, Path output) throws JavaLayerException {
    Converter converter = new Converter();
    converter.convert(input.toString(), output.toString());
  }

  /**
   * Convert an MP3 file to WAV format and return it as a Base64 string.
   * @param stream The input stream of the MP3 file.
   * @return The converted WAV audio in Base64 format.
   * @throws JavaLayerException If conversion fails or I/O error occurs.
   */
  public static String convert(InputStream stream) throws JavaLayerException {
    try {
      // Create temporary files for MP3 input and WAV output
      Path tempMp3 = Files.createTempFile("speech", ".mp3");
      Path tempWav = Files.createTempFile("speech", ".wav");

      // Write the MP3 stream to the temporary file
      Files.copy(stream, tempMp3, StandardCopyOption.REPLACE_EXISTING);

      // Perform the conversion
      Converter converter = new Converter();
      converter.convert(tempMp3.toString(), tempWav.toString());

      // Read the converted WAV bytes
      byte[] wavBytes = Files.readAllBytes(tempWav);

      // Encode WAV bytes to Base64
      String base64Wav = Base64.getEncoder().encodeToString(wavBytes);

      // Clean up temporary files
      Files.deleteIfExists(tempMp3);
      Files.deleteIfExists(tempWav);

      return base64Wav;
    } catch (IOException e) {
      throw new JavaLayerException("Error processing audio conversion", e);
    }
  }
}

