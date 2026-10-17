# Friendbot

A Fabric mod for Minecraft 26.1 that adds a companion who should feel like a real person to play with. Singleplayer first.

## Status

Feature complete for a first playable version, but the Minecraft-facing code has never been compiled against 26.1. Expect a short round of fixes from the first local build.

- **Play:** run the game, then `/friendbot spawn`. The companion appears as a person and wanders, rests, follows you when it is lonely, and runs from hostile mobs.
- **Talk and interact:** right-click with an empty hand to say hello, or with food to give it something to eat. Hit it and it remembers, keeps its distance, and trusts you less.
- **Memory:** it remembers each player's trust and familiarity, its needs, and its food, across saves.
- Fabric 26.1 project scaffold, using the values from the official `fabric-example-mod` 26.1 branch.
- `mind/`, `body/`: the companion's brain and movement, with no Minecraft dependency and unit tests (54).
- `entity/`, `command/`, `client/`: the Minecraft side. **Unverified against the 26.1 API.**

## Build and run

Requires **JDK 25**.

```sh
./gradlew build      # compile, run tests, build the jar
./gradlew test       # just the tests
./gradlew runClient  # launch Minecraft with the mod for singleplayer testing
```

## Layout

- `FriendbotMod`: the Fabric entrypoint. Registers the entity and the command, and exposes `id()`.
- `entity/`: the Minecraft side.
  - `CompanionEntity`: a `PathfinderMob` that senses the world each tick and carries out its mind's decisions. Handles hits, gifts and hellos, and saves and loads its memory.
  - `ModEntities`: registers the entity type and its default attributes.
- `command/`: `CompanionCommands` adds `/friendbot spawn`.
- `client/`: `CompanionRenderer` draws the companion with the player model and Steve skin. `FriendbotClient` registers it.
- `mind/`: the companion's brain. Nothing in here imports Minecraft.
  - `Mind`: the per-tick loop. Decides when to reconsider what it is doing, and when a decision is urgent enough to interrupt.
  - `Needs`: food, rest and company. Each runs down over time and is restored by actions.
  - `Acquaintance`: what the companion remembers about one player. Trust rises slowly and falls fast. Familiarity grows with time together and fades when apart.
  - `UtilityProvider`: the default rule-based chooser. It scores each `Intent` and picks the highest.
  - `DecisionProvider`: the seam for a future language model. Anything that maps a `Mind` and a `Percept` to a `Decision` can plug in here.
  - `MindSnapshot`: everything about a mind that should survive a save and reload, as plain data. `Mind.snapshot()` and `Mind.restore(...)` convert to and from it. `CompanionEntity` stores the encoded text in the world save.
  - `MindCodec`: turns a snapshot into a text block and back, so the whole memory fits in one string. Unknown or malformed lines are skipped. Only a missing header makes the text unreadable.
  - `PendingEncounter`: an event the companion has not answered yet: a player arriving, hitting, giving food, or greeting. Up to four wait at once, and the oldest is answered first.
  - `Utterance`: a line the companion says, and the player it is for. Lines only go to that player.
- `body/`: turns an `Intent` into a walk `Goal` (target and speed) from where the companion stands. `Steering` is pure geometry.

## Plan

Done: the companion exists in the world (`/friendbot spawn`), senses players, mobs and food, and responds to hits, gifts and hellos. It remembers across saves.

Next:

1. **Compile and fix.** Build against 26.1 and correct any API names that differ from what the code assumes. The likeliest are the renderer classes, the save methods, `hurtServer`, and `Level.isNight()`.
2. **Realism.** Idle fidgeting, reacting to weather, going to bed, remembering places, and mistakes that come from its own needs.
3. **Look.** Armor, held items, and a skin of your choice.
4. **Language.** An optional `DecisionProvider` backed by an LLM, so it can talk back in chat.
