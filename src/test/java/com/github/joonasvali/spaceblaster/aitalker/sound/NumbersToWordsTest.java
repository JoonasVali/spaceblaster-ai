package com.github.joonasvali.spaceblaster.aitalker.sound;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NumbersToWordsTest {
  @Test
  public void testConvertNumbersToWords() {
    String actual = NumbersToWords.convertNumbersToWords("123");
    Assertions.assertEquals("one hundred twenty-three", actual);
  }

  @Test
  public void testConvertNumbersToWordsInSentence() {
    String actual = NumbersToWords.convertNumbersToWords("Chicken nr 3 laid 123 eggs.");
    Assertions.assertEquals("Chicken nr three laid one hundred twenty-three eggs.", actual);
  }

  @Test
  public void testEmpty() {
    String actual = NumbersToWords.convertNumbersToWords("");
    Assertions.assertEquals("", actual);
  }
}
