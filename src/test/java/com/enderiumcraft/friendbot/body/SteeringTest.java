package com.enderiumcraft.friendbot.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.enderiumcraft.friendbot.mind.Intent;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SteeringTest {
	private static final Vec2 HOME = new Vec2(0.0, 0.0);
	private static final double EPS = 1e-9;

	private static void assertPoint(double expectedX, double expectedZ, Vec2 actual) {
		assertEquals(expectedX, actual.x(), EPS);
		assertEquals(expectedZ, actual.z(), EPS);
	}

	@Test
	void approachStopsTwoBlocksShortOfThePlayer() {
		Goal goal = Steering.plan(Intent.APPROACH_PLAYER, HOME, Optional.of(new Vec2(10.0, 0.0)), Optional.empty(), 0.0)
				.orElseThrow();

		assertPoint(8.0, 0.0, goal.target());
		assertEquals(Steering.WALK_SPEED, goal.speed(), EPS);
	}

	@Test
	void approachDoesNothingWhenAlreadyClose() {
		Optional<Goal> goal = Steering.plan(Intent.APPROACH_PLAYER, HOME, Optional.of(new Vec2(1.0, 0.0)), Optional.empty(), 0.0);

		assertFalse(goal.isPresent());
	}

	@Test
	void keepAwayBacksOffFromThePlayerDirection() {
		Goal goal = Steering.plan(Intent.KEEP_AWAY_FROM_PLAYER, HOME, Optional.of(new Vec2(3.0, 0.0)), Optional.empty(), 0.0)
				.orElseThrow();

		assertPoint(-6.0, 0.0, goal.target());
	}

	@Test
	void fleeRunsAwayFromTheThreatAtRunningSpeed() {
		Goal goal = Steering.plan(Intent.FLEE, HOME, Optional.empty(), Optional.of(new Vec2(0.0, 4.0)), 0.0)
				.orElseThrow();

		assertPoint(0.0, -12.0, goal.target());
		assertEquals(Steering.FLEE_SPEED, goal.speed(), EPS);
	}

	@Test
	void wanderUsesTheRollAsADirection() {
		Goal goal = Steering.plan(Intent.WANDER, HOME, Optional.empty(), Optional.empty(), 0.25).orElseThrow();

		assertPoint(0.0, 5.0, goal.target());
		assertEquals(Steering.WANDER_SPEED, goal.speed(), EPS);
	}

	@Test
	void restEatAndIdleStayPut() {
		for (Intent intent : new Intent[] {Intent.IDLE, Intent.EAT, Intent.REST}) {
			assertFalse(Steering.plan(intent, HOME, Optional.of(new Vec2(5.0, 0.0)), Optional.empty(), 0.0).isPresent(),
					intent.name());
		}
	}

	@Test
	void missingTargetMeansNoGoal() {
		assertFalse(Steering.plan(Intent.FLEE, HOME, Optional.empty(), Optional.empty(), 0.0).isPresent());
		assertFalse(Steering.plan(Intent.KEEP_AWAY_FROM_PLAYER, HOME, Optional.empty(), Optional.empty(), 0.0).isPresent());
	}

	@Test
	void fleeFromAThreatOnTopOfTheCompanionPicksTheRollDirection() {
		Goal goal = Steering.plan(Intent.FLEE, HOME, Optional.empty(), Optional.of(HOME), 0.0).orElseThrow();

		assertTrue(goal.target().distanceTo(HOME) > 11.9);
		assertPoint(12.0, 0.0, goal.target());
	}

	@Test
	void normalizedVectorsKeepTheirDirectionAndZeroStaysZero() {
		assertEquals(1.0, new Vec2(3.0, 4.0).normalized().length(), EPS);
		assertEquals(Vec2.ZERO, Vec2.ZERO.normalized());
	}
}
