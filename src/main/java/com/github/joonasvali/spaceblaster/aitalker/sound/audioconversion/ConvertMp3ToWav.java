package com.github.joonasvali.spaceblaster.aitalker.sound.audioconversion;

import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;

import java.nio.file.Path;

public class ConvertMp3ToWav {
  public static void convert(Path input, Path output) throws JavaLayerException {
    Converter converter = new Converter();
    converter.convert(input.toString(), output.toString());
  }
}
