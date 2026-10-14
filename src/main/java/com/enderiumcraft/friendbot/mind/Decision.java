package com.enderiumcraft.friendbot.mind;

import java.util.Optional;

/**
 * What the companion decided to do, and any line it wants to say about it.
 *
 * @param intent the action to carry out
 * @param say    a line to say to a player, if any
 */
public record Decision(Intent intent, Optional<Utterance> say) {
}
