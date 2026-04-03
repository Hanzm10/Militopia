---
phase: 09-advanced-mechanics
plan: 02
status: complete
subsystem: movement, ui
tags: [railway, floodFill, SlideMenu, movement-bonus]
dependency_graph:
  requires: ["09-01"]
  provides: ["railway-movement-bonus", "build-railway-ui"]
  affects: ["GameInputController", "SlideMenu"]
tech_stack:
  added: []
  patterns: ["x2-integer-budget-scaling", "per-step-cost-lookup"]
key_files:
  modified:
    - core/src/main/java/com/militopia/controller/GameInputController.java
    - core/src/main/java/com/militopia/ui/SlideMenu.java
decisions:
  - "x2 integer scaling: budget = moveRange*2, rail-to-rail cost=1, normal cost=2 — avoids float math and keeps existing visitedMoves comparison clean"
  - "Both-tile rule: both fromX/fromY and toX/toY must have rails for cost=1, preventing half-cost entry from non-rail tiles"
  - "Enemy territory check reuses findControllingBase(x, y, enemyOwner)[0]==1 pattern already in GameInputController"
  - "Null icon for Railway button: SummonButton.addToWrapped already handles null icon (no Image added to stack)"
metrics:
  duration: "~15 minutes"
  completed_date: "2026-04-03"
  tasks: 2
  files: 2
---

# Plan 09-02 Summary — Railway Movement Bonus & Build UI

## One-liner

Implemented x2 integer-scaled floodFill for railway movement bonus and "Build Railway" SlideMenu action for 3 funding.

## What Was Built

- `getRailStepCost(fromX, fromY, toX, toY, moveType, unitOwner)` — LAND units get cost=1 on rail-to-rail steps (both tiles must have rail and neither in enemy territory), cost=2 otherwise; non-LAND moveTypes always return cost=2
- `floodFill` refactored — x2 integer scaling: budget passed as `moveRange * 2`, 8-directional hardcoded calls replaced with a loop using per-step cost via `getRailStepCost`; new `unitOwner` parameter threaded through all recursive calls
- `SlideMenu.populateBuildMenu` — "Build Railway" button added after the structure loop: costs 3 funding, sets `gameMap.rails[buildX][buildY] = true`, terrain validation (blocks WATER, DEEP_WATER, MOUNTAIN), duplicate-rail guard, HUD update, build SFX, GameLogger entry

## Deviations from Plan

None — plan executed exactly as written.

## Verification

- `floodFill` call site in `showRangeMarkers` updated to `moveRange * 2` budget and `stats.owner` passed as `unitOwner`
- All recursive `floodFill` calls updated to 9-parameter signature (no old 8-parameter calls remain)
- AIR/SEA unaffected: `getRailStepCost` returns 2 immediately for non-LAND moveType
- Both-tile rule enforced: `!gameMap.rails[fromX][fromY] || !gameMap.rails[toX][toY]` returns 2
- Enemy territory check: reuses existing `findControllingBase` — returns `[1, ...]` when tile is in enemy's base vision range
- `SummonButton.addToWrapped` handles null icon gracefully (confirmed in source)
- `GameLogger.BUILD` constant confirmed to exist
- `GameScreen.getGameMap()` confirmed to exist (line 365)
- Commit: ad7e969

## Self-Check: PASSED

- `C:/Users/Hanz Mapua/Workspace/Militopia/core/src/main/java/com/militopia/controller/GameInputController.java` — FOUND
- `C:/Users/Hanz Mapua/Workspace/Militopia/core/src/main/java/com/militopia/ui/SlideMenu.java` — FOUND
- Commit ad7e969 — FOUND
