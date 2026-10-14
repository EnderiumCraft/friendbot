package com.enderiumcraft.friendbot.mind;

/**
 * Chooses what the companion does next. The rule-based {@link UtilityProvider} is the default.
 * A language model can implement this later without changing anything else.
 *
 * <p>Events the companion has not answered yet are waiting in {@link Mind#pendingEncounters()},
 * oldest first. The mind removes the oldest one after each decision, whether or not the provider
 * used it, so a provider that wants to answer every event should look at that first entry.
 */
@FunctionalInterface
public interface DecisionProvider {
	Decision decide(Mind mind, Percept percept);
}
