package com.enderiumcraft.friendbot.body;

import com.enderiumcraft.friendbot.mind.Intent;
import java.util.Optional;

/**
 * Turns an {@link Intent} into a walk {@link Goal} from where the companion is standing.
 *
 * <p>Positions come from the game side. {@code roll} is a random number from 0 up to 1, supplied
 * by the caller, so this class stays deterministic and testable.
 */
public final class Steering {
	/** How close the companion stops to a player it is approaching. */
	static final double CONVERSATION_DISTANCE = 2.0;
	/** How far the companion backs off from a player it no longer trusts. */
	static final double BACK_OFF_DISTANCE = 6.0;
	/** How far the companion runs from a threat. */
	static final double FLEE_DISTANCE = 12.0;
	/** How far a wandering companion strolls in one go. */
	static final double WANDER_DISTANCE = 5.0;

	static final double WALK_SPEED = 1.0;
	static final double WANDER_SPEED = 0.5;
	static final double FLEE_SPEED = 1.5;

	private Steering() {
	}

	/**
	 * @param intent   what the companion has decided to do
	 * @param self     where the companion is standing
	 * @param player   where the nearest player is, if there is one
	 * @param threat   where the nearest hostile mob is, if there is one
	 * @param roll     a random number from 0 up to 1
	 * @return the walk to start, or empty when the companion should stop where it is
	 */
	public static Optional<Goal> plan(Intent intent, Vec2 self, Optional<Vec2> player, Optional<Vec2> threat, double roll) {
		return switch (intent) {
			case APPROACH_PLAYER -> player.flatMap(target -> approach(self, target));
			case KEEP_AWAY_FROM_PLAYER -> player.map(target ->
					new Goal(self.plus(awayFrom(target, self, roll).scale(BACK_OFF_DISTANCE)), WALK_SPEED));
			case FLEE -> threat.map(target ->
					new Goal(self.plus(awayFrom(target, self, roll).scale(FLEE_DISTANCE)), FLEE_SPEED));
			case WANDER -> Optional.of(new Goal(
					self.plus(Vec2.fromAngle(roll * 2.0 * Math.PI).scale(WANDER_DISTANCE)), WANDER_SPEED));
			case IDLE, EAT, REST -> Optional.empty();
		};
	}

	private static Optional<Goal> approach(Vec2 self, Vec2 player) {
		Vec2 toPlayer = player.minus(self);
		if (toPlayer.length() <= CONVERSATION_DISTANCE) {
			return Optional.empty();
		}
		Vec2 stopPoint = player.minus(toPlayer.normalized().scale(CONVERSATION_DISTANCE));
		return Optional.of(new Goal(stopPoint, WALK_SPEED));
	}

	/** The direction pointing from {@code from} toward {@code self}, or a random one if they overlap. */
	private static Vec2 awayFrom(Vec2 from, Vec2 self, double roll) {
		Vec2 away = self.minus(from).normalized();
		return away.equals(Vec2.ZERO) ? Vec2.fromAngle(roll * 2.0 * Math.PI) : away;
	}
}
