package com.enderiumcraft.friendbot.body;

/**
 * A point or direction on the ground plane, in blocks. Only x and z, because the companion walks.
 */
public record Vec2(double x, double z) {
	public static final Vec2 ZERO = new Vec2(0.0, 0.0);

	/** A unit vector pointing at the given angle, in radians. */
	public static Vec2 fromAngle(double radians) {
		return new Vec2(Math.cos(radians), Math.sin(radians));
	}

	public Vec2 plus(Vec2 other) {
		return new Vec2(x + other.x, z + other.z);
	}

	public Vec2 minus(Vec2 other) {
		return new Vec2(x - other.x, z - other.z);
	}

	public Vec2 scale(double factor) {
		return new Vec2(x * factor, z * factor);
	}

	public double length() {
		return Math.hypot(x, z);
	}

	public double distanceTo(Vec2 other) {
		return minus(other).length();
	}

	/** The same direction with length 1, or {@link #ZERO} if there is no direction. */
	public Vec2 normalized() {
		double length = length();
		return length < 1e-9 ? ZERO : scale(1.0 / length);
	}
}
