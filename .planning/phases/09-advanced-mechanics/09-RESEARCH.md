# Phase 9: Advanced Mechanics - Research

**Researched:** 2026-04-03
**Domain:** Railway infrastructure system — tile-based connectivity, adjacency sprite rendering, pathfinding cost modification
**Confidence:** HIGH (mechanics), MEDIUM (art pipeline specifics)

---

## Summary

Phase 9 adds a Railway system inspired by Polytopia roads. The core mechanic is simple: railway tiles halve the movement cost for land units traversing them, effectively doubling their mobility range across connected rail networks. The hard parts are not the mechanic itself — they are (1) the adjacency sprite system that makes rails look visually connected, and (2) integrating the cost reduction into the existing flood-fill BFS without breaking the current step-count model.

The existing codebase uses integer move steps (units have `stats.move = 1`, `2`, `3`, etc.) and a recursive `floodFill` that decrements by 1 per tile. Railways require fractional costs (0.5 per rail tile), which means the flood-fill must either switch to float arithmetic or scale everything by ×2. Scaling by ×2 is the safer approach — no floating-point ambiguity in BFS.

The adjacency sprite system uses a 4-bit bitmask over the four cardinal directions (N/S/E/W in grid space). With 16 possible variants, this is the minimum sprite set needed. For an isometric game at Militopia's scale (TILE_WIDTH=16, TILE_HEIGHT=10), rail overlays drawn on top of terrain tiles during the map render pass is the correct approach — no Z-ordering issues because rails are ground-plane decorations, not elevated objects.

**Primary recommendation:** Store rail state in `GameMap` (a `boolean[][] rails` layer), compute 4-bit bitmask per tile on render, draw the correct rail sprite overlay in `MapRenderSystem.drawTerrainTile()`, and modify `floodFill` to subtract 0.5 (or use ×2 scaling) when crossing a rail tile.

---

## Polytopia Road Mechanics

This section documents Polytopia roads precisely so the Railway adaptation is grounded in verified behavior, not assumption.

### Core Rules (MEDIUM confidence — wiki + multiple community sources, fandom wiki returned 403)

| Rule | Detail |
|------|--------|
| Movement cost baseline | Every tile costs 1 move point |
| Road tile cost | Moving FROM a road tile TO another road tile (or city) costs 0.5 moves instead of 1 |
| Both tiles must be road | The bonus only applies when BOTH the origin tile AND the destination tile are road/city tiles |
| Final tile exemption | The unit does NOT need a road on the tile it ends on — only the tiles it passes through |
| Terrain override | Roads override Forest's +1 movement penalty — but NOT Zone of Control |
| Territory restriction | Roads only grant bonuses in your own territory or neutral territory — NOT in enemy territory |
| Fractional rounding | The game rounds UP at the end: 0.5 remaining moves can still enter a 1-cost tile |
| Who builds | Any player can build roads; any player can use roads in friendly or neutral territory |
| Placement restriction | Cannot place on: Mountains, Water, Ice, Algae |
| City tiles act as roads | Cities and villages function as road endpoints automatically |
| Bridges | Roads tech unlocks bridges across 1-tile water gaps (costs 7 stars) |
| Cost to build | 3 stars per road tile |

### How Polytopia Railways Differ (Militopia Adaptation)

Militopia's "Railway" is a renamed, adapted version. Key design decisions to make:

| Question | Polytopia Answer | Recommended Militopia Answer |
|----------|------------------|------------------------------|
| Who builds? | Any city action in your territory | Player action via SlideMenu, costs resources, land only |
| Both-tile requirement? | Yes — both origin AND destination must be rail | Yes — preserve this; avoids rail exploitation |
| Enemy territory? | No bonus | No bonus (same) |
| Move type restriction? | All land units | LAND MoveType only; AIR/SEA unaffected |
| Cost model | 0.5 per rail-to-rail step | 0.5 per rail-to-rail step (same) |
| Placement terrain | No mountains/water | No mountains/water/deep water |

---

## Standard Stack

### Core (no new libraries needed)

| Component | Version | Purpose |
|-----------|---------|---------|
| libGDX SpriteBatch | 1.14.0 (existing) | Draw rail overlay sprites per tile |
| Ashley ECS | 1.7.4 (existing) | No new systems required for MVP |
| Java 8 | existing | All implementation language |

**No new dependencies.** Railway is a pure data + render + logic feature using existing infrastructure.

### Installation

```bash
# No new packages — railway is implemented entirely within existing tech stack
```

---

## Architecture Patterns

### Data Layer: Map-Layer Storage

Rails are stored as a layer on `GameMap`, not as ECS entities. This matches how `terrain[][]` and `objects[][]` work. Rails are ground decorations, not entities.

**Add to `MapGenerator.GameMap`:**

```java
// Source: mirrors existing visibleTiles and detectedTiles pattern
public boolean[][] rails;  // true = this tile has a railway

public GameMap(int w, int h) {
    // existing fields...
    rails = new boolean[w][h];
}
```

**Also add to `MapGenerator.ObjectType` if rails need save/load via object layer — OR** handle rails as a separate boolean layer in `GameState`/`UnitSnapshot` serialization. The latter is cleaner because rails are infrastructure state, not objects with HP or ownership.

### Persistence: GameState Extension

`SaveManager` serializes `GameState`. Rails need to survive save/load.

```java
// In GameState.java — add field
public boolean[][] railGrid;  // serialized as a flat array or 2D JSON array
```

### Recommended Project Structure (additions only)

```
core/src/main/java/com/militopia/
├── map/
│   └── MapGenerator.java        # Add: boolean[][] rails to GameMap
├── data/
│   └── GameState.java           # Add: boolean[][] railGrid for persistence
├── systems/
│   └── MapRenderSystem.java     # Add: drawRailOverlay(x, y, ...) call in drawTerrainTile()
├── controller/
│   └── GameInputController.java # Modify: floodFill() to use ×2 scaling for rail cost
└── ui/
    └── SlideMenu.java (or GameHUD) # Add: "Build Railway" action button
```

### Pattern 1: 4-Bit Bitmask for Rail Sprites

**What:** Each rail tile computes a 4-bit value based on whether its 4 cardinal neighbors also have rails. This value (0–15) indexes into a 16-sprite rail sprite sheet.

**Bitmask bit assignment (cardinal grid directions):**

```
North (+Y) = bit 3 = value 8
East  (+X) = bit 2 = value 4
South (-Y) = bit 1 = value 2
West  (-X) = bit 0 = value 1
```

**Implementation:**

```java
// Source: "Tile Bitmasking" technique, verified across multiple sources
// Called from MapRenderSystem.drawTerrainTile()
private int computeRailMask(int x, int y) {
    int mask = 0;
    if (hasRail(x, y + 1)) mask |= 8; // North
    if (hasRail(x + 1, y)) mask |= 4; // East
    if (hasRail(x, y - 1)) mask |= 2; // South
    if (hasRail(x - 1, y)) mask |= 1; // West
    return mask; // 0..15
}

private boolean hasRail(int x, int y) {
    if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) return false;
    return gameMap.rails[x][y];
}
```

**Sprite variants needed:** 16 TextureRegions from a sprite sheet. Index them as `railRegions[mask]`.

**When to use:** Always when `gameMap.rails[x][y] == true`.

**Example mask meanings:**

| Mask | Binary | Shape |
|------|--------|-------|
| 0 | 0000 | Isolated dot (dead end, no neighbors) |
| 5 | 0101 | N–S straight (connects North and South) |
| 10 | 1010 | E–W straight (connects East and West) |
| 3 | 0011 | S–W corner (South and West) |
| 15 | 1111 | Cross junction (all 4 directions) |

### Pattern 2: Rail Rendering — Overlay in drawTerrainTile()

Rails are drawn on top of the terrain tile, below units. They should be drawn AFTER the terrain base sprite and BEFORE entity rendering. MapRenderSystem's `drawTerrainTile()` is the correct insertion point.

```java
// In MapRenderSystem.drawTerrainTile(), after drawing the terrain region:
if (!isFog && gameMap.rails[x][y]) {
    int mask = computeRailMask(x, y);
    TextureRegion railRegion = railRegions[mask]; // array of 16 regions
    if (railRegion != null) {
        batch.draw(railRegion, drawX, drawY + animY, drawW, drawH);
    }
}
```

**No Z-ordering issues:** Rail overlays are ground-plane sprites drawn in the same map pass. Units are drawn in a separate pass by `UnitRenderSystem` with priority=1 (map has priority=0). Rails render correctly beneath units automatically.

### Pattern 3: Movement Cost Modification — ×2 Scaling

The existing `floodFill` uses integer arithmetic (`remainingMoves` decremented by 1 per step). Introducing float 0.5 cost creates floating-point comparison bugs. Use ×2 integer scaling instead.

**Approach:**

- Unit's `stats.move` is scaled ×2 before entering `floodFill` (e.g., Tank move=2 becomes 4 in the flood-fill budget)
- Non-rail steps cost 2 (instead of 1)
- Rail-to-rail steps cost 1 (instead of 0.5)
- This is pure integer arithmetic — no floats in BFS

**Modified floodFill signature (conceptual):**

```java
// Called with: floodFill(startX, startY, stats.move * 2, ...)
// Cost deducted per step:
//   - normal tile: cost 2
//   - current tile AND destination tile both have rail: cost 1

private void floodFill(int x, int y, int budget, int[][] visited,
        int startX, int startY, StatsComponent.MoveType moveType, UnitType unitType) {
    if (budget < 0) return;
    if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) return;
    if (visited[x][y] >= budget) return;

    boolean isStart = (x == startX && y == startY);
    if (!isStart && !isWalkable(x, y, moveType, unitType)) return;

    visited[x][y] = budget;
    if (!isStart && getEntityAt(x, y, TypeComponent.Type.MARKER) == null) {
        entityFactory.createMovementMarker(x, y);
    }

    // Rail bonus: if both current and neighbor have rail (and unit is LAND)
    // use cost 1; otherwise use cost 2
    for each neighbor (nx, ny):
        int stepCost = getRailStepCost(x, y, nx, ny, moveType);
        floodFill(nx, ny, budget - stepCost, visited, startX, startY, moveType, unitType);
}

private int getRailStepCost(int fromX, int fromY, int toX, int toY, MoveType moveType) {
    if (moveType == MoveType.LAND
            && gameMap.rails[fromX][fromY]
            && toX >= 0 && toX < gameMap.width
            && toY >= 0 && toY < gameMap.height
            && gameMap.rails[toX][toY]) {
        return 1; // half cost (scaled)
    }
    return 2; // normal cost (scaled)
}
```

**Why this is correct:** A unit with move=1 gets budget=2. Normal step costs 2, so they reach 1 tile. Two consecutive rail steps cost 1+1=2, so they also reach 2 tiles total. A unit with move=2 gets budget=4; crossing 4 consecutive rail tiles costs 4×1=4 exactly, reaching 4 tiles.

**Enemy territory rule:** Add a check in `getRailStepCost`: if the from-tile is in enemy territory (owned by the opposing player), don't apply the rail bonus — return 2 instead of 1.

### Pattern 4: Building Railway via SlideMenu

Rails are built by a player action. The action should be available when:
1. A land tile is selected
2. It is the current player's turn
3. The tile is in the player's territory (or neutral territory — design decision)
4. No existing rail on that tile
5. Player has sufficient funds (design decision: suggest 3 stars, matching Polytopia)

This plugs into the existing SlideMenu/GameHUD action system (`StructurePlacementSystem` pattern).

### Anti-Patterns to Avoid

- **Storing rails as ECS entities:** Rails have no HP, no owner, no turn state. They are permanent terrain-layer features. Storing them as entities inflates the entity pool, complicates save/load, and adds Z-ordering complexity. Use `boolean[][] rails` on `GameMap`.
- **Using float arithmetic in floodFill:** Float comparisons in BFS (`visited[x][y] >= remainingMoves`) are unreliable. Use ×2 integer scaling.
- **Recomputing rail bitmasks every frame:** `computeRailMask` is called per-tile per-frame in the render loop. It's only 4 array lookups — this is fine. Do NOT cache bitmasks in a separate array unless profiling shows a problem.
- **Drawing rails in UnitRenderSystem:** Rail is terrain, not an entity. Drawing it in the entity pass causes Z-order problems with entities that sit on rail tiles.
- **Applying rail bonus to AIR/SEA units:** Rail is physical infrastructure. Only `MoveType.LAND` units should receive the cost bonus.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead |
|---------|-------------|-------------|
| Adjacency sprite selection | Custom per-sprite if/else chain | 4-bit bitmask → array index (16 entries, O(1)) |
| Rail persistence | New file format or separate save file | Add `boolean[][] railGrid` to existing `GameState` JSON via SaveManager |
| Rail build action UI | New UI system | Reuse `SlideMenu`/`StructurePlacementSystem` pattern from Phase 5 |
| Movement cost math | Float arithmetic in BFS | ×2 integer scaling (no floats in pathfinding) |

**Key insight:** The adjacency sprite system feels complex but reduces to a 4-line bitmask computation and a 16-element array lookup. Any if/else approach to the same problem will have 16+ cases and be impossible to maintain.

---

## Common Pitfalls

### Pitfall 1: Both-Tile Requirement Forgotten
**What goes wrong:** Developer applies rail bonus whenever the destination tile has a rail, ignoring whether the source tile has one. Units can then exploit a single rail tile to leap into it from any direction at half cost.
**Why it happens:** The intuitive model is "step onto a rail = get the bonus." Polytopia's actual rule is stricter: both the tile you're leaving AND the tile you're entering must be rail.
**How to avoid:** In `getRailStepCost`, check `gameMap.rails[fromX][fromY]` AND `gameMap.rails[toX][toY]`. Both must be true.
**Warning signs:** A unit standing adjacent to a lone rail tile gains unexpected extra reach.

### Pitfall 2: Bitmask Direction Mismatch With Isometric Grid
**What goes wrong:** The bitmask is computed in grid coordinates (x, y), but the sprite sheet was authored expecting screen-space directions (visual north/south/east/west). In isometric projection, grid +X goes to the lower-right visually and grid +Y goes to the lower-left.
**Why it happens:** Artist draws sprites with "visual north" at screen top; code computes bitmask in grid space where north is +Y.
**How to avoid:** Define the bitmask convention ONCE in comments and use it consistently in both the art brief and the code. Isometric grid conventions: grid +Y = visual NW, grid +X = visual NE, grid -Y = visual SE, grid -X = visual SW.
**Warning signs:** Rail sprites connect visually in the wrong direction (a horizontal-looking straight rail connects grid-diagonally).

### Pitfall 3: Save/Load Rails Not Persisted
**What goes wrong:** Built rails disappear after saving and reloading the game because `GameState` doesn't include the rail grid.
**Why it happens:** Developer adds `rails` to `GameMap` but forgets to serialize it to `GameState` and deserialize it in `SaveManager`.
**How to avoid:** Add `boolean[][] railGrid` to `GameState` and wire it in `SaveManager` alongside the existing unit/structure snapshots. Test by building a rail, saving, loading, and verifying the rail is still visible.
**Warning signs:** Rails exist in play but disappear after reload.

### Pitfall 4: floodFill ×2 Scaling Not Applied to Attack Markers
**What goes wrong:** Attack markers use the movement flood-fill result to determine attack range from reachable tiles. If `stats.move` is passed as-is (unscaled) to some paths but ×2 to others, the reach calculation is inconsistent.
**How to avoid:** The ×2 scaling only applies to the move budget entering floodFill. The attack-range loop is separate (it checks `Math.max(Math.abs(dx), Math.abs(dy)) > atkRange`) and does not need scaling changes.

### Pitfall 5: Rail Build Allowed on Water/Mountain
**What goes wrong:** Player places a railway on water or a mountain tile, causing visual absurdity and potential logic errors (rail on impassable terrain is useless and misleading).
**Why it happens:** Build validation not checking terrain type.
**How to avoid:** In the build action handler, check `gameMap.terrain[x][y]` is not `WATER`, `DEEP_WATER`, or `MOUNTAIN`. Rail on SAND is allowed (desert rail exists in real life and Polytopia).

### Pitfall 6: Rail Bonus Applied in Enemy Territory
**What goes wrong:** A unit moving through captured enemy rails still gets the 0.5x cost benefit. This is a significant balance issue — enemies should not benefit from your rail network during an invasion.
**How to avoid:** In `getRailStepCost`, check territory ownership before applying the bonus. Use the same `getTileOwner` logic from `MapRenderSystem`.

---

## Code Examples

### 4-Bit Bitmask Computation (Verified Pattern)

```java
// Source: "Tile Bitmasking" technique — N=8, E=4, S=2, W=1 convention
// Standard across all strategy game autotiling implementations

private int computeRailMask(int x, int y, GameMap map) {
    int mask = 0;
    if (hasRail(x,     y + 1, map)) mask |= 8; // North (grid +Y)
    if (hasRail(x + 1, y,     map)) mask |= 4; // East  (grid +X)
    if (hasRail(x,     y - 1, map)) mask |= 2; // South (grid -Y)
    if (hasRail(x - 1, y,     map)) mask |= 1; // West  (grid -X)
    return mask;
}

private boolean hasRail(int x, int y, GameMap map) {
    if (x < 0 || x >= map.width || y < 0 || y >= map.height) return false;
    return map.rails[x][y];
}
```

### Rail Sprite Loading

```java
// In UnitFactory or AssetManager initialization — mirrors existing texture loading
TextureRegion[] railRegions = new TextureRegion[16];
// Assumes a 4x4 sprite sheet with tiles ordered by bitmask value 0..15
Texture railSheet = game.assets.get("rail_tiles.png", Texture.class);
int tileW = 18, tileH = 20; // match DRAW_WIDTH/DRAW_HEIGHT
for (int i = 0; i < 16; i++) {
    int col = i % 4;
    int row = i / 4;
    railRegions[i] = new TextureRegion(railSheet, col * tileW, row * tileH, tileW, tileH);
}
```

### ×2 Integer Scaling in floodFill

```java
// In GameInputController.showRangeMarkers():
int moveRange = (stats != null) ? stats.move : 3;
// ... existing skirmish cap logic ...

// Scale by 2 for rail cost model
int moveBudget = moveRange * 2;

int[][] visitedMoves = new int[gameMap.width][gameMap.height];
for (int[] row : visitedMoves) java.util.Arrays.fill(row, -1);
floodFill(startX, startY, moveBudget, visitedMoves, startX, startY, moveType, stats.unitType);

// In floodFill — changed parameter name from remainingMoves to budget
// Normal step cost: 2
// Rail-to-rail step cost: 1 (land units only, both tiles must be rail)
```

### GameMap Extension

```java
// In MapGenerator.GameMap constructor:
public boolean[][] rails;

public GameMap(int w, int h) {
    this.width = w;
    this.height = h;
    terrain = new TerrainType[w][h];
    objects = new ObjectType[w][h];
    visibleTiles = new boolean[w][h];
    detectedTiles = new boolean[w][h];
    rails = new boolean[w][h];   // NEW
}
```

---

## Environment Availability

Step 2.6: SKIPPED (no external dependencies — railway is a pure in-engine feature using existing Java/libGDX/Ashley stack)

---

## Validation Architecture

CLAUDE.md states: "No test suite exists — manual play-testing is the verification method."

No automated test framework is present. All verification for Phase 9 is manual play-testing:

| Behavior | Verification Method |
|----------|-------------------|
| Rail tile renders with correct connectivity sprite | Visual check in-game |
| Rail bonus doubles effective movement range | Move a unit across 4+ rail tiles, confirm reach |
| Both-tile requirement enforced | Place single isolated rail; confirm no bonus from adjacent non-rail |
| Rails persist after save/load | Build rails, save, reload, confirm still visible |
| AIR/SEA units unaffected | Move helicopter across rail; confirm no range increase |
| Enemy territory: no bonus | Move onto enemy-owned rails; confirm normal movement cost |
| Cannot build on water/mountain | Attempt build; confirm action blocked |

---

## Sources

### Primary (HIGH confidence)
- Polytopia community wiki summaries (multiple queries verified consistent answers): Roads cost 3 stars, both tiles must be rail for 0.5x bonus, cannot place on mountains/water, cities count as road endpoints, no bonus in enemy territory
- Bitmask autotiling technique: Verified across multiple independent sources (Envato Tuts+, Chris Hammond blog, Excalibur.js docs, GameMaker forum, Godot forum) — all agree on N=8/E=4/S=2/W=1 convention and 16-variant sprite count

### Secondary (MEDIUM confidence)
- Polytopia wiki (polytopia.fandom.com) — returned 403 at research time, but content cross-verified via Google cache summaries and community guides
- Fractional rounding rule (0.5 remaining can enter a 1-cost tile) — cited in Polytopia Hints & Tips Google Doc, not independently verified against game code

### Tertiary (LOW confidence)
- Precise Polytopia bridge rules (7 star cost, 1-tile water gap) — single source, not relevant to Militopia adaptation

---

## Metadata

**Confidence breakdown:**
- Polytopia road mechanics: MEDIUM — wiki 403'd, verified via community guides; core rules (both-tile, 0.5x, territory restriction) are consistent across 4+ independent sources
- 4-bit bitmask pattern: HIGH — verified across 5+ independent sources, has a Java reference implementation
- ECS integration approach: HIGH — directly verified against existing codebase source (floodFill, MapRenderSystem, GameMap)
- ×2 integer scaling approach: HIGH — derived from first principles; avoids float comparison bugs in BFS

**Research date:** 2026-04-03
**Valid until:** 2026-05-03 (stable topic — Polytopia road mechanics and autotiling are not changing)
