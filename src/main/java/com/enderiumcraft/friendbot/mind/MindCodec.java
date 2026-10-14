package com.enderiumcraft.friendbot.mind;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a {@link MindSnapshot} into text and back, so a companion's memory can be stored as one
 * string in the world save.
 *
 * <p>The text starts with a header line and then has one field per line, such as
 * {@code need FOOD 0.5}. Unknown and malformed lines are skipped and missing fields take their
 * defaults. That way a damaged save, or one written by a newer version, loads as much as it can
 * instead of failing outright. Only a missing or unknown header makes the whole text unreadable.
 */
public final class MindCodec {
	static final String HEADER = "friendbot-mind 1";

	/** Matches the mind's own "never thought" sentinel for the last tick. */
	private static final long NEVER = Long.MIN_VALUE;

	private MindCodec() {
	}

	public static String encode(MindSnapshot snapshot) {
		StringBuilder out = new StringBuilder(HEADER).append('\n');
		line(out, "decided", Boolean.toString(snapshot.decided()));
		line(out, "intent", snapshot.intent().name());
		line(out, "intentStartedTick", Long.toString(snapshot.intentStartedTick()));
		line(out, "lastTick", Long.toString(snapshot.lastTick()));
		for (Need need : Need.values()) {
			if (snapshot.needs().containsKey(need)) {
				line(out, "need", need.name(), Double.toString(snapshot.needs().get(need)));
			}
		}
		for (MindSnapshot.AcquaintanceSnapshot known : snapshot.acquaintances()) {
			line(out, "acquaintance", encodeName(known.name()), Double.toString(known.trust()),
					Double.toString(known.familiarity()), Long.toString(known.lastSeenTick()));
		}
		for (PendingEncounter event : snapshot.pending()) {
			line(out, "pending", encodeName(event.playerName()), event.encounter().name());
		}
		return out.toString();
	}

	/**
	 * @return the snapshot, or empty if the text does not start with a header this version understands
	 */
	public static Optional<MindSnapshot> decode(String text) {
		if (text == null) {
			return Optional.empty();
		}
		List<String> lines = text.lines().toList();
		if (lines.isEmpty() || !lines.get(0).strip().equals(HEADER)) {
			return Optional.empty();
		}

		boolean decided = false;
		Intent intent = Intent.IDLE;
		long intentStartedTick = 0;
		long lastTick = NEVER;
		Map<Need, Double> needs = new EnumMap<>(Need.class);
		Map<String, MindSnapshot.AcquaintanceSnapshot> known = new LinkedHashMap<>();
		List<PendingEncounter> pending = new ArrayList<>();

		for (String raw : lines.subList(1, lines.size())) {
			String[] fields = raw.strip().split(" ");
			try {
				switch (fields[0]) {
					case "decided" -> decided = Boolean.parseBoolean(fields[1]);
					case "intent" -> intent = Intent.valueOf(fields[1]);
					case "intentStartedTick" -> intentStartedTick = Long.parseLong(fields[1]);
					case "lastTick" -> lastTick = Long.parseLong(fields[1]);
					case "need" -> needs.put(Need.valueOf(fields[1]), Double.parseDouble(fields[2]));
					case "acquaintance" -> {
						MindSnapshot.AcquaintanceSnapshot entry = new MindSnapshot.AcquaintanceSnapshot(
								decodeName(fields[1]), Double.parseDouble(fields[2]),
								Double.parseDouble(fields[3]), Long.parseLong(fields[4]));
						known.put(entry.name(), entry); // a name listed twice keeps its last entry
					}
					case "pending" -> pending.add(new PendingEncounter(
							decodeName(fields[1]), Encounter.valueOf(fields[2])));
					default -> {
						// Unknown field from a newer version: skip it.
					}
				}
			} catch (RuntimeException malformed) {
				// A line that does not parse is skipped. Its field keeps the default value.
			}
		}
		return Optional.of(new MindSnapshot(decided, intent, intentStartedTick, lastTick, needs,
				List.copyOf(known.values()), pending));
	}

	private static void line(StringBuilder out, String... fields) {
		out.append(String.join(" ", fields)).append('\n');
	}

	/** Names are escaped so that spaces and line breaks cannot split a field. */
	private static String encodeName(String name) {
		return URLEncoder.encode(name, StandardCharsets.UTF_8);
	}

	private static String decodeName(String encoded) {
		return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
	}
}
