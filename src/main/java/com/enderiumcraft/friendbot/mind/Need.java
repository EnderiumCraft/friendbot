package com.enderiumcraft.friendbot.mind;

/**
 * Things the companion wants. Each one runs down over time unless something restores it.
 */
public enum Need {
	FOOD(18_000),
	REST(24_000),
	COMPANY(12_000);

	/** Ticks (20 per second) to run from full to empty when nothing is done about it. */
	final long ticksToEmpty;

	Need(long ticksToEmpty) {
		this.ticksToEmpty = ticksToEmpty;
	}
}
