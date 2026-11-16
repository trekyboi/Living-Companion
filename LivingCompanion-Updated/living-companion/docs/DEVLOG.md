# Living Companion — Development Log

## Scope revision — 2026-11-16

The project has been deliberately simplified to keep the mod achievable and
maintainable:

- Use Minecraft's vanilla `PlayerModel` with a custom knight skin.
- Use vanilla humanoid movement and melee swing animations.
- Remove GeckoLib and custom 3D model/animation work.
- Keep Better Combat optional; do not call player-only animation APIs from the NPC.
- Keep long-term memory lightweight: store a bounded list of significant events
  directly in the companion's entity NBT and occasionally reference them.
- Remove complex relationship progression and evolving personality simulation.

## Current implementation

- [x] Forge 1.20.1 project and Java 17 configuration
- [x] Entity registration, spawn egg, and creative tab
- [x] Owner UUID persistence and owner binding
- [x] Configurable core attributes
- [x] Vanilla player-model renderer and knight skin
- [x] Held-item and armor rendering
- [x] FOLLOW / STAY / DEFEND command cycle
- [x] Follow pathfinding and catch-up teleport
- [x] Basic hostile target selection and vanilla melee attack goal
- [x] Weapon, shield, and armor handoff from player to companion
- [x] Simple contextual dialogue with cooldown
- [x] Persistent event memories for player deaths, ore discoveries, dimension entries,
      and Ender Dragon/Wither victories (maximum twelve)
- [x] Bed resurrection queue stores the companion's NBT and restores equipment and memories
- [ ] Better Combat weapon compatibility testing
- [x] Bed resurrection queue with saved entity equipment/memories, owner bed target, delay, fallback spawn, and duplicate prevention
- [ ] More robust protect-owner target selection
- [ ] Full Gradle build and in-game test on a normal Windows development machine

## Explicitly removed from scope

- Custom 3D character model
- GeckoLib and custom animation controllers
- Complex relationship progression
- Evolving personality simulation
- Elaborate autonomous behaviors

## Notes

The vanilla player model provides the standard player-shaped humanoid rig and
its normal walk/arm-swing animation behavior. Better Combat is primarily
player-oriented; this project does not claim that Better Combat's special
player animation pipeline automatically controls a custom mob. The NPC uses
vanilla melee AI and standard item attack-damage attributes so it remains
functional when Better Combat is absent.

The memory journal stores only the newest twelve unique event summaries. It is
intended to create occasional callbacks in dialogue, not to simulate a full
AI memory or relationship system.

## Verification notes — 2026-11-16

- Added `CompanionResurrectionData` using overworld `SavedData` so pending returns survive server restarts.
- Added death and server-tick event handlers.
- The queue captures the companion's entity NBT, owner UUID, target dimension/bed position, and due game time.
- Resurrection removes the old entity UUID, restores the entity data, sets health to maximum, and checks for an existing living companion owned by the same player before spawning.
- If the saved bed location is unavailable or obstructed, the companion returns at the destination dimension's shared spawn. If the owner's bed is unknown when death occurs, the Overworld shared spawn is used.
- Full Gradle verification could not run in this environment: Gradle 8.1.1 download failed because `services.gradle.org` could not be resolved (network/DNS unavailable).
- Better Combat remains optional and unverified for NPC-specific animations. The companion's vanilla melee path remains the fallback.

## Maintenance pass — 2026-11-16

- Fixed STAY behavior so the companion drops an existing hunt target and stops proactively acquiring nearby hostile targets. The retaliation goal remains enabled.
- Fixed spawn-egg consumption so failed spawns do not consume the item and Creative players keep their egg.
- Added a GitHub Actions workflow at `.github/workflows/forge-build.yml` to run the ForgeGradle build on pushes, pull requests, and manual dispatches. This provides a remote build check; it does not replace in-game testing.
- These changes are committed to the `main` branch. Full local Forge build and gameplay verification remain pending.
- Fixed a resurrection health edge case: resurrected entities now receive the configured maximum health after their first-tick attribute configuration is applied, including when the server's max-health setting differs from the default.
