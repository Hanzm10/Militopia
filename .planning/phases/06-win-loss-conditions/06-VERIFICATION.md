---
phase: 06-win-loss-conditions
verified: 2026-03-27T03:00:00Z
status: passed
score: 5/5 must-haves verified
re_verification: false
---

# Phase 6: Win/Loss Conditions Verification Report

**Phase Goal:** Implement win/loss conditions with WinConditionSystem, GameOverScreen UI, and automated tests covering both game-logic and UI components.
**Verified:** 2026-03-27T03:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

> Note: The phase was delivered as a refactor — `GameOverScreen` (full-screen) was replaced by `GameOverPopup` (modal overlay) and win-detection logic was extracted from `GameScreen.render()` into a dedicated `WinConditionSystem` Ashley ECS system. The goal is fully met under this revised design.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | WinConditionSystem detects when a player loses all bases and triggers game over | VERIFIED | `WinConditionSystem.update()` checks `p1BaseCount == 0` / `p2BaseCount == 0` and calls `trigger.trigger(winnerID)` |
| 2 | Game over UI appears when win condition fires | VERIFIED | `GameScreen` wires trigger → `gameHUD.showGameOverPopup(winnerID)` → `GameOverPopup.show()` adds modal to stage |
| 3 | Game over UI shows winner info and provides menu return | VERIFIED | `GameOverPopup.buildPopup()` renders winner label and "Return to Main Menu" button with `ClickListener` calling `gameScreen.saveAndExit()` |
| 4 | Automated tests cover game-logic (base tracking, win trigger) | VERIFIED | `WinConditionTest.java` — 5/5 tests pass: creation, destruction, capture, town-capture, and trigger mock verification |
| 5 | Automated tests cover UI components (HudTopBar, InfoPanel) | VERIFIED | `UITest.java` — 2/2 tests pass: `testHudTopBarLogic` and `testInfoPanelLayout` |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `core/src/main/java/com/militopia/systems/WinConditionSystem.java` | Ashley ECS system for win detection | VERIFIED | 67 lines, full implementation with `GameOverTrigger` interface, `update()` logic, `setPlaying()` guard |
| `core/src/main/java/com/militopia/ui/GameOverPopup.java` | Modal UI showing winner and menu button | VERIFIED | 104 lines, builds popup table, sets winner label with color, wires menu-return click listener |
| `core/src/test/java/com/militopia/systems/WinConditionTest.java` | JUnit5 tests for game-logic | VERIFIED | 139 lines, 5 test methods all passing |
| `core/src/test/java/com/militopia/ui/UITest.java` | JUnit5 tests for UI components | VERIFIED | 162 lines, 2 test methods all passing, uses Mockito MockedConstruction for headless libGDX |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `GameScreen` | `WinConditionSystem` | `engine.addSystem(winConditionSystem)` at line 162 | WIRED | System instantiated with `loadedState` and lambda trigger, added to Ashley engine |
| `WinConditionSystem` | `GameHUD.showGameOverPopup()` | Lambda `winnerID -> gameHUD.showGameOverPopup(winnerID)` | WIRED | Trigger fires on base count reaching zero, delegates to HUD |
| `GameHUD` | `GameOverPopup.show()` | `gameOverPopup.show(winnerID)` at line 255 | WIRED | `GameOverPopup` constructed in `GameHUD` constructor (line 89), `show()` called on trigger |
| `GameOverPopup` | `GameScreen.saveAndExit()` | `ClickListener` on "Return to Main Menu" button | WIRED | Button click calls `gameScreen.saveAndExit()` |
| `GameScreen` (load path) | `GameOverPopup` (restore) | `if (gameState.isGameOver)` block lines 229-232 | WIRED | Finished games restored from save correctly show popup and disable system |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `WinConditionSystem` | `gameState.p1BaseCount` / `p2BaseCount` | `GameState` mutated by `CombatSystem.flagDeath()` and `UnitFactory.captureStructure()` | Yes — tested in `WinConditionTest` | FLOWING |
| `GameOverPopup` | `winnerID` (determines label text and color) | Passed from `WinConditionSystem.triggerGameOver()` through trigger lambda | Yes — mock-verified in `testWinConditionSystemTriggersGameOver` | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `WinConditionTest` — 5 tests pass | `./gradlew :core:test --tests com.militopia.systems.WinConditionTest` | 5/5 passed, 0 failures, 0 errors (3.467s) | PASS |
| `UITest` — 2 tests pass | `./gradlew :core:test --tests com.militopia.ui.UITest` | 2/2 passed, 0 failures, 0 errors (2.012s) | PASS |
| Module exports expected trigger interface | `WinConditionSystem.GameOverTrigger` public interface | Confirmed in source | PASS |

### Requirements Coverage

No requirement IDs were declared for this phase (phase requirement IDs: null). Coverage assessed against phase goal directly — all goal components verified above.

### Anti-Patterns Found

None. No TODOs, FIXMEs, placeholder comments, empty handlers, or stub returns found in `WinConditionSystem.java` or `GameOverPopup.java`.

Old `GameOverScreen.java` (full-screen approach) confirmed absent — no dead code left behind.

### Human Verification Required

#### 1. Visual popup appearance on real hardware

**Test:** Start a new game, destroy or capture all enemy bases.
**Expected:** Modal `GameOverPopup` overlay appears over the game board showing "GAME OVER", winner name in color (CYAN for P1, RED for P2), and "Return to Main Menu" button.
**Why human:** libGDX rendering requires a GL context; cannot verify visual layout in headless test environment.

#### 2. Input blocking while popup is visible

**Test:** With game over popup open, attempt to click tiles on the map and interact with the HUD bottom bar.
**Expected:** All map and HUD input is blocked; only the "Return to Main Menu" button responds.
**Why human:** `InputListener` returning `true` blocks events in the scene2d event system — requires live input processing to verify.

#### 3. Save-and-exit flow from game over

**Test:** Click "Return to Main Menu" on the game over popup.
**Expected:** Game saves state with `isGameOver=true` and navigates to the main menu screen.
**Why human:** Screen transition and save I/O require the running application.

### Gaps Summary

No gaps. All automated checks passed. Phase goal is fully achieved.

---

_Verified: 2026-03-27T03:00:00Z_
_Verifier: Claude (gsd-verifier)_
