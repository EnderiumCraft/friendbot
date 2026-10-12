package com.enderiumcraft.friendbot.body;

/**
 * Where the companion should walk and how fast, relative to its normal walking speed.
 *
 * @param target the ground position to walk to
 * @param speed  1.0 is a normal walk; higher values run
 */
public record Goal(Vec2 target, double speed) {
}
