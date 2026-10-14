package com.enderiumcraft.friendbot.mind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MindCodecTest {
	private static final Personality NEUTRAL = new Personality(0.5, 0.5, 0.5);

	private static Percept calm(long tick) {
		return new Percept(tick, Optional.empty(), false, 1.0, false, false);
	}

	private static Percept withPlayer(long tick, Percept.Sighting player) {
		return new Percept(tick, Optional.of(player), false, 1.0, false, false);
	}

	@Test
	void roundTripReproducesTheSnapshotExactly() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		Percept.Sighting odd = new Percept.Sighting("Sam Smith+1%", 4.0);
		for (long tick = 0; tick <= 2_000; tick++) {
			mind.think(tick % 400 < 200 ? withPlayer(tick, odd) : calm(tick));
		}
		mind.recordEncounter("Sam Smith+1%", Encounter.GAVE_FOOD);
		mind.recordEncounter("Bob", Encounter.HURT_ME);
		MindSnapshot before = mind.snapshot();

		Optional<MindSnapshot> after = MindCodec.decode(MindCodec.encode(before));

		assertEquals(Optional.of(before), after);
	}

	@Test
	void decodedSaveContinuesLikeTheOriginal() {
		Mind original = new Mind("Alex", NEUTRAL, new UtilityProvider());
		for (long tick = 0; tick <= 3_000; tick++) {
			original.think(tick % 500 < 250 ? withPlayer(tick, new Percept.Sighting("Sam", 3.0)) : calm(tick));
		}
		original.recordEncounter("Sam", Encounter.HURT_ME);

		String saved = MindCodec.encode(original.snapshot());
		Mind restored = Mind.restore("Alex", NEUTRAL, new UtilityProvider(), MindCodec.decode(saved).orElseThrow());

		for (long tick = 3_001; tick <= 6_000; tick++) {
			Percept percept = tick % 700 < 300 ? withPlayer(tick, new Percept.Sighting("Sam", 3.0)) : calm(tick);
			assertEquals(original.think(percept), restored.think(percept), "decision at tick " + tick);
		}
	}

	@Test
	void unknownAndMalformedLinesAreSkipped() {
		String text = MindCodec.HEADER + "\n"
				+ "lastTick 500\n"
				+ "from a newer version 42\n"
				+ "need FOOD not-a-number\n"
				+ "intent FLYING\n"
				+ "pending Sam NOT_AN_EVENT\n"
				+ "acquaintance Sam\n"
				+ "intentStartedTick 480\n";

		MindSnapshot snapshot = MindCodec.decode(text).orElseThrow();

		assertEquals(500, snapshot.lastTick());
		assertEquals(480, snapshot.intentStartedTick());
		assertEquals(Intent.IDLE, snapshot.intent());
		assertTrue(snapshot.needs().isEmpty(), "a malformed need is dropped, so it starts full");
		assertTrue(snapshot.pending().isEmpty());
		assertTrue(snapshot.acquaintances().isEmpty());
	}

	@Test
	void missingFieldsTakeTheirDefaults() {
		MindSnapshot snapshot = MindCodec.decode(MindCodec.HEADER + "\n").orElseThrow();

		assertEquals(false, snapshot.decided());
		assertEquals(Intent.IDLE, snapshot.intent());
		assertEquals(0L, snapshot.intentStartedTick());
		assertEquals(Long.MIN_VALUE, snapshot.lastTick(), "a save with no clock reading has never thought");
		assertTrue(snapshot.needs().isEmpty(), "missing needs start full when the mind is restored");
		assertTrue(snapshot.acquaintances().isEmpty());
		assertTrue(snapshot.pending().isEmpty());
	}

	@Test
	void textWithoutAKnownHeaderIsRejected() {
		assertTrue(MindCodec.decode("").isEmpty());
		assertTrue(MindCodec.decode(null).isEmpty());
		assertTrue(MindCodec.decode("lastTick 5\n").isEmpty());
		assertTrue(MindCodec.decode("friendbot-mind 2\nlastTick 5\n").isEmpty(), "a version this code does not know");
	}

	@Test
	void needsAtZeroAreSavedSoAStarvingCompanionDoesNotReloadFed() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.think(calm(0));
		mind.think(calm(30_000));
		assertEquals(0.0, mind.needs().level(Need.FOOD), 1e-12);

		MindSnapshot reloaded = MindCodec.decode(MindCodec.encode(mind.snapshot())).orElseThrow();

		assertEquals(0.0, reloaded.needs().get(Need.FOOD), 1e-12);
	}

	@Test
	void tickValuesBeyondTheRangeOfAnIntAreReadExactly() {
		String text = MindCodec.HEADER + "\nlastTick 3000000000\nintentStartedTick 3000000000\n";

		MindSnapshot snapshot = MindCodec.decode(text).orElseThrow();

		assertEquals(3_000_000_000L, snapshot.lastTick());
		assertEquals(3_000_000_000L, snapshot.intentStartedTick());
	}

	@Test
	void namesWithLineBreaksAndNonAsciiCharactersSurviveASave() {
		Mind mind = new Mind("Alex", NEUTRAL, new UtilityProvider());
		mind.recordEncounter("line\nbreak", Encounter.GREETED);
		mind.recordEncounter("carriage\rreturn", Encounter.GREETED);
		mind.recordEncounter("Zoë", Encounter.GREETED);
		MindSnapshot before = mind.snapshot();

		assertEquals(Optional.of(before), MindCodec.decode(MindCodec.encode(before)));
	}

	@Test
	void headerWithTrailingSpacesOrWindowsLineEndingsStillLoads() {
		assertTrue(MindCodec.decode(MindCodec.HEADER + "   \nlastTick 9\n").isPresent());
		assertEquals(9L, MindCodec.decode(MindCodec.HEADER + "\r\nlastTick 9\r\n").orElseThrow().lastTick());
	}

	@Test
	void aRepeatedPlayerNameKeepsTheLastEntry() {
		String text = MindCodec.HEADER + "\n"
				+ "acquaintance Sam 0.2 0.1 100\n"
				+ "acquaintance Sam 0.9 0.8 200\n";

		MindSnapshot snapshot = MindCodec.decode(text).orElseThrow();

		assertEquals(1, snapshot.acquaintances().size());
		assertEquals(0.9, snapshot.acquaintances().get(0).trust(), 1e-12);
	}
}
