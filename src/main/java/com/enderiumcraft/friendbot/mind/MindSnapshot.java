package com.enderiumcraft.friendbot.mind;

import java.util.List;
import java.util.Map;

/**
 * Everything about a companion's mind that should survive a save and reload, as plain data.
 * The name, personality and decision provider are configuration, so they are not included.
 *
 * @param decided           whether the mind has made its first decision yet
 * @param intent            the intent it is currently holding
 * @param intentStartedTick the tick that intent was chosen on
 * @param lastTick          the last tick the mind was advanced to
 * @param needs             each need's level, from 0 to 1
 * @param acquaintances     what it remembers about each player
 * @param pending           events it has not answered yet, oldest first. A null list is read as empty.
 */
public record MindSnapshot(
		boolean decided,
		Intent intent,
		long intentStartedTick,
		long lastTick,
		Map<Need, Double> needs,
		List<AcquaintanceSnapshot> acquaintances,
		List<PendingEncounter> pending) {

	public MindSnapshot {
		pending = pending == null ? List.of() : List.copyOf(pending);
	}

	/**
	 * @param name         the player's name
	 * @param trust        from 0 (hostile) to 1 (fully trusted)
	 * @param familiarity  from 0 (stranger) to 1 (knows them well)
	 * @param lastSeenTick the last tick the player was seen, or {@link Long#MIN_VALUE} if never
	 */
	public record AcquaintanceSnapshot(String name, double trust, double familiarity, long lastSeenTick) {
	}
}
