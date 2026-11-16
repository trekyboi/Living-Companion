# Living Companion — Minecraft Forge 1.20.1

A lightweight knight companion mod for Minecraft Java 1.20.1 and Forge 47.x.
The companion uses Minecraft's **vanilla player model**, a custom 64×64 knight
skin, and the vanilla humanoid movement pipeline. With Better Combat and Better
Mob Combat installed, the knight uses weapon-aware attack animations through
Player Animator.

## Target environment

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.x (development pinned to 47.4.0)
- Java 17
- ForgeGradle 6 / Gradle wrapper 8.1.1
- Optional combat stack: Better Combat 1.8.6 or 1.9.x, Better Mob Combat 1.3.0,
  Player Animator, Mob Player Animator, Cloth Config API, and BMCFIX 1.0.2 for Better Combat 1.9.x

There is no GeckoLib dependency and no custom 3D model or procedural attack-animation
framework. Better Mob Combat routes humanoid mob melee attacks through Better
Combat. The companion keeps its existing PlayerModel rig, allowing Player
Animator to animate the same model used to render the knight. Vanilla melee
remains the fallback when the combat addons are absent.

## Current project features

- Companion entity registration and spawn egg
- Owner UUID binding and persistent ownership
- Configurable health, damage, movement speed and hostile scan range
- Player-model renderer with `companion_knight.png`
- Vanilla player-style movement animations
- Better Combat attack animations when the optional Better Mob Combat stack is installed
- Held-item and armor rendering
- FOLLOW / STAY / DEFEND command cycle (right-click the companion)
- Shift-right-click to display command and health status
- Follow pathfinding and catch-up teleport
- Basic hostile-mob targeting and vanilla melee attacks
- Give the companion weapons, shields, and armor by right-clicking while holding an item
- Occasional contextual chat based on player health, nearby combat, weather,
  time of day, and idle conditions
- Small persistent memory journal for deaths, diamond/ancient-debris discoveries,
  Nether/End entries, and boss victories; the newest twelve memories are saved
  with the companion and can be referenced in later dialogue
- Persistent bed-based resurrection queue; companion equipment and memories are
  captured on death and restored after the configured delay


## Optional backpack compatibility

Living Companion integrates with **Sophisticated Backpacks** on Forge 1.20.1.

Required mods for the full integration:
- Curios API **5.14.1+** (Forge 1.20.1)
- Sophisticated Core **1.5.30+**
- Sophisticated Backpacks **3.26.20+**

When all three mods are installed:
- The companion receives a Curios **back** slot.
- The companion inventory screen displays the backpack slot.
- Equip a Sophisticated Backpack in that slot.
- Use the **Backpack** button in the companion inventory to open Sophisticated Backpacks' native storage container, including its storage and upgrade interface.
- Backpack access is owner-only and requires the companion to remain within 8 blocks.

The compatibility layer registers the companion's equipped backpack with Sophisticated Backpacks' inventory provider. Curios persists the equipped item, while Sophisticated Backpacks manages its own contents and upgrades. All three dependencies remain optional for Living Companion's core functionality.

## Remaining verification work

- Full ForgeGradle build and in-game test on a normal Windows development machine
- In-game testing of Better Combat / Better Mob Combat attack visuals and weapon compatibility
- Multiplayer testing for ownership, duplicate prevention, and resurrection timing

Complex relationship progression and evolving personality simulation are
intentionally out of scope. The memory journal stays small, event-driven, and
stored directly in the companion's entity data.

## Build

Open this folder in IntelliJ IDEA as a Gradle project, using JDK 17.

```bash
./gradlew genIntellijRuns
./gradlew runClient
./gradlew build
```

On Windows use `gradlew.bat` instead of `./gradlew`.

The distributable mod JAR is created in `build/libs/`. Install it in a Forge
1.20.1 instance.

For Better Combat-style companion attacks, use this Forge 1.20.1 stack:
- Better Combat **1.8.6** or **1.9.0+1.20.1**
- Better Mob Combat **1.3.0**
- Player Animator (Forge 1.20.1)
- Mob Player Animator (Forge 1.20.1)
- Cloth Config API (Forge 1.20.1)
- Better Mob Combat Fix (**BMCFIX 1.0.2**) when using Better Combat 1.9.0

Better Combat 1.9.0 changed two APIs used by Better Mob Combat. BMCFIX 1.0.2
patches the removed `CompatibilityFlags.firstPersonRender()` call and the moved
`PehkuiHelper` class. Install BMCFIX on both client and server with Better
Combat 1.9.0. BMCFIX is unnecessary with Better Combat 1.8.6. Living Companion
accepts Better Combat versions from 1.8.6 up to (but excluding) 2.0.0. This
version declaration permits loading; the complete attack-animation stack still
requires the matching addons listed above.

Install Better Combat and Better Mob Combat on the client and server. Install
Player Animator and Cloth Config API wherever required by their dependency
metadata. Mob Player Animator is a client-side requirement for rendering mob
animations. Without the addon stack, Living Companion continues using vanilla
combat.

### Preventing friendly-fire sweeps

Better Mob Combat's default sweep relation is HOSTILE when no relation is
configured. To keep the companion's wide attacks from hitting players standing
near its target, add this entry inside the `mob_relations` object in
`config/bettermobcombat/server.json`:

```json
"livingcompanion:companion": "{\\"minecraft:player\\":\\"FRIENDLY\\"}"
```

Keep the existing entries in that object. This marks all players as friendly
targets for the companion's Better Combat sweep checks.

## In-game use

1. Find **Living Companion** in the creative inventory tab.
2. Use the Knight Companion Spawn Egg to summon the knight. The player who uses
   the egg becomes the companion's owner.
3. Right-click with an empty hand to cycle FOLLOW → STAY → DEFEND → FOLLOW.
4. Shift-right-click to view his current command and health.
5. Right-click while holding a weapon, shield, or armor piece to give it to him.
6. The companion targets nearby hostile monsters. With Better Mob Combat installed,
   its melee goal uses Better Combat weapon attacks and Player Animator poses.

The `/summon livingcompanion:companion` command also creates the entity. A
nearby player is assigned as owner during spawn finalization when possible.

## Configuration

Forge creates `config/livingcompanion-common.toml` on first launch. Settings
cover basic stats, following distances, hostile scan range, planned resurrection
delay, and dialogue cooldown.
