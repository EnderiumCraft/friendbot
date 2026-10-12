package com.enderiumcraft.friendbot.mind;

/**
 * Something a player did to or with the companion. Trust rises slowly and falls fast.
 */
public enum Encounter {
	/** The player said hello or otherwise was friendly. */
	GREETED(0.02),
	/** The player handed the companion food. */
	GAVE_FOOD(0.10),
	/** The player hit the companion. */
	HURT_ME(-0.35);

	final double trustChange;

	Encounter(double trustChange) {
		this.trustChange = trustChange;
	}
}
