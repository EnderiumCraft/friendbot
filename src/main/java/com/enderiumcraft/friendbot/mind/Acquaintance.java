package com.enderiumcraft.friendbot.mind;

/**
 * What the companion remembers about one player.
 */
public final class Acquaintance {
	/** About 10 minutes spent together makes a stranger familiar. */
	private static final double TICKS_TO_FAMILIAR = 12_000;
	/** About 40 minutes apart and the details start to slip. */
	private static final double TICKS_TO_FORGET = 48_000;
	private static final long NEVER = Long.MIN_VALUE;

	private final String name;
	private double trust = 0.5;
	private double familiarity = 0.0;
	private long lastSeenTick = NEVER;

	Acquaintance(String name) {
		this.name = name;
	}

	Acquaintance(String name, double trust, double familiarity, long lastSeenTick) {
		this.name = name;
		this.trust = Math.clamp(trust, 0.0, 1.0);
		this.familiarity = Math.clamp(familiarity, 0.0, 1.0);
		this.lastSeenTick = lastSeenTick;
	}

	long lastSeenTick() {
		return lastSeenTick;
	}

	public String name() {
		return name;
	}

	/** From 0 (hostile) to 1 (completely trusted). Strangers start at 0.5. */
	public double trust() {
		return trust;
	}

	/** From 0 (stranger) to 1 (knows them well). */
	public double familiarity() {
		return familiarity;
	}

	boolean seenWithin(long tick, long ticks) {
		return lastSeenTick != NEVER && tick - lastSeenTick <= ticks;
	}

	void seen(long tick, long elapsed) {
		familiarity = Math.clamp(familiarity + elapsed / TICKS_TO_FAMILIAR, 0.0, 1.0);
		lastSeenTick = tick;
	}

	boolean lastSeenAt(long tick) {
		return lastSeenTick == tick;
	}

	void fade(long elapsed) {
		familiarity = Math.clamp(familiarity - elapsed / TICKS_TO_FORGET, 0.0, 1.0);
	}

	void apply(Encounter encounter) {
		trust = Math.clamp(trust + encounter.trustChange, 0.0, 1.0);
	}
}
