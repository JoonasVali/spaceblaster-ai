package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EventDigester {

  public static final int MAX_PERIOD_MS = 20000;
  // This will not be respected if the next event is a high priority event and happens before this period.
  public static final int MIN_PERIOD = 2000;
  private static final Set<EventType> highPriorityEvents = Set.of(EventType.PLAYER_KILLED, EventType.GAME_OVER, EventType.VICTORY, EventType.ROUND_COMPLETED, EventType.POWERUP_COLLECTED);
  private static final Set<EventType> lowPriorityEvents = Set.of(EventType.ENEMY_HIT, EventType.ENEMY_KILLED, EventType.PLAYER_NO_LONGER_INVINCIBLE, EventType.ENEMY_FORMATION_CHANGES_MOVEMENT_DIRECTION);
  public static final int END_OF_GAME_DURATION = 20000;
  public static final int START_OF_GAME_EXTRA_DURATION = 20000;
  private final List<Event> eventList;
  private int index = 0;

  private boolean introduceCommentaryPeriodAtStart;

  public EventDigester(List<Event> eventList, boolean introduceCommentaryPeriodAtStart) {
    this.eventList = eventList;
    this.introduceCommentaryPeriodAtStart = introduceCommentaryPeriodAtStart;
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
    while (index < eventList.size() && (lowPriorityEvents.contains(nextEvent.getType()) || duration < MIN_PERIOD) && !highPriorityEvents.contains(nextEvent.getType()) && duration < MAX_PERIOD_MS) {
      secondaryEvents.add(nextEvent);
      event = nextEvent;
      if (index == eventList.size() - 1) {
        duration += END_OF_GAME_DURATION;
        break;
      }
      nextEvent = eventList.get(++index);
      duration += nextEvent.getEventTimestamp() - event.getEventTimestamp();
    }

    return new Period(startEvent, secondaryEvents, duration);
  }

  public boolean hasNextPeriod() {
    return index < eventList.size();
  }
}
