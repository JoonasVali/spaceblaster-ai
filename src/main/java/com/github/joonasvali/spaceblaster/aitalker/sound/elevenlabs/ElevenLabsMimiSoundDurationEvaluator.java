package com.github.joonasvali.spaceblaster.aitalker.sound.elevenlabs;

import com.github.joonasvali.spaceblaster.aitalker.sound.NumbersToWords;
import com.github.joonasvali.spaceblaster.aitalker.sound.SoundDurationEvaluator;

public class ElevenLabsMimiSoundDurationEvaluator implements SoundDurationEvaluator {

  public static final int COLON_PENALTY = 15;
  public static final int PUNCTUATION_PENALTY = 14;
  public static final long PER_CHARACTER_TIME = 40L;
  private static final int WHITESPACE_PENALTY = 6;
  private static final int EXCLAMATION_PENALTY = 15;

  @Override
  public long evaluateDurationInMs(String input) {
    input = input.trim().replaceAll("\\s+", " ").replaceAll("([.,!:])\\s", "$1").replaceAll("\"", "");

    int colonCharacters = input.length() - input.replace(":", "").length();
    int exclamationCharacters = input.length() - input.replace("!", "").length();
    int punctuationCharacters = input.length() - input.replaceAll("[.,]", "").length();
    int whitespaceCharacters = input.length() - input.replaceAll("\\s", "").length();
    int penaltyTotal = colonCharacters * COLON_PENALTY + punctuationCharacters * PUNCTUATION_PENALTY + whitespaceCharacters * WHITESPACE_PENALTY + exclamationCharacters * EXCLAMATION_PENALTY;

    String speech = NumbersToWords.convertNumbersToWords(input);
    int nonPenaltyCharacters = speech.length() - speech.replaceAll("[^\\s.,:!]", "").length();
    return (nonPenaltyCharacters + penaltyTotal) * PER_CHARACTER_TIME;
  }
}
