package com.enderiumcraft.friendbot.mind;

/**
 * Stable traits that bias what the companion prefers. Each trait is from 0 (none) to 1 (strong).
 *
 * @param sociability how much it wants company and how readily it approaches people
 * @param caution     how quickly it backs off from threats and from people who hurt it
 * @param curiosity   how much it wanders and explores when nothing else is pressing
 */
public record Personality(double sociability, double caution, double curiosity) {
	public Personality {
		check("sociability", sociability);
		check("caution", caution);
		check("curiosity", curiosity);
	}

	private static void check(String trait, double value) {
		if (!(value >= 0.0 && value <= 1.0)) {
			throw new IllegalArgumentException(trait + " must be between 0 and 1, was " + value);
		}
	}
}
