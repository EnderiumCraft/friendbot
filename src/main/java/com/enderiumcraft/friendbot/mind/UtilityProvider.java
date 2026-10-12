package com.enderiumcraft.friendbot.mind;

import java.util.Optional;

/**
 * Scores every {@link Intent} from the companion's needs, personality and surroundings,
 * then picks the highest.
 *
 * <p>The ranges are tuned so that a threat always beats everything else, then trust and
 * loneliness, then hunger and tiredness, then idle wandering.
 */
public final class UtilityProvider implements DecisionProvider {
	@Override
	public Decision decide(Mind mind, Percept percept) {
		Intent best = Intent.IDLE;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (Intent intent : Intent.values()) {
			double score = score(intent, mind, percept);
			if (score > bestScore) {
				best = intent;
				bestScore = score;
			}
		}
		return new Decision(best, greeting(best, mind, percept));
	}

	private static double score(Intent intent, Mind mind, Percept percept) {
		Personality personality = mind.personality();
		Needs needs = mind.needs();
		return switch (intent) {
			case IDLE -> 0.1;
			case WANDER -> 0.15 + 0.35 * personality.curiosity();
			case EAT -> percept.hasFood() ? 0.9 * square(needs.urgency(Need.FOOD)) : 0.0;
			case REST -> 0.9 * square(needs.urgency(Need.REST)) * (percept.night() ? 1.1 : 1.0);
			case APPROACH_PLAYER -> approachScore(mind, percept);
			case KEEP_AWAY_FROM_PLAYER -> keepAwayScore(mind, percept);
			case FLEE -> percept.hostileNearby() ? 1.0 + 0.5 * personality.caution() + 0.5 * (1.0 - percept.health()) : 0.0;
		};
	}

	private static double approachScore(Mind mind, Percept percept) {
		if (percept.player().isEmpty() || percept.hostileNearby()) {
			return 0.0;
		}
		Percept.Sighting player = percept.player().get();
		Acquaintance acquaintance = mind.acquaintance(player.name());
		if (acquaintance.trust() < 0.3) {
			return 0.0;
		}
		double closeness = player.distance() > 3.0 ? 1.0 : 0.2;
		return needsCompany(mind) * mind.personality().sociability() * (0.5 + 0.5 * acquaintance.trust()) * closeness;
	}

	private static double keepAwayScore(Mind mind, Percept percept) {
		if (percept.player().isEmpty()) {
			return 0.0;
		}
		Acquaintance acquaintance = mind.acquaintance(percept.player().get().name());
		if (acquaintance.trust() >= 0.3) {
			return 0.0;
		}
		return (1.0 - acquaintance.trust()) * mind.personality().caution();
	}

	private static Optional<String> greeting(Intent intent, Mind mind, Percept percept) {
		if (intent == Intent.FLEE || intent == Intent.KEEP_AWAY_FROM_PLAYER || !mind.isNewEncounter(percept)) {
			return Optional.empty();
		}
		return percept.player().map(player -> "Hey " + player.name() + "!");
	}

	private static double needsCompany(Mind mind) {
		return mind.needs().urgency(Need.COMPANY);
	}

	private static double square(double value) {
		return value * value;
	}
}
