# Phase 6: Win / Loss Conditions - Research

## Research Objective
Identify the current state of win/loss conditions and determine what remains for full implementation and verification.

## Findings

### 1. Base Tracking
- **Location**: `core/src/main/java/com/militopia/data/GameState.java`
- **Mechanism**: `p1BaseCount` and `p2BaseCount` integers track the number of bases owned by each player.
- **Initialization**: `GameScreen` constructor resets these to 0, and `UnitFactory.createObjectEntity` increments them during map loading.

### 2. Event-Driven Updates
- **Destruction**: `CombatSystem.flagDeath(Entity)` decrements the base count if the entity name contains "Base".
- **Capture**: `UnitFactory.captureStructure(...)` correctly handles ownership transfer, incrementing the new owner's count and decrementing the old owner's count if it was a base.

### 3. Victory Condition Check
- **Location**: `core/src/main/java/com/militopia/screen/GameScreen.render(float)`
- **Logic**: If `turnState == PLAYING` and either base count is 0, a `GameOverScreen` is instantiated.
- **Transition**: `game.setScreen(new GameOverScreen(game, winnerID))` is used.

### 4. Game Over Screen
- **Location**: `core/src/main/java/com/militopia/screen/GameOverScreen.java`
- **Features**: 
    - Full-screen scene2D table.
    - Displays winner name in their respective color.
    - "Return to Main Menu" button transitions back to `MenuScreen`.

## Identified Gaps
1. **Validation**: No automated test explicitly verifies that base counts reaching 0 triggers the screen transition logic.
2. **Implementation Plan Quality**: The existing `06-01-PLAN.md` lacks GSD execution tags (`<read_first>`, `<acceptance_criteria>`, `<action>`).
3. **UAT Status**: `06-UAT.md` incorrectly reports that counts are not yet updated in `CombatSystem`.

## Conclusion
The technical implementation is mostly complete. The focus should shift to verification and plan formalization for GSD compliance.

---
*Research completed: 2026-03-23*
