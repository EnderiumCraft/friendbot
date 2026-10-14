package com.enderiumcraft.friendbot.mind;

/**
 * Something that happens between the companion and a player. Trust rises slowly and falls fast.
 */
public enum Encounter {
	/** The player said hello or otherwise was friendly. */
	GREETED(0.02),
	/** The player handed the companion food. */
	GAVE_FOOD(0.10),
	/** The player hit the companion. */
	HURT_ME(-0.35),
	/** A player came into view again after being away for a while. This is not a deliberate act, so it changes no trust. */
	ARRIVED(0.0);

	final double trustChange;

	Encounter(double trustChange) {
		this.trustChange = trustChange;
	}
}
