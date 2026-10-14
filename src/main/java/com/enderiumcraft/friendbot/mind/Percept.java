package com.enderiumcraft.friendbot.mind;

import java.util.Objects;
import java.util.Optional;

/**
 * What the companion can sense right now, already reduced to plain values.
 * The game side builds one of these every tick.
 *
 * @param tick          current world tick (20 per second)
 * @param player        the closest player the companion can notice, if any
 * @param hostileNearby whether a hostile mob is close enough to be a threat
 * @param health        the companion's health, from 0 (dead) to 1 (full). An unknown (NaN) value is treated as full.
 * @param hasFood       whether the companion is carrying something to eat
 * @param night         whether it is night
 */
public record Percept(
		long tick,
		Optional<Sighting> player,
		boolean hostileNearby,
		double health,
		boolean hasFood,
		boolean night) {

	public Percept {
		health = Values.unit(health, 1.0);
	}

	/**
	 * @param name     the player's name, which must not be null
	 * @param distance blocks between the companion and the player
	 */
	public record Sighting(String name, double distance) {
		public Sighting {
			Objects.requireNonNull(name, "a sighting needs a player name");
		}
	}
}
