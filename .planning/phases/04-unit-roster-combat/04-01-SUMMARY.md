---
phase: 04-unit-roster-combat
plan: 01
subsystem: testing
tags: [ashley-ecs, unit-factory, combat-roster, junit5]

# Dependency graph
requires:
  - phase: 04-unit-roster-combat
    provides: UnitFactory with getUnitCost and createUnit for all 13 unit types
provides:
  - WRAITH dead code removed from UnitFactory and UnitRenderSystem
  - CombatRosterTest guards cost consistency for all 13 unit type keys
  - getUnitCost made static for testability
affects: [unit-roster-combat, combat-system, future-unit-types]

# Tech tracking
tech-stack:
  added: []
  patterns: [Static pure-switch utility methods for cost/type lookups; JUnit5 tests directly calling UnitFactory static methods without AssetManager]

key-files:
  created:
    - core/src/test/java/com/militopia/systems/CombatRosterTest.java
  modified:
    - core/src/main/java/com/militopia/factories/UnitFactory.java
    - core/src/main/java/com/militopia/systems/UnitRenderSystem.java

key-decisions:
  - "Made UnitFactory.getUnitCost static — pure switch with no instance fields, allows test-time invocation without AssetManager dependency"
  - "CombatRosterTest calls getUnitCost statically rather than instantiating UnitFactory — avoids full libGDX AssetManager setup in unit tests"

patterns-established:
  - "Pure data lookup methods on factories (getUnitCost, getUnitMoveType) should be static to enable lightweight testing"

requirements-completed: []

# Metrics
duration: 5min
completed: 2026-03-27
---

# Phase 04 Plan 01: Unit Roster Combat Summary

**WRAITH dead code removed from UnitFactory and UnitRenderSystem; CombatRosterTest added with static getUnitCost guard for all 13 unit types**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-27T02:13:18Z
- **Completed:** 2026-03-27T02:18:22Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Removed WRAITH from UnitFactory cloaking post-process block and UnitRenderSystem stealth branch — zero WRAITH references remain in production code
- SUBMARINE cloaking preserved intact
- Created CombatRosterTest with testCostConsistency (13 units) and testSummonableUnitCount (10 summonable) — both pass
- Made getUnitCost static to allow clean unit tests without AssetManager

## Task Commits

Each task was committed atomically:

1. **Task 1: Remove WRAITH dead code** - `be22025` (fix)
2. **Task 2: Create CombatRosterTest for cost duplication guard** - `9f934c8` (feat)

**Plan metadata:** (docs commit — next)

## Files Created/Modified
- `core/src/test/java/com/militopia/systems/CombatRosterTest.java` - New test: verifies getUnitCost values and summonable count for all 13 unit types
- `core/src/main/java/com/militopia/factories/UnitFactory.java` - WRAITH removed from cloaking block; getUnitCost made static
- `core/src/main/java/com/militopia/systems/UnitRenderSystem.java` - WRAITH else-if stealth branch deleted

## Decisions Made
- Made `getUnitCost` static instead of creating a separate data class — the method is already a pure switch (no instance fields), making it static is the minimal change
- CombatRosterTest tests `getUnitCost` against a hardcoded expected-cost map rather than calling `createUnit` — avoids the need for AssetManager/libGDX initialisation in tests

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Made getUnitCost static to enable test instantiation without AssetManager**
- **Found during:** Task 2 (Create CombatRosterTest)
- **Issue:** Plan assumed a 4-arg UnitFactory constructor `(PooledEngine, AssetManager, GameMap, GameState)` but actual constructor is `(PooledEngine, AssetManager)` and immediately calls `assets.get(...)` — passing null would NPE. The test could not instantiate UnitFactory.
- **Fix:** Made `getUnitCost` static (pure switch, no instance fields). Test calls `UnitFactory.getUnitCost(key)` directly.
- **Files modified:** `core/src/main/java/com/militopia/factories/UnitFactory.java`
- **Verification:** Existing callers (`InfoPanel.java`, `SlideMenu.java`) still compile — Java allows instance calls to static methods. CombatRosterTest BUILD SUCCESSFUL.
- **Committed in:** `9f934c8` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Minimal — pure compiler-level change, no behavioral difference. getUnitCost was already stateless.

## Issues Encountered
- AbilityTest has 3 pre-existing failures (NPE: entityFactory is null in CombatSystem) — these predate this plan and are out of scope. Confirmed by running tests before and after stash.

## Known Stubs
None — no stub patterns detected in created/modified files.

## Next Phase Readiness
- Phase 4 debt cleared: no WRAITH references, cost regression guard active
- CombatRosterTest will catch any future cost drift between createUnit and getUnitCost
- Pre-existing AbilityTest failures (entityFactory null) remain — logged as out-of-scope deferred item

## Self-Check: PASSED

- FOUND: `core/src/test/java/com/militopia/systems/CombatRosterTest.java`
- FOUND: `core/src/main/java/com/militopia/factories/UnitFactory.java`
- FOUND: `core/src/main/java/com/militopia/systems/UnitRenderSystem.java`
- FOUND: commit `be22025` (fix: WRAITH removal)
- FOUND: commit `9f934c8` (feat: CombatRosterTest + static getUnitCost)

---
*Phase: 04-unit-roster-combat*
*Completed: 2026-03-27*
