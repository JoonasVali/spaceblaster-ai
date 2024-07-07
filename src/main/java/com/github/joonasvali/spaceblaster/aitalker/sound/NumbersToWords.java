package com.github.joonasvali.spaceblaster.aitalker.sound;

import com.ibm.icu.text.RuleBasedNumberFormat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumbersToWords {
  public static String convertNumbersToWords(String input) {
    RuleBasedNumberFormat nf = new RuleBasedNumberFormat(Locale.ENGLISH, RuleBasedNumberFormat.SPELLOUT);
    Pattern pattern = Pattern.compile("\\d+");
    Matcher matcher = pattern.matcher(input);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String numberStr = matcher.group();
      int number = Integer.parseInt(numberStr);
      String wordRepresentation = nf.format(number);
      matcher.appendReplacement(result, wordRepresentation);
    }
    matcher.appendTail(result);

    result = new StringBuilder(result.toString().replaceAll("%", " percent"));

    return result.toString();
  }
}
