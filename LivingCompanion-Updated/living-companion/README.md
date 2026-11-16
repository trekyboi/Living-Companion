# Living Companion — Minecraft Forge 1.20.1

A lightweight knight companion mod for Minecraft Java 1.20.1 and Forge 47.x.
The companion uses Minecraft's **vanilla player model**, a custom 64×64 knight
skin, and the normal humanoid walking and arm-swing animation pipeline.

## Target environment

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.x (development pinned to 47.4.0)
- Java 17
- ForgeGradle 6 / Gradle wrapper 8.1.1
- Optional: Better Combat for 1.20.1 Forge

There is no GeckoLib dependency and no custom 3D model or animation framework.
Better Combat can be installed alongside the mod. The companion uses vanilla
server-side melee AI and standard item attack-damage attributes; weapon items
that expose those attributes can be equipped. Better Combat-specific NPC attack
animations are not claimed or required.

## Current project features

- Companion entity registration and spawn egg
- Owner UUID binding and persistent ownership
- Configurable health, damage, movement speed and hostile scan range
- Player-model renderer with `companion_knight.png`
- Vanilla player-style movement and melee swing animations
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

## Remaining verification work

- Full ForgeGradle build and in-game test on a normal Windows development machine
- Better Combat compatibility testing for modded weapon items and NPC attack visuals
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
1.20.1 instance. Better Combat is optional and can be installed separately.

## In-game use

1. Find **Living Companion** in the creative inventory tab.
2. Use the Knight Companion Spawn Egg to summon the knight. The player who uses
   the egg becomes the companion's owner.
3. Right-click with an empty hand to cycle FOLLOW → STAY → DEFEND → FOLLOW.
4. Shift-right-click to view his current command and health.
5. Right-click while holding a weapon, shield, or armor piece to give it to him.
6. The companion targets nearby hostile monsters and uses ordinary melee AI.

The `/summon livingcompanion:companion` command also creates the entity. A
nearby player is assigned as owner during spawn finalization when possible.

## Configuration

Forge creates `config/livingcompanion-common.toml` on first launch. Settings
cover basic stats, following distances, hostile scan range, planned resurrection
delay, and dialogue cooldown.
