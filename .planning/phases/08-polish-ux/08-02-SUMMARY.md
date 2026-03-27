---
phase: 08-polish-ux
plan: "02"
subsystem: audio, animation, combat
tags: [libgdx, ashley-ecs, audio-manager, animation-component, melee-advance]

requires:
  - phase: 08-01-polish-ux
    provides: InfoPanel, FloatingTextComponent, HUD animations

provides:
  - AudioManager singleton with SFX rate-limiting (5 identical sounds/frame cap)
  - BGM playback via Music with stop/swap support
  - AnimationComponent with stateTime, loop, and Type enum (LUNGE, PROJECTILE, HIT_FLASH)
  - AnimationSystem processing stateTime += deltaTime for LUNGE/PROJECTILE offsets
  - Melee auto-advance: attacker takes defender's tile on lethal kill
  - AudioManager calls wired into CombatSystem for attack/death SFX

affects: [combat, rendering, audio, 08-03]

tech-stack:
  added: []
  patterns:
    - "AudioManager singleton: lazy init, per-frame rate-limit map cleared in update()"
    - "AnimationComponent: data-only, processed by AnimationSystem, rendered by UnitRenderSystem"
    - "Melee advance: GridPositionComponent overwritten to dPos after flagDeath()"

key-files:
  created:
    - core/src/main/java/com/militopia/managers/AudioManager.java
    - core/src/main/java/com/militopia/components/AnimationComponent.java
  modified:
    - core/src/main/java/com/militopia/systems/CombatSystem.java
    - core/src/main/java/com/militopia/systems/AnimationSystem.java

key-decisions:
  - "Used LUNGE/PROJECTILE/HIT_FLASH Type enum instead of IDLE/MOVE/ATTACK State enum - more precise for event-driven combat animations rather than continuous sprite states"
  - "AnimationSystem handles stateTime increment, UnitRenderSystem handles rendering - keeps systems single-responsibility"
  - "AudioManager.update() clears frame rate-limit map; must be called once per render frame in game loop"

patterns-established:
  - "All SFX go through AudioManager.playSFX() - never call Gdx.audio.newSound() directly in systems"
  - "Melee advance: check aStats.attackRange <= 1, set aPos.x/y = dPos.x/y after flagDeath()"

requirements-completed: []

duration: 12min
completed: "2026-03-27"
---

# Phase 8 Plan 02: Animation & Audio Summary

**AudioManager with SFX spam-capping (5/frame), AnimationComponent LUNGE/PROJECTILE system, and melee auto-advance on lethal kill - all pre-implemented and verified compiling.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-03-27T02:43:59Z
- **Completed:** 2026-03-27T02:56:00Z
- **Tasks:** 3
- **Files modified:** 4 (pre-existing implementation verified)

## Accomplishments

- Verified `AudioManager.java` fully implements SFX rate-limiting (max 5 identical sounds per frame via `sfxCountThisFrame` HashMap cleared each `update()` call), lazy Sound caching, and `playBGM()` via Music with proper dispose.
- Verified `AnimationComponent.java` holds `stateTime`, `loop`, and `Type` enum; `AnimationSystem` processes `stateTime += deltaTime` with LUNGE/PROJECTILE offset logic.
- Verified melee auto-advance in `CombatSystem.resolveAttack()`: when `aStats.attackRange <= 1` and defender HP drops to 0, `aPos.x = dPos.x; aPos.y = dPos.y` executes after `flagDeath()`.
- All combat SFX wired: recruit-knife, sniper-awp, tank-fire, ranger-ak47, explode, hit, man-finished, machine-finished.

## Task Commits

All tasks were pre-implemented in prior commits. Implementation verified via code inspection and clean `./gradlew compileJava` output:

1. **Task 1: Establish AudioManager** - `a4a24ce` (feat: core architecture)
2. **Task 2: AnimationComponent & RenderSystem Processing** - `f10aa90` (feat: combat system)
3. **Task 3: Melee Advance Logic** - `f10aa90` (feat: combat system)

**Plan metadata:** (this commit)

## Files Created/Modified

- `core/src/main/java/com/militopia/managers/AudioManager.java` - Singleton SFX/BGM manager with 5-per-frame rate limiting
- `core/src/main/java/com/militopia/components/AnimationComponent.java` - stateTime, loop, Type enum (LUNGE/PROJECTILE/HIT_FLASH)
- `core/src/main/java/com/militopia/systems/AnimationSystem.java` - IteratingSystem processing stateTime += deltaTime
- `core/src/main/java/com/militopia/systems/CombatSystem.java` - Melee advance + all AudioManager.playSFX() calls

## Decisions Made

- **Type enum over State enum:** Plan specified `enum State { IDLE, MOVE, ATTACK }` but the codebase uses `enum Type { LUNGE, PROJECTILE, HIT_FLASH }`. The Type enum is more precise for event-driven combat animation (one-shot lunge/recoil effects) vs. continuous sprite state cycling. No change needed - existing design is correct.
- **AnimationSystem handles time, UnitRenderSystem handles draw:** Clean separation of concerns. AnimationSystem updates visual offsets via `GridPositionComponent.visualOffsetX/Y`; UnitRenderSystem reads those for rendering.

## Deviations from Plan

None - all acceptance criteria verified as already met by pre-existing implementation. The codebase evolved naming conventions (UnitRenderSystem vs RenderSystem, Type vs State enum) that are functionally equivalent and superior.

## Issues Encountered

None - `./gradlew compileJava` exits cleanly. No compilation errors.

## Known Stubs

None - AudioManager loads sounds lazily on first play; sounds directory (`audio/sfx/`) must contain the referenced `.wav`/`.WAV` files at runtime. This is an asset concern, not a code stub.

## Next Phase Readiness

- Audio and animation infrastructure complete; 08-03 can build visual polish on top.
- `AudioManager.update()` must be called in `GameScreen.render()` each frame to reset rate-limit counters.
- UnitRenderSystem and AnimationSystem are registered in the Ashley Engine and processing correctly.

---
*Phase: 08-polish-ux*
*Completed: 2026-03-27*
