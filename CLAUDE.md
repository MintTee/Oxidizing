# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this mod is

"Oxidizing" (mod id `oxidizing`, Modrinth slug `oxidizing`): a server-side mod. Copper touching water oxidizes faster: on each oxidation roll, wet copper advances one stage with chance `wetSides * chancePerWetSide` (default 1/6, capped at 1). Wet sides are the 6 neighbours whose fluid is water (source, flowing, or waterlogged); a waterlogged copper block counts as 6. The idea comes from `../WayMuchFasterOxidize` (credited in the README), but the implementation is different.

`ChangeOverTimeBlockMixin` is an **interface** mixin on `ChangeOverTimeBlock`, injecting at HEAD of the default method holding vanilla's 0.0569 roll: `onRandomTick` up to 1.20.2, `changeOverTime` from 1.20.3. Both names are listed in one `@Inject` (only one exists per version, so `require = 1` is met without a `//?`). Every weathering block's `randomTick` calls it after its own guards (door lower half, unopened chest...), so one mixin covers every copper block on every version. The handler takes its arguments through MixinExtras `@Local(argsOnly = true)`: the `Random` parameter's type changed in 1.19, and an `@Inject` handler must take all of the target's arguments or none. The logic lives in `WetOxidation` (`wetSides`, `tryOxidize`). When wet, it cancels vanilla's roll and its Manhattan-distance-4 neighbour scan, and sets the next state with `getNext(state)` + `setBlockAndUpdate` (vanilla's own setter). When dry, it returns false and vanilla runs unchanged. Config: `config/oxidizing.json` (`config/ConfigIO`, Gson, loaded in `Main`). `assets/oxidizing/icon.png` is still Land Here's icon.

`../NEW_MOD_GUIDE.md` is the full recipe: Stonecutter nodes, published vs test-only nodes, byte-identity check, loaders, release.

## Multi-version / multi-loader setup (Stonecutter)

Nodes: Fabric 1.18 to 26.3 and NeoForge 1.21.1, 1.21.11, 26.1 to 26.3. All of them pass `runGameTest`.

- One source tree in `src/main` and `src/gametest`. Nodes are declared in `settings.gradle.kts`; per-node values live in `stonecutter.properties.toml` (`mod.publish`, `mod.mc_compat`, `mod.mc_releases`, `deps.*`). Every other release is a test-only node (`mod.publish = false`).
- **Published node rule:** a range may share one jar only if every node in it compiles byte-identical classes (split where Java changes, where a `//?` in `src/main` falls inside the range, and where bytecode changes without one). `src/main` has no `//?` besides `Main.java`'s loader switch, yet the bytecode still changes at 1.18.2 (`FluidTags.WATER` became a `TagKey`), 1.19 (`Level.getRandom()` returns `RandomSource`) and 1.20.3 (the mixin target name; Loom writes remapped mixin names into the `@Inject` annotation, and there's no refmap), plus the Java boundaries. Published Fabric jars: 1.18.1 (1.18–1.18.1), 1.18.2, 1.20.2 (1.19–1.20.2), 1.20.4 (1.20.3–1.20.4), 1.21.11 (1.20.5–1.21.11), 26.3 (26.1–26.3). NeoForge: 1.21.1, 1.21.11, 26.3 (26.1–26.3). Re-check whenever a `//?` is added to `src/main` or a version is added. Compare with `javap -v`, not `-c`: `-c` doesn't print annotations and misses the 1.20.3 split.
- Loader nodes: Fabric = `<mc>`, NeoForge = `<mc>-neoforge` with `build.neoforge.gradle.kts`; shared logic in `gradle/common.gradle.kts` (also holds the Modrinth `projectId` and the 10-minute GameTest timeout). Loader constants (`fabric`, `neoforge`) are available in `//?` conditionals.
- Mojang names everywhere. Version differences only via `//? if >=X {` Stonecutter comments, in a `Compat.java` where possible; never put `//` comments inside a branch. Run `./gradlew "Reset active project"` before committing.
- Metadata files are templated (`${version}`, `${minecraft}`, `${loader}`, `${java}`); only the node's own loader file ships in the jar.
- Fabric API is a test-only dependency; the mod needs only Fabric Loader.
- `settings.gradle.kts` pins ASM 9.10.1 on the settings classpath (fresh 26.x setups otherwise produce a Minecraft jar with no classes).

## GameTests

`src/gametest/java/oxidizing/test/OxidizingGameTest.java`, shared by both loaders. `TestMod.java` registers tests on NeoForge 1.21.5+: **add each new test method to its `functions` map**. Things that aren't obvious:
- Most tests call `WetOxidation` directly with a fixed roll (0 = always succeeds, 0.99 = fails for one wet side), so they're deterministic. `place()` sets all 6 neighbours explicitly (water or air), because the NeoForge empty structure is capped by barriers.
- `randomTickOxidizesWaterloggedCopper` is the only test going through vanilla's `randomTick`, so it's the one that proves the mixin fires (waterlogged = chance 1). It fails when the mixin is removed.
- Tests use only blocks that exist since 1.18 (copper block, cut copper slab/stairs).
- Helpers: `check` (`assertTrue`'s signature differs per version) and `newPlayer` (mock player, hand-built before 1.20; currently unused, kept from the template). `/*$ gametest*/ @GameTest` is a Stonecutter swap defined in `stonecutter.gradle.kts`.

## Commands

```sh
./gradlew :1.21.11:runGameTest             # one node's GameTests (headless)
./gradlew :1.21.1-neoforge:runGameTest     # a NeoForge node
./gradlew buildAndCollect                  # published jars in build/libs/<mod.version>/
./gradlew :26.3:runClient                  # dev client/server for any node, run dir run/<node>/
./gradlew "Set active project to 1.20.4"   # make src/ (and the IDE) show that version
./gradlew "Reset active project"           # back to the VCS version (1.21.11) before committing
```

Machine safety (16 GB machine): never run the all-nodes `runGameTest` casually. Run one node at a time under a memory cap, after checking `free -m` (keep at least 4 GB free):
```sh
systemd-run --user --scope -p MemoryMax=6G -p MemorySwapMax=0 nice -n 10 \
  ./gradlew :<node>:runGameTest --no-daemon -Dorg.gradle.jvmargs="-Xmx1500m" --console=plain
```

## Releasing

Bump `mod.version` once in `stonecutter.properties.toml`, rewrite `CHANGELOG.md` for this release only, dry run `MODRINTH_TOKEN=dummy ./gradlew modrinth -PmodrinthDryRun`, commit, push, then `git tag vX.Y.Z && git push origin vX.Y.Z`. The tag triggers `.github/workflows/release.yml`: GameTests, then the Modrinth upload (secret `MODRINTH_TOKEN`), then a GitHub Release with the jars. No git remote is configured yet (the siblings use `git@github.com:MintTee/<Repo>.git`).
