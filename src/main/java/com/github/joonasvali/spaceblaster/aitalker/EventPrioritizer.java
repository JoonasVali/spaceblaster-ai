package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.event.EventType;

public class EventPrioritizer {
  public static int getPriority(EventType type) {
    switch (type) {
      case VICTORY:
        return 1;
      case GAME_OVER:
        return 2;
      case START_GAME:
        return 3;
      case PLAYER_KILLED:
        return 4;
      case LOAD_LEVEL:
        return 5;
      case POWERUP_CREATED:
        return 6;
      case ROUND_COMPLETED:
        return 7;
      case POWERUP_MISSED:
        return 8;
      case PLAYER_NARROWLY_ESCAPED_INCOMING_BULLET:
        return 9;
      case ENEMY_FORMATION_CHANGES_MOVEMENT_DIRECTION:
        return 10;
      case PLAYER_BORN:
        return 11;
      case ENEMY_KILLED_BY_ENEMY:
        return 12;
      case ENEMY_KILLED:
        return 13;
      case POWERUP_COLLECTED:
        return 14;
      case PLAYER_NO_LONGER_INVINCIBLE:
        return 15;
      case ENEMY_HIT:
        return 16;
    }
    return 1000;
  }
}
