package com.enderiumcraft.friendbot.mind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MindTest {
	private static final Personality NEUTRAL = new Personality(0.5, 0.5, 0.5);
	private static final Percept.Sighting SAM_NEAR = new Percept.Sighting("Sam", 6.0);

	private static Percept calm(long tick, boolean hasFood) {
		return new Percept(tick, Optional.empty(), false, 1.0, hasFood, false);
	}

	private static Percept withPlayer(long tick, Percept.Sighting player) {
		return new Percept(tick, Optional.of(player), false, 1.0, false, false);
	}

	@Test
	void hungryCompanionEatsWhenItHasFood() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, true));

		Decision later = mind.think(calm(15_000, true)).orElseThrow();

		assertEquals(Intent.EAT, later.intent());
	}

	@Test
	void hungryCompanionWithoutFoodDoesNotEat() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));

		Decision later = mind.think(calm(15_000, false)).orElseThrow();

		assertNotEquals(Intent.EAT, later.intent());
	}

	@Test
	void hostileMobInterruptsAnIntentThatIsStillHeld() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));

		Percept threat = new Percept(5, Optional.empty(), true, 1.0, false, false);
		Decision reaction = mind.think(threat).orElseThrow();

		assertEquals(Intent.FLEE, reaction.intent());
	}

	@Test
	void intentIsHeldUntilTheMinimumTimePasses() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		assertTrue(mind.think(calm(0, false)).isPresent());

		assertFalse(mind.think(calm(10, false)).isPresent());
		assertTrue(mind.think(calm(Mind.MIN_HOLD_TICKS, false)).isPresent());
	}

	@Test
	void sociableCompanionGreetsAPlayerWhoJustArrived() {
		Mind mind = new Mind("Alex", new Personality(0.8, 0.5, 0.5), new UtilityProvider());
		mind.think(calm(0, false));

		Decision greeting = mind.think(withPlayer(10, SAM_NEAR)).orElseThrow();

		assertEquals("Hey Sam!", greeting.say().orElseThrow());
	}

	@Test
	void playerWhoHitsTheCompanionIsAvoided() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(withPlayer(0, SAM_NEAR));

		mind.recordEncounter("Sam", Encounter.HURT_ME);
		Decision reaction = mind.think(withPlayer(Mind.MIN_HOLD_TICKS, SAM_NEAR)).orElseThrow();

		assertEquals(Intent.KEEP_AWAY_FROM_PLAYER, reaction.intent());
	}

	@Test
	void giftRaisesTrustAndHurtLowersItFaster() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());

		mind.recordEncounter("Sam", Encounter.GAVE_FOOD);
		assertEquals(0.6, mind.acquaintance("Sam").trust(), 1e-9);

		mind.recordEncounter("Sam", Encounter.HURT_ME);
		assertEquals(0.25, mind.acquaintance("Sam").trust(), 1e-9);
	}

	@Test
	void needsDecayOverTimeAndNeverLeaveTheValidRange() {
		Needs needs = new Needs();

		needs.decay(18_000);
		assertEquals(0.0, needs.level(Need.FOOD), 1e-9);

		needs.restore(Need.FOOD, 0.4);
		assertEquals(0.4, needs.level(Need.FOOD), 1e-9);

		needs.restore(Need.FOOD, 5.0);
		assertEquals(1.0, needs.level(Need.FOOD), 1e-9);
	}

	@Test
	void personalityRejectsTraitsOutsideZeroToOne() {
		assertThrows(IllegalArgumentException.class, () -> new Personality(1.5, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> new Personality(0.0, -0.1, 0.0));
	}
}
