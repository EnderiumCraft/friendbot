package com.enderiumcraft.friendbot.mind;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The inner life of one companion: what it needs, who it knows, and what it is doing.
 *
 * <p>Nothing in this package touches Minecraft. The game side feeds in a {@link Percept} every
 * tick, carries out the returned {@link Decision}, and reports results with {@link #restore}
 * and {@link #recordEncounter}.
 */
public final class Mind {
	/** The companion keeps an intent for at least 3 seconds unless something urgent happens. */
	static final long MIN_HOLD_TICKS = 60;
	/** A player who has not been seen for a minute counts as a new encounter when they return. */
	static final long NEW_ENCOUNTER_TICKS = 1200;
	/** Resting refills REST from empty to full in about 5 minutes. */
	static final double REST_RECOVERY_TICKS = 6000;
	/** Time spent near a player refills COMPANY from empty to full in about 2 minutes. */
	static final double COMPANY_RECOVERY_TICKS = 2400;
	/** How close a player must be for the companion to feel their company, in blocks. */
	static final double NEARBY_PLAYER_DISTANCE = 6.0;

	private static final long NEVER = Long.MIN_VALUE;

	private final String name;
	private final Personality personality;
	private final DecisionProvider provider;
	private Needs needs = new Needs();
	private final Map<String, Acquaintance> acquaintances = new HashMap<>();

	private Intent intent = Intent.IDLE;
	private boolean decided = false;
	private long intentStartedTick;
	private long lastTick = NEVER;

	public Mind(String name, Personality personality, DecisionProvider provider) {
		this.name = name;
		this.personality = personality;
		this.provider = provider;
	}

	/** Captures everything that should survive a save and reload. */
	public MindSnapshot snapshot() {
		List<MindSnapshot.AcquaintanceSnapshot> known = acquaintances.values().stream()
				.sorted(Comparator.comparing(Acquaintance::name))
				.map(a -> new MindSnapshot.AcquaintanceSnapshot(a.name(), a.trust(), a.familiarity(), a.lastSeenTick()))
				.toList();
		return new MindSnapshot(decided, intent, intentStartedTick, lastTick, needs.snapshotLevels(), known);
	}

	/**
	 * Rebuilds a mind from a snapshot. The configuration (name, personality, provider) must be
	 * supplied again, because it is not stored.
	 */
	public static Mind restore(String name, Personality personality, DecisionProvider provider, MindSnapshot snapshot) {
		Mind mind = new Mind(name, personality, provider);
		mind.needs = new Needs(snapshot.needs());
		for (MindSnapshot.AcquaintanceSnapshot saved : snapshot.acquaintances()) {
			mind.acquaintances.put(saved.name(),
					new Acquaintance(saved.name(), saved.trust(), saved.familiarity(), saved.lastSeenTick()));
		}
		mind.intent = snapshot.intent();
		mind.decided = snapshot.decided();
		mind.intentStartedTick = snapshot.intentStartedTick();
		mind.lastTick = snapshot.lastTick();
		return mind;
	}

	/**
	 * Advances the mind by one observation.
	 *
	 * @return a new decision when the companion chose something, or empty when it is
	 *         carrying on with what it was already doing
	 */
	public Optional<Decision> think(Percept percept) {
		long elapsed = lastTick == NEVER ? 0 : Math.max(0, percept.tick() - lastTick);
		lastTick = percept.tick();
		needs.decay(elapsed);
		recoverNeeds(percept, elapsed);

		Optional<Decision> result = Optional.empty();
		if (isDue(percept)) {
			Decision decision = provider.decide(this, percept);
			intent = decision.intent();
			intentStartedTick = percept.tick();
			decided = true;
			result = Optional.of(decision);
		}

		updateSocial(percept, elapsed);
		return result;
	}

	/** Whether the nearby player is someone the companion has not seen for a while. */
	public boolean isNewEncounter(Percept percept) {
		return percept.player().map(sighting -> {
			Acquaintance known = acquaintances.get(sighting.name());
			return known == null || !known.seenWithin(percept.tick(), NEW_ENCOUNTER_TICKS);
		}).orElse(false);
	}

	/** Satisfies a need, for example after the companion has eaten. Clamped to full. */
	public void restore(Need need, double amount) {
		needs.restore(need, amount);
	}

	/** Records something a player did to or with the companion. */
	public void recordEncounter(String playerName, Encounter encounter) {
		acquaintance(playerName).apply(encounter);
	}

	public Acquaintance acquaintance(String playerName) {
		return acquaintances.computeIfAbsent(playerName, Acquaintance::new);
	}

	public String name() {
		return name;
	}

	public Personality personality() {
		return personality;
	}

	public Needs needs() {
		return needs;
	}

	public Intent intent() {
		return intent;
	}

	private boolean isDue(Percept percept) {
		if (!decided) {
			return true;
		}
		if (percept.hostileNearby() && intent != Intent.FLEE) {
			return true;
		}
		if (isNewEncounter(percept)) {
			return true;
		}
		return percept.tick() - intentStartedTick >= MIN_HOLD_TICKS;
	}

	/** Needs are refilled by what the companion is doing, so resting can end and company can help. */
	private void recoverNeeds(Percept percept, long elapsed) {
		if (intent == Intent.REST) {
			needs.restore(Need.REST, elapsed / REST_RECOVERY_TICKS);
		}
		boolean playerNearby = percept.player().filter(player -> player.distance() <= NEARBY_PLAYER_DISTANCE).isPresent();
		if (playerNearby) {
			needs.restore(Need.COMPANY, elapsed / COMPANY_RECOVERY_TICKS);
		}
	}

	private void updateSocial(Percept percept, long elapsed) {
		percept.player().ifPresent(sighting -> acquaintance(sighting.name()).seen(percept.tick(), elapsed));
		for (Acquaintance known : acquaintances.values()) {
			if (!known.lastSeenAt(percept.tick())) {
				known.fade(elapsed);
			}
		}
	}
}
