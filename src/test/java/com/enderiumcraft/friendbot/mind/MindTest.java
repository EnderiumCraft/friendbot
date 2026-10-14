package com.enderiumcraft.friendbot.mind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MindTest {
	private static final Personality NEUTRAL = new Personality(0.5, 0.5, 0.5);
	private static final Percept.Sighting SAM_NEAR = new Percept.Sighting("Sam", 6.0);
	private static final Percept.Sighting BOB_NEAR = new Percept.Sighting("Bob", 6.0);

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

		assertEquals(new Utterance("Sam", "Hi, I'm Alex."), greeting.say().orElseThrow(), "a stranger gets an introduction");
	}

	@Test
	void acquaintanceOfMiddlingFamiliarityGetsASimpleHello() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		for (long tick = 0; tick <= 6_000; tick++) {
			mind.think(withPlayer(tick, SAM_NEAR));
		}
		for (long tick = 6_001; tick <= 8_000; tick++) {
			mind.think(calm(tick, false));
		}

		Decision hello = mind.think(withPlayer(8_001, SAM_NEAR)).orElseThrow();

		assertEquals("Hey, Sam!", hello.say().orElseThrow().text());
	}

	@Test
	void familiarPlayerIsGreetedWarmlyAfterBeingAway() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		for (long tick = 0; tick <= 12_000; tick++) {
			mind.think(withPlayer(tick, SAM_NEAR));
		}
		for (long tick = 12_001; tick <= 14_000; tick++) {
			mind.think(calm(tick, false));
		}

		Decision welcome = mind.think(withPlayer(14_001, SAM_NEAR)).orElseThrow();

		assertEquals("Hey Sam, good to see you!", welcome.say().orElseThrow().text());
	}

	@Test
	void hurtCompanionSpeaksUpWhileStillHoldingItsIntent() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(withPlayer(0, SAM_NEAR));

		mind.recordEncounter("Sam", Encounter.HURT_ME);
		Decision reaction = mind.think(withPlayer(1, SAM_NEAR)).orElseThrow();

		assertEquals(new Utterance("Sam", "Stop it, Sam!"), reaction.say().orElseThrow(),
				"trust has fallen below 0.3 after one hit");
		assertTrue(mind.pendingEncounters().isEmpty(), "a reaction is spoken once");
	}

	@Test
	void pendingEncounterForcesADecisionInsideTheHold() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(withPlayer(0, SAM_NEAR));

		mind.recordEncounter("Sam", Encounter.GAVE_FOOD);
		Decision thanks = mind.think(calm(5, false)).orElseThrow();

		assertEquals("Thanks, Sam!", thanks.say().orElseThrow().text());
	}

	@Test
	void helloIsAnsweredWithAHi() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));

		mind.recordEncounter("Sam", Encounter.GREETED);
		Decision hi = mind.think(calm(5, false)).orElseThrow();

		assertEquals("Hi, Sam!", hi.say().orElseThrow().text());
	}

	@Test
	void repeatedKindnessThenAHitGetsAnOuchNotAStop() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));
		mind.recordEncounter("Sam", Encounter.GAVE_FOOD);
		mind.think(calm(1, false));
		mind.recordEncounter("Sam", Encounter.GAVE_FOOD);
		mind.think(calm(2, false));

		mind.recordEncounter("Sam", Encounter.HURT_ME);
		Decision ow = mind.think(calm(3, false)).orElseThrow();

		assertEquals("Ow! Hey, what was that for?", ow.say().orElseThrow().text(), "trust is still above 0.3");
	}

	@Test
	void eventsAreAnsweredOneAtATimeInOrder() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));
		mind.recordEncounter("Sam", Encounter.HURT_ME);
		mind.recordEncounter("Bob", Encounter.GREETED);

		Decision first = mind.think(calm(5, false)).orElseThrow();
		assertEquals(new Utterance("Sam", "Stop it, Sam!"), first.say().orElseThrow());
		assertEquals(1, mind.pendingEncounters().size());

		Decision second = mind.think(calm(6, false)).orElseThrow();
		assertEquals(new Utterance("Bob", "Hi, Bob!"), second.say().orElseThrow());
		assertTrue(mind.pendingEncounters().isEmpty());
	}

	@Test
	void anArrivalIsNotLostBehindAHit() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));
		mind.recordEncounter("Sam", Encounter.HURT_ME);

		Decision hit = mind.think(withPlayer(5, BOB_NEAR)).orElseThrow();
		Decision intro = mind.think(withPlayer(6, BOB_NEAR)).orElseThrow();

		assertEquals("Stop it, Sam!", hit.say().orElseThrow().text());
		assertEquals(new Utterance("Bob", "Hi, I'm Alex."), intro.say().orElseThrow());
	}

	@Test
	void arrivalIsNotGreetedWhileFleeing() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));

		Percept scared = new Percept(5, Optional.of(BOB_NEAR), true, 1.0, false, false);
		Decision flee = mind.think(scared).orElseThrow();

		assertEquals(Intent.FLEE, flee.intent());
		assertTrue(flee.say().isEmpty(), "a companion running from a mob does not stop to say hello");
		assertTrue(mind.pendingEncounters().isEmpty(), "the arrival is consumed even when unanswered");
	}

	@Test
	void waitingEventsAreCapped() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		for (int i = 0; i < Mind.MAX_PENDING + 3; i++) {
			mind.recordEncounter("Sam" + i, Encounter.GREETED);
		}

		List<PendingEncounter> waiting = mind.pendingEncounters();
		assertEquals(Mind.MAX_PENDING, waiting.size());
		assertEquals("Sam3", waiting.get(0).playerName(), "the oldest events are the ones dropped");
	}

	@Test
	void pendingEncounterSurvivesSaveAndReload() {
		Mind original = new Mind("Alex", NEUTRAL, new UtilityProvider());
		original.think(calm(0, false));
		original.recordEncounter("Sam", Encounter.GREETED);

		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), original.snapshot());

		assertEquals(original.pendingEncounters(), restored.pendingEncounters());
		assertEquals("Hi, Sam!", restored.think(calm(5, false)).orElseThrow().say().orElseThrow().text());
	}

	@Test
	void snapshotWithNoPendingListRestoresCleanly() {
		MindSnapshot saved = new MindSnapshot(true, Intent.IDLE, 0, 0, Map.of(), List.of(), null);
		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), saved);

		assertTrue(restored.pendingEncounters().isEmpty());
		assertTrue(restored.think(calm(60, false)).isPresent());
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
	void liveRestoreIgnoresANaNAmount() {
		Needs needs = new Needs();
		needs.decay(9_000);
		double before = needs.level(Need.FOOD);

		needs.restore(Need.FOOD, Double.NaN);

		assertEquals(before, needs.level(Need.FOOD), 1e-12, "a NaN must not poison the need for the rest of the session");
	}

	@Test
	void companionDoesNotGetStuckRestingWhenAloneForAnHour() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		long hour = 72_000;
		long restingTicks = 0;

		for (long tick = 0; tick <= hour; tick++) {
			mind.think(calm(tick, false));
			if (mind.intent() == Intent.REST) {
				restingTicks++;
			}
		}

		assertTrue(restingTicks < hour / 2, "rested " + restingTicks + " of " + hour + " ticks");
	}

	@Test
	void timeNearAPlayerRefillsCompany() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		for (long tick = 0; tick <= 9_000; tick++) {
			mind.think(calm(tick, false));
		}
		assertTrue(mind.needs().urgency(Need.COMPANY) > 0.5, "lonely before anyone visits");

		Percept.Sighting nearby = new Percept.Sighting("Sam", 3.0);
		for (long tick = 9_001; tick <= 11_400; tick++) {
			mind.think(withPlayer(tick, nearby));
		}

		assertTrue(mind.needs().urgency(Need.COMPANY) < 0.05, "still lonely: " + mind.needs().urgency(Need.COMPANY));
	}

	@Test
	void restoredMindContinuesExactlyWhereTheOriginalLeftOff() {
		Mind original = new Mind("Alex", NEUTRAL, new UtilityProvider());
		for (long tick = 0; tick <= 3_000; tick++) {
			original.think(tick % 500 < 250 ? withPlayer(tick, SAM_NEAR) : calm(tick, false));
		}
		original.recordEncounter("Sam", Encounter.GAVE_FOOD);

		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), original.snapshot());
		assertEquals(original.snapshot(), restored.snapshot(), "restore must reproduce every saved field");

		for (long tick = 3_001; tick <= 6_000; tick++) {
			Percept percept = tick % 700 < 300 ? withPlayer(tick, SAM_NEAR) : calm(tick, false);
			assertEquals(original.think(percept), restored.think(percept), "decision at tick " + tick);
			assertEquals(original.intent(), restored.intent(), "intent at tick " + tick);
		}
		assertEquals(original.needs().level(Need.COMPANY), restored.needs().level(Need.COMPANY), 1e-12);
		assertEquals(original.acquaintance("Sam").trust(), restored.acquaintance("Sam").trust(), 1e-12);
		assertEquals(original.acquaintance("Sam").familiarity(), restored.acquaintance("Sam").familiarity(), 1e-12);
	}

	@Test
	void snapshotOfAFreshMindRestoresAsAFreshMind() {
		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(),
				new Mind("Alex", NEUTRAL, new UtilityProvider()).snapshot());

		Optional<Decision> first = restored.think(calm(10_000, false));

		assertTrue(first.isPresent(), "a fresh mind must decide on its first think");
		assertEquals(1.0, restored.needs().level(Need.FOOD), 1e-12, "no time has passed for a fresh mind");
	}

	@Test
	void restoredMindWhoseClockWentBackwardsStillDecides() {
		Mind saved = new Mind("Alex", NEUTRAL, new UtilityProvider());
		saved.think(calm(5_000, false));

		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), saved.snapshot());

		assertTrue(restored.think(calm(0, false)).isPresent(), "a saved tick in the future must not freeze the intent");
	}

	@Test
	void playerSeenInTheFutureIsGreetedAgainAfterTheClockGoesBack() {
		Mind saved = new Mind("Alex", NEUTRAL, new UtilityProvider());
		saved.think(withPlayer(5_000, SAM_NEAR));

		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), saved.snapshot());
		Decision greeting = restored.think(withPlayer(0, SAM_NEAR)).orElseThrow();

		assertTrue(greeting.say().isPresent(), "a last sighting in the future must not count as recent");
	}

	@Test
	void corruptSavedValuesFallBackToDefaults() {
		Needs needs = new Needs(Map.of(Need.FOOD, Double.NaN));
		assertEquals(1.0, needs.level(Need.FOOD), 1e-12);

		MindSnapshot corrupt = new MindSnapshot(true, Intent.IDLE, 0, 0, Map.of(),
				List.of(new MindSnapshot.AcquaintanceSnapshot("Sam", Double.NaN, Double.NaN, 0)),
				List.of());
		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), corrupt);

		assertEquals(0.5, restored.acquaintance("Sam").trust(), 1e-12);
		assertEquals(0.0, restored.acquaintance("Sam").familiarity(), 1e-12);
	}

	@Test
	void unknownHealthStillLetsTheCompanionFleeFromAThreat() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));

		Percept threat = new Percept(5, Optional.empty(), true, Double.NaN, false, false);
		Decision flee = mind.think(threat).orElseThrow();

		assertEquals(Intent.FLEE, flee.intent());
	}

	@Test
	void sightingRequiresAName() {
		assertThrows(NullPointerException.class, () -> new Percept.Sighting(null, 3.0));
	}

	@Test
	void encounterRequiresAName() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());

		assertThrows(NullPointerException.class, () -> mind.recordEncounter(null, Encounter.GREETED));
	}

	@Test
	void snapshotIsAFrozenCopy() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0, false));
		mind.think(calm(9_000, false));
		MindSnapshot before = mind.snapshot();

		mind.think(calm(18_000, false));

		assertEquals(0.5, before.needs().get(Need.FOOD), 1e-9, "the saved copy must not see later changes");
		assertEquals(0.0, mind.needs().level(Need.FOOD), 1e-9, "the live mind did keep getting hungrier");
	}

	@Test
	void needsMissingFromASnapshotStartFull() {
		Needs needs = new Needs(Map.of(Need.REST, 0.2));

		assertEquals(0.2, needs.level(Need.REST), 1e-12);
		assertEquals(1.0, needs.level(Need.FOOD), 1e-12);
		assertEquals(1.0, needs.level(Need.COMPANY), 1e-12);
	}

	@Test
	void personalityRejectsTraitsOutsideZeroToOne() {
		assertThrows(IllegalArgumentException.class, () -> new Personality(1.5, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> new Personality(0.0, -0.1, 0.0));
	}
}
