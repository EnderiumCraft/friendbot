package com.enderiumcraft.friendbot.mind;

import java.util.List;
import java.util.Optional;

/**
 * Scores every {@link Intent} from the companion's needs, personality and surroundings,
 * then picks the highest. It also answers the oldest event waiting for a reply.
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
		return new Decision(best, reply(best, mind));
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

	/**
	 * Answers the oldest event waiting for a reply. A player who has just come into view is only
	 * greeted if the companion is not busy fleeing or keeping away from someone.
	 */
	private static Optional<Utterance> reply(Intent chosen, Mind mind) {
		List<PendingEncounter> waiting = mind.pendingEncounters();
		if (waiting.isEmpty()) {
			return Optional.empty();
		}
		PendingEncounter event = waiting.get(0);
		String name = event.playerName();
		Optional<String> line = switch (event.encounter()) {
			case ARRIVED -> arrivalGreeting(chosen, name, mind);
			case GREETED -> Optional.of("Hi, " + name + "!");
			case GAVE_FOOD -> Optional.of("Thanks, " + name + "!");
			case HURT_ME -> Optional.of(mind.acquaintance(name).trust() >= 0.3
					? "Ow! Hey, what was that for?"
					: "Stop it, " + name + "!");
		};
		return line.map(text -> new Utterance(name, text));
	}

	/** Strangers get an introduction; people the companion knows get a warmer hello. */
	private static Optional<String> arrivalGreeting(Intent chosen, String name, Mind mind) {
		if (chosen == Intent.FLEE || chosen == Intent.KEEP_AWAY_FROM_PLAYER) {
			return Optional.empty();
		}
		double familiarity = mind.acquaintance(name).familiarity();
		if (familiarity < 0.2) {
			return Optional.of("Hi, I'm " + mind.name() + ".");
		}
		if (familiarity < 0.6) {
			return Optional.of("Hey, " + name + "!");
		}
		return Optional.of("Hey " + name + ", good to see you!");
	}

	private static double needsCompany(Mind mind) {
		return mind.needs().urgency(Need.COMPANY);
	}

	private static double square(double value) {
		return value * value;
	}
}
