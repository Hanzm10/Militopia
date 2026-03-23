---
phase: 06-win-loss-conditions
status: COMPLETE
---

# Phase 6 UAT: Win / Loss Conditions

### 1. Victory Detection (Base Count zero)
expected: When a player's base count reached zero, the game ends.
result: PASS
reason: Verified via `WinConditionTest.java` and code audit of `GameScreen.java`.

### 2. Game Over Screen Navigation
expected: Game over screen displays the winner and allows returning to the main menu.
result: PASS
reason: `GameOverScreen.java` is implemented and correctly transitions back to `MenuScreen`.

### 3. Verification Test
expected: Automated test suite passes for win/loss logic.
result: PASS
reason: `WinConditionTest.java` passed 4/4 cases on 2026-03-23.
