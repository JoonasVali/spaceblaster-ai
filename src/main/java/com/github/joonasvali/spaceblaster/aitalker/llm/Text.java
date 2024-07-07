package com.github.joonasvali.spaceblaster.aitalker.llm;

public class Text {
  private final String longTerm;
  private final String shortTerm;

  private boolean rememberShortTerm = true;

  public Text(String longTerm, String shortTerm) {
    this.longTerm = longTerm;
    this.shortTerm = shortTerm;
  }

  public void forgetShortTerm() {
    rememberShortTerm = false;
  }

  @Override
  public String toString() {
    return rememberShortTerm ? longTerm + "\n" + shortTerm : longTerm;
  }
}

