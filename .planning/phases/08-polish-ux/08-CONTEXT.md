# Phase 8: Polish & UX - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Comprehensive UI overhaul, dynamic visual feedback, animations, and sound effects.

</domain>

<decisions>
## Implementation Decisions

### Base Info Panel UI
- **D-01:** Integrate an `InfoPanel` into the existing `SlideMenu` to show base stats (Name, Level, XP, Income, Vision, and upcoming level-up rewards).
- **D-02:** Use horizontal scrolling if the summon buttons get crowded.
- **D-03:** Auto-hides when clicking elsewhere on the map (fast hot-seat play).
- **D-04:** Reuses the exact same sliding transition style currently used by `SlideMenu`.

### Attack Animations
- **D-05:** Projectile sprites will physically travel from the attacker to the defender.
- **D-06:** Damage numbers pop up instantly upon clicking, even as the projectile animation begins.
- **D-07:** Counterattacks play sequentially (after the attacker finishes).
- **D-08:** Screen shakes slightly when units take damage.

### Floating Status Popups
- **D-09:** Cascading floating text (one after another) when multiple events (like XP+Funding) happen at once.
- **D-10:** Text will be visually differentiated with both distinct colors and small icons (e.g., a tiny coin icon for Funding).

### Action Undo System
- **D-11:** Right UI panel displays a list of GameState snapshots (limited to the last 10 turns).
- **D-12:** Accessed via a button in the Settings.
- **D-13:** Panel must include a description of what it is and how to use it at the top.
- **D-14:** Keyboard shortcuts: `Ctrl+Z` to undo, `Ctrl+Shift+Z` to redo.

### Claude's Discretion
- Exactly what animations look like (duration, visual flair, screen shake intensity) as long as they fit the constraints.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### UI & Input
- `core/src/main/java/com/militopia/ui/GameHUD.java` — For adding the new base InfoPanel, snapshot right UI panel, and settings button.
- `core/src/main/java/com/militopia/systems/GameInputController.java` — For handling Ctrl+Z and Ctrl+Shift+Z hotkeys and auto-hide logic.
- `core/src/main/java/com/militopia/ui/SlideMenu.java` — For observing the existing Slide transition behavior and layout.

</canonical_refs>
