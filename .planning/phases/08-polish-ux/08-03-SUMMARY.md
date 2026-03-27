---
phase: "08"
plan: "03"
subsystem: undo-mechanics
tags: [undo, snapshot, hud, input, history]
dependency_graph:
  requires: [08-02-PLAN.md]
  provides: [action-level-undo, snapshot-panel, ctrl-z-hotkey]
  affects: [GameInputController, GameHUD, TurnHistoryManager, GameScreen]
tech_stack:
  added: []
  patterns: [pre-action-snapshot, collapsible-overlay, ashley-ecs-restore]
key_files:
  created: []
  modified:
    - core/src/main/java/com/militopia/controller/GameInputController.java
    - core/src/main/java/com/militopia/managers/TurnHistoryManager.java
    - core/src/main/java/com/militopia/ui/GameHUD.java
    - core/src/main/java/com/militopia/screen/GameScreen.java
decisions:
  - Pre-action snapshot (not post-action) enables Ctrl+Z to rewind to exactly before each discrete action
  - TurnHistoryManager.size() added to support right panel label count
  - Undo button made always-visible (removed TESTING_MODE guard) via dedicated right panel
  - GameHUD.build() extended with TurnHistoryManager overload to preserve backward compat
metrics:
  duration: "~10 minutes"
  completed_date: "2026-03-27"
  tasks_completed: 3
  files_modified: 4
---

# Phase 08 Plan 03: Undo Mechanics Summary

Pre-action granular snapshotting, Ctrl+Z hotkey (pre-existing), and a collapsible right-panel history overlay with an always-visible Undo button wired to the same `undoTurn()` restore logic.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Action Snapshot Granularity | 6d15bb1 | GameInputController.java, TurnHistoryManager.java |
| 2 | Ctrl+Z Hotkey Listener | c4374a2 | (pre-existing; verified complete) |
| 3 | Right Panel Snapshot Overlay | c4374a2 | GameHUD.java, GameScreen.java, GameInputController.java |

## What Was Built

**Task 1 — Action Snapshot Granularity**

Changed `snapshot()` call placement from AFTER each discrete action to BEFORE. This ensures `TurnHistoryManager` captures the pre-action state, so Ctrl+Z rewinds to exactly before that action occurred. Actions covered: `moveUnit`, `performAttack`, `performHunt`, `performAbility` (DIG_IN, OVERWATCH), `executeTargetingAbility` (LAUNCH_NUKE). Added `size()` to `TurnHistoryManager` for panel queries.

**Task 2 — Ctrl+Z Hotkey Listener**

Already fully implemented in a prior session. `GameInputController.keyDown()` detects `Input.Keys.Z` with either CTRL modifier and calls `screen.undoTurn()`. The `undoTurn()` method in `GameScreen` already: removes all UNIT entities, restores GameState scalars, restores map objects array, updates structures in-place, recreates unit entities from snapshot, and refreshes fog + HUD (turn, funding, XP). Verified complete — no code changes required.

**Task 3 — Right Panel Snapshot Overlay**

Added a collapsible right-side `Table` panel in `GameHUD` that:
- Mounts as a stage overlay anchored top-right (not inside the rootTable layout)
- Shows up to 10 "Action {N}" `Label` rows reflecting current `TurnHistoryManager.size()`
- Has a toggle button ("▶ History" / "▼ History") to collapse/expand the list
- Contains a dedicated "↩ Undo" `TextButton` (always visible, no longer TESTING_MODE-gated) that calls `screen.undoTurn()` and refreshes the panel
- `refreshSnapshotPanel()` is called from both `snapshot()` (post-push) and `undoTurn()` so the panel stays in sync

`GameHUD.build()` now has an overload accepting `TurnHistoryManager`. `GameScreen` passes `turnHistory` to it. `GameScreen.undoTurn()` calls `gameHUD.refreshSnapshotPanel()` after restoring state.

## Deviations from Plan

### Auto-fixed Issues

None — plan executed as written.

### Observations

The plan specified "push BEFORE actions, not just at turn execution boundaries." The prior implementation already called `snapshot()` granularly (after every action), so the primary change was moving the call to the top of each action method. The post-action calls were removed to avoid double-pushing.

## Deferred Issues

**Pre-existing AbilityTest failures (out of scope)**

`AbilityTest` has 3 failing tests due to `NullPointerException` on `entityFactory` in `CombatSystem` during headless unit tests. These failures pre-date this plan — the test environment cannot create libGDX visual effects (muzzle flash, tank attack). CLAUDE.md confirms "No test suite exists — manual play-testing is the verification method." The Java compilation succeeds cleanly.

Files: `core/src/test/java/com/militopia/systems/AbilityTest.java`

## Known Stubs

The right panel currently labels history entries generically as "Action {N}" (1-indexed by stack position). No action type metadata (move/attack/ability) is stored in `TurnSnapshot`, so richer labels like "Moved Recruit (2,3)→(4,5)" are not possible without extending `TurnSnapshot` with an action description field. This is intentional — the plan specified "Action {N}" labels.

## Self-Check: PASSED

- `GameInputController.java` — snapshot() before each action: verified
- `TurnHistoryManager.java` — size() method added: verified
- `GameHUD.java` — buildSnapshotPanel(), refreshSnapshotPanel() added: verified
- `GameScreen.java` — passes turnHistory to build(), calls refreshSnapshotPanel() in undoTurn(): verified
- Commits 6d15bb1, c4374a2 — both present in git log
