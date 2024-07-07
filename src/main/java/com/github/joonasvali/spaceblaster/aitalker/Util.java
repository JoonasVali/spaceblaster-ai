package com.github.joonasvali.spaceblaster.aitalker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
  private static Logger logger = LoggerFactory.getLogger(Util.class);

  public static String convertToSecondsAndMs(long l) {
    long seconds = l / 1000;
    long ms = l % 1000;
    return seconds + "." + ms + "s";
  }

  public static void sleep(long period) {
    try {
      Thread.sleep(period);
    } catch (InterruptedException e) {
      logger.error("Interrupted while sleeping", e);
      Thread.currentThread().interrupt();
    }
  }
}
