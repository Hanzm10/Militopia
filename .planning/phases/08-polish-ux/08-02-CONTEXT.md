# Phase 08: Polish & UX (Wave 2) - Context

**Wave:** 2
**Scope:** Combat Animations, Audio Infrastructure, and Action Undo.

## Decisions

### 1. Combat Animations
- **Melee (Range 1)**: Units will perform a "lunge" (sprite physically moves towards the target and back). A "hit" sprite animation must play on the target upon impact.
- **Ranged**: Implement a generic projectile entity (bullet/shell sprite) that travels from the attacker to the defender.
- **Sequential**: Counterattacks must wait for the initial attack animation (including projectile travel) to finish.
- **Auto-Advance**: When a unit with an **Attack Range of 1** (melee) kills its target, it automatically moves into the defender's tile. This is a "replacement" move and should **not** trigger Overwatch or Ruins rewards.

### 2. Action Undo System
- **Granularity**: Every single action (Movement, Combat, Summoning) must be undoable within the current turn.
- **Persistence**: Snapshots should be taken before each discrete action.
- **UI**: Ensure the Undo button is accessible (e.g., via a hotkey `Ctrl+Z` or a HUD button).

### 3. Audio Infrastructure
- **System**: Create a dedicated `AudioManager` or integrate sound triggers into existing ECS systems.
- **Assets**: Use placeholder "beeps/boops" for now. Ensure they are easy to replace with real assets later.
- **Coverage**: Movement, Attack, Damage, and Turn Start/End.

## Canonical References

- `core/src/main/java/com/militopia/systems/CombatSystem.java`: Inject animation triggers and auto-advance logic.
- `core/src/main/java/com/militopia/systems/UnitRenderSystem.java`: Implement lunge and hit sprite rendering.
- `core/src/main/java/com/militopia/managers/TurnHistoryManager.java`: Expand to support per-action snapshots.
- `core/src/main/java/com/militopia/managers/AssetManager.java`: Add placeholder sound loading.
- `core/src/main/java/com/militopia/controller/GameInputController.java`: Trigger undo snapshots before processing actions.
