package com.github.joonasvali.spaceblaster.aitalker;

import com.github.joonasvali.spaceblaster.event.Event;
import com.github.joonasvali.spaceblaster.event.MovingDirection;

public class EventSerializer {
  public static String serialize(Event event) {
    StringBuilder strb = new StringBuilder();
    strb.append("\nGame difficulty is ").append(event.gameDifficulty).append(".");
    strb.append("\n");
    if (event.enemiesStartedWithCannonCount > 0) {
      strb.append("Enemies started with cannon count: ").append(event.enemiesStartedWithCannonCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithGaussGunCount > 0) {
      strb.append("Enemies started with gauss gun count: ").append(event.enemiesStartedWithGaussGunCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithMissileCount > 0) {
      strb.append("Enemies started with missile count: ").append(event.enemiesStartedWithMissileCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithTripleShotCount > 0) {
      strb.append("Enemies started with triple shot count: ").append(event.enemiesStartedWithTripleShotCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithCannonCount > 0) {
      strb.append("Enemies left with cannon count: ").append(event.enemiesLeftWithCannonCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithGaussGunCount > 0) {
      strb.append("Enemies left with gauss gun count: ").append(event.enemiesLeftWithGaussGunCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithMissileCount > 0) {
      strb.append("Enemies left with missile count: ").append(event.enemiesLeftWithMissileCount);
      strb.append("\n");
    }
    if (event.enemiesStartedWithTripleShotCount > 0) {
      strb.append("Enemies left with triple shot count: ").append(event.enemiesLeftWithTripleShotCount);
      strb.append("\n");
    }
    strb.append(event.shotsFiredLastThreeSeconds).append(" shots were fired by player in the last three seconds.");
    strb.append("\n");
    if (event.enemiesHitEnemiesThisRoundCount > 0) {
      strb.append(event.enemiesHitEnemiesThisRoundCount).append(" enemies were hit by enemies this round.");
      strb.append("\n");
    }
    if (event.enemiesKilledEnemiesThisRoundCount > 0) {
      strb.append(event.enemiesKilledEnemiesThisRoundCount).append(" enemies were killed by enemies this round.");
      strb.append("\n");
    }
    strb.append("\n");
    if (event.enemyCloseness != null) {
      strb.append("Enemy distance is ").append(event.enemyCloseness).append(" to the player.");
      strb.append("\n");
    }

    if (!event.inBetweenRounds || event.enemiesKilledThisRoundCount > 0) {
      strb.append("Player has killed this round: ").append(event.enemiesKilledThisRoundCount).append(" enemies.");
      strb.append("\n");

      strb.append("Enemies left this round: ").append(event.enemiesLeftThisRoundCount).append(".");
      strb.append("\n");
    }
    strb.append("Player has ").append(event.playerLivesLeft).append(" of ").append(event.playerLivesOriginal).append(" lives left.");
    strb.append("\n");
    strb.append("Player has collected ").append(event.powerUpsCollectedThisRoundCount).append(" power-ups this round.");
    strb.append("\n");
    if (event.powerUpsMissedCount > 0) {
      strb.append("Player has missed ").append(event.powerUpsMissedCount).append(" power-ups this round.");
      strb.append("\n");
    }
    strb.append("Player has collected a total of ").append(event.powerUpsCollectedTotalCount).append(" power-ups.");
    strb.append("\n");
    if (event.powerUpsCannonCollectedCount > 0) {
      strb.append("Player has collected ").append(event.powerUpsCannonCollectedCount).append(" cannon power-ups.");
      strb.append("\n");
    }
    if (event.powerUpsGaussGunCollectedCount > 0) {
      strb.append("Player has collected ").append(event.powerUpsGaussGunCollectedCount).append(" gauss gun power-ups.");
      strb.append("\n");
    }
    if (event.powerUpsMissileCollectedCount > 0) {
      strb.append("Player has collected ").append(event.powerUpsMissileCollectedCount).append(" missile power-ups.");
      strb.append("\n");
    }
    if (event.powerUpsTripleShotCollectedCount > 0) {
      strb.append("Player has collected ").append(event.powerUpsTripleShotCollectedCount).append(" triple shot power-ups.");
      strb.append("\n");
    }
    strb.append("Player has a score of ").append(event.playerScore).append(".");
    strb.append("\n");
    strb.append("Player is wielding a weapon: ").append(event.playerWeapon).append(".");
    strb.append("\n");
    if (!event.playerDead) {
      strb.append("Is Player moving: ").append(event.playerIsMoving).append(".");
      strb.append("\n");
      if (!event.inBetweenRounds) {
        strb.append("Is Player invincible: ").append(event.playerInvincible).append(".");
        strb.append("\n");
      }
    }
    strb.append("Is Player dead: ").append(event.playerDead).append(".");
    strb.append("\n");

    if (!event.inBetweenRounds) {
      strb.append("Is Player directly under an enemy formation: ").append(event.playerIsUnderEnemyFormation).append(".");
      strb.append("\n");
    }

    strb.append("Is Player victorious: ").append(event.isVictory).append(".");
    strb.append("\n");
    strb.append("Is Player defeated: ").append(event.isDefeat).append(".");
    strb.append("\n");
    if (event.lastDeathTimestamp != null) {
      strb.append("Player last died ").append(msToSeconds(event.eventTimestamp - event.lastDeathTimestamp)).append(" seconds ago.");
      strb.append("\n");
    }
    if (event.lastKillTimestamp != null && !event.inBetweenRounds) {
      strb.append("Player last killed enemy ").append(msToSeconds(event.eventTimestamp - event.lastKillTimestamp)).append(" seconds ago.");
      strb.append("\n");
    }
    if (event.lastHitTimestamp != null && !event.inBetweenRounds) {
      strb.append("Player last hit enemy ").append(msToSeconds(event.eventTimestamp - event.lastHitTimestamp)).append(" seconds ago.");
      strb.append("\n");
    }
    if (event.lastPowerupTimestamp != null) {
      strb.append("Player last collected a power-up ").append(msToSeconds(event.eventTimestamp - event.lastPowerupTimestamp)).append(" seconds ago.");
      strb.append("\n");
    }
    if (event.lastPowerupMissedTimestamp != null) {
      strb.append("Player last missed a power-up ").append(msToSeconds(event.eventTimestamp - event.lastPowerupMissedTimestamp)).append(" seconds ago.");
      strb.append("\n");
    }
    strb.append("Game started ").append(msToSeconds(event.eventTimestamp - event.gameStartTimestamp)).append(" seconds ago.");
    strb.append("\n");

    if (!event.inBetweenRounds && event.roundStartTimestamp != 0) {
      strb.append("Round started ").append(msToSeconds(event.eventTimestamp - event.roundStartTimestamp)).append(" seconds ago.");
      strb.append("\n");
    }

    strb.append("Player has finished ").append(event.roundsFinishedCount).append(" rounds.");
    strb.append("\n");
    strb.append("Player has a fire hit ratio of ").append(toPercentage(event.playerFireHitRatio)).append("%.");
    strb.append("\n");
    if (event.enemyTouchedPlayerDeathsCount > 0) {
      strb.append("Player has touched and died by crashing into the enemy ").append(event.enemyTouchedPlayerDeathsCount).append(" times.");
      strb.append("\n");
    }
    if (event.enemyMovingDirection != MovingDirection.NONE) {
      strb.append("Enemy is moving to ").append(event.enemyMovingDirection).append(" direction.");
      strb.append("\n");
    }
    if (event.enemyBulletFlyingTowardsPlayerDistance != null) {
      strb.append("Enemy bullet is directly flying towards player at ").append(event.enemyBulletFlyingTowardsPlayerDistance).append(" distance.");
      strb.append("\n");
    }
    if (!event.inBetweenRounds) {
      strb.append("Enemy speed is ").append(event.enemySpeed).append(".");
      strb.append("\n");
      strb.append("Enemy formation is at ").append(event.enemyPositionXOnScreen).append(" of the screen.");
      strb.append("\n");
    }
    if (!event.playerDead) {
      strb.append("Player is at position ").append(event.playerPositionX).append(" of the screen.");
      strb.append("\n");
    }
    strb.append("Is game in between two rounds, waiting for next round to happen: ").append(event.inBetweenRounds).append(".");
    strb.append("\n");

    return strb.toString();
  }

  private static Long msToSeconds(Long duration) {
    return (long) Math.round(duration / 1000f);
  }

  private static int toPercentage(float value) {
    return Math.round(value * 100);
  }
}
