package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventDigester {
  private static final Logger log = LoggerFactory.getLogger(EventDigester.class);

  public static final int MAX_PERIOD_MS = 20000;
  // This will not be respected if the next event is a high priority event and happens before this period.
  public static final int MIN_PERIOD = 2000;
  public static final int SMALL_PERIOD = 6000;
  private static final Set<EventType> highPriorityEvents = Set.of(EventType.PLAYER_KILLED, EventType.GAME_OVER, EventType.VICTORY, EventType.ROUND_COMPLETED, EventType.POWERUP_COLLECTED);
  private static final Set<EventType> lowPriorityEvents = Set.of(EventType.ENEMY_HIT, EventType.ENEMY_KILLED, EventType.PLAYER_NO_LONGER_INVINCIBLE, EventType.ENEMY_FORMATION_CHANGES_MOVEMENT_DIRECTION);
  public static final int END_OF_GAME_DURATION = 20000;
  public static final int START_OF_GAME_EXTRA_DURATION = 20000;
  private final List<Event> eventList;
  private int index = 0;

  private boolean introduceCommentaryPeriodAtStart;
  private final Path screenshotFolder;

  public EventDigester(List<Event> eventList, Path screenshotFolder, boolean introduceCommentaryPeriodAtStart) {
    this.eventList = eventList;
    this.introduceCommentaryPeriodAtStart = introduceCommentaryPeriodAtStart;
    this.screenshotFolder = screenshotFolder;
  }

  public Period getNextPeriod() {
    Event event = eventList.get(index);

    if (index == 0 && introduceCommentaryPeriodAtStart) {
      event.eventTimestamp = event.eventTimestamp - START_OF_GAME_EXTRA_DURATION;
    }

    Event startEvent = event;
    List<Event> secondaryEvents = new ArrayList<>();
    if (index == eventList.size() - 1) {
      index++;
      return new Period(startEvent, secondaryEvents, END_OF_GAME_DURATION);
    }
    Event nextEvent = eventList.get(++index);

    long duration = nextEvent.getEventTimestamp() - event.getEventTimestamp();
    boolean finishWhenMinPeriodExceeds = false;
    while (index < eventList.size()) {
      boolean isMidPriority =
          !lowPriorityEvents.contains(nextEvent.getType()) &&
              !highPriorityEvents.contains(nextEvent.getType());
      boolean isHighPriority = highPriorityEvents.contains(nextEvent.getType());

      if (finishWhenMinPeriodExceeds && duration > MIN_PERIOD) {
        break;
      }

      if (duration >= MAX_PERIOD_MS) {
        break;
      }

      if (isHighPriority) {
        if (duration >= MIN_PERIOD) {
          break;
        } else {
          finishWhenMinPeriodExceeds = true;
        }
      }

      if (duration >= SMALL_PERIOD && isMidPriority) {
        break;
      }

      secondaryEvents.add(nextEvent);
      event = nextEvent;
      if (index == eventList.size() - 1) {
        duration += END_OF_GAME_DURATION;
        break;
      }
      nextEvent = eventList.get(++index);
      duration += nextEvent.getEventTimestamp() - event.getEventTimestamp();
    }

    Path screenshotPath = screenshotFolder.resolve(event.eventTimestamp + ".png");
    BufferedImage screenshot = null;
    if (Files.exists(screenshotPath)) {
      try {
        screenshot = ImageIO.read(screenshotPath.toFile());
      } catch (IOException e) {
        log.error("Failed to read screenshot: " + screenshotPath, e);
      }
    }
    return new Period(startEvent, secondaryEvents, screenshot, duration);
  }

  public boolean hasNextPeriod() {
    return index < eventList.size();
  }
}