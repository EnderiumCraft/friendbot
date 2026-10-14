package com.enderiumcraft.friendbot.mind;

/**
 * A line the companion says to one player.
 *
 * @param listener the player the line is for, so the game side can send it to them and no one else
 * @param text     what the companion says
 */
public record Utterance(String listener, String text) {
}
