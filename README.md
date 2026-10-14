# Friendbot

A Fabric mod for Minecraft 26.1 that adds a companion who should feel like a real person to play with. Singleplayer first.

## Status

The companion exists as a Minecraft entity, but it can't be spawned yet. It has no renderer and no spawn command, and a spawned entity without a renderer would crash the client.

- Fabric 26.1 project scaffold, using the values from the official `fabric-example-mod` 26.1 branch.
- `mind/`: the companion's inner life. Needs that decay over time and recover while acted on, a personality, memory and trust for each player, and a rule-based decision maker. It answers what players do (a hit, a gift, a hello) one event at a time, and greets people differently depending on how well it knows them. It can be snapshotted and restored, including events it has not answered yet. Corrupt saved values fall back to defaults. It has no Minecraft dependency and is covered by tests. Nothing in the running mod reports hits, gifts or hellos yet, so these replies only happen once that is wired up.
- `body/`: turns the chosen intent into a walk target. It has no Minecraft dependency and is covered by tests.
- `entity/`: the Minecraft side. It registers the entity type and default attributes, senses the world each tick, and applies the mind's decisions. **Not yet verified against the 26.1 API.** The first local build is the real check.

## Build and run

Requires **JDK 25**.

```sh
./gradlew build      # compile, run tests, build the jar
./gradlew test       # just the tests
./gradlew runClient  # launch Minecraft with the mod for singleplayer testing
```

## Layout

- `FriendbotMod`: the Fabric entrypoint. Registers the entity and exposes `id()`.
- `entity/`: the Minecraft side.
  - `CompanionEntity`: a `PathfinderMob` that senses the world each tick and carries out its mind's decisions.
  - `ModEntities`: registers the entity type and its default attributes.
- `mind/`: the companion's brain. Nothing in here imports Minecraft.
  - `Mind`: the per-tick loop. Decides when to reconsider what it is doing, and when a decision is urgent enough to interrupt.
  - `Needs`: food, rest and company. Each runs down over time and is restored by actions.
  - `Acquaintance`: what the companion remembers about one player. Trust rises slowly and falls fast. Familiarity grows with time together and fades when apart.
  - `UtilityProvider`: the default rule-based chooser. It scores each `Intent` and picks the highest.
  - `DecisionProvider`: the seam for a future language model. Anything that maps a `Mind` and a `Percept` to a `Decision` can plug in here.
  - `MindSnapshot`: everything about a mind that should survive a save and reload, as plain data. `Mind.snapshot()` and `Mind.restore(...)` convert to and from it. Nothing writes it to a world file yet; that waits on the 26.1 save API.
  - `MindCodec`: turns a snapshot into a text block and back, so the whole memory fits in one string. Unknown or malformed lines are skipped. Only a missing header makes the text unreadable.
  - `PendingEncounter`: an event the companion has not answered yet: a player arriving, hitting, giving food, or greeting. Up to four wait at once, and the oldest is answered first.
  - `Utterance`: a line the companion says, and the player it is for. Lines only go to that player.
- `body/`: turns an `Intent` into a walk `Goal` (target and speed) from where the companion stands. `Steering` is pure geometry.

## Plan

1. **Companion in the world.** A spawnable entity that walks, looks at the player, and eats or rests visibly.
2. **Perception.** Build a `Percept` from the world each tick: nearby players, hostile mobs, time of day, inventory.
3. **Player interaction.** Give food, hit, and greet. Feed these into `Mind.recordEncounter`.
4. **Realism.** Idle fidgeting, reacting to weather, going to bed, remembering places, and mistakes that come from its own needs.
5. **Language.** An optional `DecisionProvider` backed by an LLM, so it can talk back in chat.
