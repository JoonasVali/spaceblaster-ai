package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.event.Event;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Period {
  private final Event event;
  private final List<Event> secondaryEvents;

  private final Long duration;

  public Period(Event event, List<Event> secondaryEvents, long duration) {
    this.event = event;
    this.secondaryEvents = secondaryEvents;
    this.duration = duration;
  }

  @Override
  public String toString() {
    return timeStampToHumanReadable(event.getEventTimestamp()) + " Period{" +
        "start=" + event +
        ", secondaryEvents=" + secondaryEvents +
        ", duration=" + Util.convertToSecondsAndMs(duration) +
        '}';
  }

  public String timeStampToHumanReadable(long timeStamp) {
    Instant instant = Instant.ofEpochMilli(timeStamp);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
    return zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  public Long getIntendedDuration() {
    return Math.max(EventDigester.MIN_PERIOD, duration);
  }

  public Long getPeriodDuration() {
    return duration;
  }

  public Event getEvent() {
    return event;
  }

  public List<Event> getSecondaryEvents() {
    return secondaryEvents;
  }
}
