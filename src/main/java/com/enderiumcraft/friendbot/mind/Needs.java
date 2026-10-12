package com.enderiumcraft.friendbot.mind;

import java.util.EnumMap;

/**
 * How satisfied each {@link Need} is, from 0 (desperate) to 1 (fully satisfied).
 */
public final class Needs {
	private final EnumMap<Need, Double> levels = new EnumMap<>(Need.class);

	public Needs() {
		for (Need need : Need.values()) {
			levels.put(need, 1.0);
		}
	}

	public double level(Need need) {
		return levels.get(need);
	}

	/** How badly the companion wants this, from 0 (not at all) to 1 (desperate). */
	public double urgency(Need need) {
		return 1.0 - levels.get(need);
	}

	public void restore(Need need, double amount) {
		levels.put(need, Math.clamp(levels.get(need) + amount, 0.0, 1.0));
	}

	void decay(long ticks) {
		for (Need need : Need.values()) {
			double next = levels.get(need) - (double) ticks / need.ticksToEmpty;
			levels.put(need, Math.clamp(next, 0.0, 1.0));
		}
	}
}
