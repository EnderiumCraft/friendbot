package com.enderiumcraft.friendbot.mind;

/**
 * Something a player just did that the companion has not reacted to yet. Only the latest one is
 * kept: if two things happen before the companion looks up, it reacts to the most recent.
 *
 * @param playerName the player involved
 * @param encounter  what they did
 */
public record PendingEncounter(String playerName, Encounter encounter) {
}
