# Friendbot

A Fabric mod for Minecraft 26.1 that adds a companion who should feel like a real person to play with. Singleplayer first.

## Status

Foundation only. There is no companion in the world yet.

- Fabric 26.1 project scaffold, using the values from the official `fabric-example-mod` 26.1 branch.
- `mind/`: the companion's inner life. Needs that decay over time, a personality, memory and trust for each player, and a rule-based decision maker. It has no Minecraft dependency and is covered by tests.

## Build and run

Requires **JDK 25**.

```sh
./gradlew build      # compile, run tests, build the jar
./gradlew test       # just the tests
./gradlew runClient  # launch Minecraft with the mod for singleplayer testing
```

## Layout

- `FriendbotMod`: the Fabric entrypoint. The only class that touches Minecraft so far.
- `mind/`: the companion's brain. Nothing in here imports Minecraft.
  - `Mind`: the per-tick loop. Decides when to reconsider what it is doing, and when a decision is urgent enough to interrupt.
  - `Needs`: food, rest and company. Each runs down over time and is restored by actions.
  - `Acquaintance`: what the companion remembers about one player. Trust rises slowly and falls fast. Familiarity grows with time together and fades when apart.
  - `UtilityProvider`: the default rule-based chooser. It scores each `Intent` and picks the highest.
  - `DecisionProvider`: the seam for a future language model. Anything that maps a `Mind` and a `Percept` to a `Decision` can plug in here.

## Plan

1. **Companion in the world.** A spawnable entity that walks, looks at the player, and eats or rests visibly.
2. **Perception.** Build a `Percept` from the world each tick: nearby players, hostile mobs, time of day, inventory.
3. **Player interaction.** Give food, hit, and greet. Feed these into `Mind.recordEncounter`.
4. **Realism.** Idle fidgeting, reacting to weather, going to bed, remembering places, and mistakes that come from its own needs.
5. **Language.** An optional `DecisionProvider` backed by an LLM, so it can talk back in chat.
