# Phase 5: Specialized Structures - Research

**Researched:** 2026-02-23
**Domain:** Building Construction & Specialized Mechanics
**Confidence:** MEDIUM

## Summary
The investigation into Phase 5 (Specialized Structures) focuses on implementing a robust construction system that handles terrain-specific validation (e.g., Ports on coastlines), domain-specific summoning (Sea units), and a variety of per-turn building bonuses (XP, Income, Healing).

**Primary recommendation:** Implement a `PlacementSystem` within the Ashley ECS to centralize validation logic (territory, terrain, and radius checks) and use "Ghost Placement" visuals in the `GameInputController` to improve user experience.

---

## User Constraints
- **Ports:** Must be built on water/coastal tiles; Sea units (Gunboat, Destroyer, Carrier) spawn directly on them.
- **Parent Linking:** Specialized structures must link to a parent base to contribute to that base's XP growth.
- **Unlocks:** Port is strictly unlocked at Base Level 2.
- **Structure List:** Munition Factory, Solar Array, Oil Derrick, Nuclear Plant, Port, Hospital, Radar, Jammer.
- **Base Leveling:** Structures contribute `xpGain` per turn to their parent base.

---

## Standard Stack
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| libGDX | 1.12.x+ | Game Engine | Project core |
| Ashley | 1.7.x+ | ECS Framework | Used for all game logic/entities |
| Scene2D.ui | N/A | UI/HUD | Standard libGDX UI library for menus |

---

## Architecture Patterns

### Recommended Project Structure
- `core/src/.../systems/PlacementSystem.java`: Handles validation logic for building placement.
- `core/src/.../factories/BuildingFactory.java`: Extension of `EntityFactory` for structure-specific components.
- `core/src/.../ui/BuildMenu.java`: Sub-panel for the `GameHUD`.

### Pattern: Ghost Placement
**What:** Render a semi-transparent building at the cursor position during the "Build Mode".
**Validation:** The ghost turns red/green based on `PlacementSystem` feedback (Terrain type + Territory radius).

### Pattern: Domain-Specific Summoning
**What:** The `SummonSystem` must check the `Domain` of the unit being summoned. 
- `LAND`/`AIR` -> Valid at `BASE`.
- `SEA` -> Valid only at `PORT`.

---

## Don't Hand-Roll
| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Collision/Stacking | Custom physics | Grid check + `UnitOccupancyMap` | Simpler for hex/grid turn-based movement. |
| UI Transitions | Manual timers | Scene2D `Actions` | Built-in support for fades/slides. |

---

## Common Pitfalls
1. **Z-Order/Rendering:** Buildings must be at `Z=1` (above terrain, below units). If a Port is on water, ensure the unit (Z=3) renders clearly on top.
2. **Economic Imbalance:** The Nuclear Plant (+15 income) can end games quickly.
3. **Ghost Cleanup:** Failing to remove the ghost building if the player cancels the build action.

---

## Code Examples (Conceptual)
```java
// Logic for Coastline Validation
public boolean isCoastal(int x, int y) {
    Tile tile = map.getTile(x, y);
    if (tile.type != TileType.WATER && tile.type != TileType.DEEP_WATER) return false;
    for (Tile neighbor : getNeighbors(x, y)) {
        if (neighbor.isLand()) return true;
    }
    return false;
}
```

## Open Questions
1. **Asset Verification:** Are the textures for the specialized structures already present in `assets/`? This needs to be checked before implementation. (Checked: Yes, they are in AssetManager and UnitFactory).
2. **Port Stacking:** If a Sea unit is summoned while a unit is already on the Port, does the summon fail or does it push the unit out? (Decision: Summoning should fail if tile is occupied).
