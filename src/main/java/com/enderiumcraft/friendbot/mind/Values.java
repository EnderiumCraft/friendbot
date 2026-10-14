package com.enderiumcraft.friendbot.mind;

/**
 * Numeric helpers shared by the mind's state classes.
 */
final class Values {
	private Values() {
	}

	/**
	 * Clamps to the range 0 to 1. NaN can come from corrupt saved data or a bad calculation, so it
	 * becomes the fallback instead of spreading through every calculation that reads it.
	 */
	static double unit(double value, double fallback) {
		return Double.isNaN(value) ? fallback : Math.clamp(value, 0.0, 1.0);
	}
}
