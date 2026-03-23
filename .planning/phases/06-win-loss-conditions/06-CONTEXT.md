# Phase 6: Win / Loss Conditions - Context

**Gathered:** 2026-03-23
**Status:** Ready for planning
**Source:** Roadmap & Skill Analysis

<domain>
## Phase Boundary

This phase implements the final victory/defeat conditions and the transition to a Game Over state.

1. **Victory Detection**: The game ends when one player loses all their bases.
2. **Game Over Transition**: Transition from `GameScreen` to `GameOverScreen`.
3. **Results Display**: `GameOverScreen` shows the winner and allows returning to the main menu.

</domain>

<decisions>
## Implementation Decisions

### Win Condition Logic
- **[LOCKED]** Win condition is based on `GameState.p1BaseCount` and `p2BaseCount`.
- **[LOCKED]** Destruction of a base (last hit) decrements the owner's count.
- **[LOCKED]** Capture of a base decrements the old owner's count and increments the new owner's count.
- **[LOCKED]** Game over check happens in `GameScreen.render()`.

### User Interface
- **[LOCKED]** `GameOverScreen` is a full-screen UI using Scene2D.
- **[LOCKED]** Displays "PLAYER X VICTORIOUS!".
- **[LOCKED]** "Return to Main Menu" button returns to `MenuScreen`.

### the agent's Discretion
- Implementation of automated tests for win logic.
- Potential polish for `GameOverScreen` (animations, SFX hooks).

</decisions>

<canonical_refs>
## Canonical References

### Data Model
- `core/src/main/java/com/militopia/data/GameState.java` — Tracks base counts.

### Core Systems
- `core/src/main/java/com/militopia/systems/CombatSystem.java` — Handles base destruction logic in `flagDeath`.
- `core/src/main/java/com/militopia/factories/UnitFactory.java` — Handles base creation and capture logic.

### Screens & UI
- `core/src/main/java/com/militopia/screen/GameScreen.java` — Checks win conditions.
- `core/src/main/java/com/militopia/screen/GameOverScreen.java` — Displays game over UI.

</canonical_refs>

---

*Phase: 06-win-loss-conditions*
*Context gathered: 2026-03-23*
