package com.enderiumcraft.friendbot.mind;

/**
 * Chooses what the companion does next. The rule-based {@link UtilityProvider} is the default.
 * A language model can implement this later without changing anything else.
 */
@FunctionalInterface
public interface DecisionProvider {
	Decision decide(Mind mind, Percept percept);
}
