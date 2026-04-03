# Phase 9: Advanced Mechanics - Research

**Researched:** 2026-04-03
**Domain:** Railway infrastructure system — tile-based connectivity, ShapeRenderer rendering, pathfinding cost modification
**Confidence:** HIGH (mechanics, rendering approach confirmed against codebase)

---

## Summary

Phase 9 adds a Railway system inspired by Polytopia roads. The core mechanic is simple: railway tiles halve the movement cost for land units traversing them, effectively doubling their mobility range across connected rail networks. The hard parts are not the mechanic itself — they are (1) the adjacency sprite system that makes rails look visually connected, and (2) integrating the cost reduction into the existing flood-fill BFS without breaking the current step-count model.

The existing codebase uses integer move steps (units have `stats.move = 1`, `2`, `3`, etc.) and a recursive `floodFill` that decrements by 1 per tile. Railways require fractional costs (0.5 per rail tile), which means the flood-fill must either switch to float arithmetic or scale everything by ×2. Scaling by ×2 is the safer approach — no floating-point ambiguity in BFS.

Rails are rendered using `ShapeRenderer` — the same approach already used for territory borders and the selection indicator in `MapRenderSystem`. This means **zero art assets are required**. Rail connectivity is drawn procedurally: for each rail tile, check which of the 4 cardinal neighbors also have rail, then draw `rectLine` segments from the tile center to each connected edge midpoint. Color: steel gray (`0.6, 0.6, 0.6, 1.0`).

**Primary recommendation:** Store rail state in `GameMap` (a `boolean[][] rails` layer), draw rail lines procedurally in `renderBordersPass()` using the existing `ShapeRenderer`, and modify `floodFill` to use ×2 scaling when crossing a rail tile. No sprite sheet, no art assets needed.

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

| Question | Polytopia Answer | Militopia Decision (confirmed) |
|----------|------------------|-------------------------------|
| Who builds? | Any city action in your territory | Player action via SlideMenu |
| Build cost | 3 stars | **+3 funding** |
| Both-tile requirement? | Yes — both origin AND destination must be rail | **Yes** — preserve this; avoids rail exploitation |
| Enemy territory? | No bonus | **No bonus** (same) |
| Move type restriction? | All land units | **LAND MoveType only** — AIR/SEA unaffected |
| Cost model | 0.5 per rail-to-rail step | 0.5 per rail-to-rail step (×2 integer scaling) |
| Placement terrain | No mountains/water | No mountains/water/deep water |
| Rendering approach | Sprite tiles | **ShapeRenderer lines** — steel gray, no art assets |

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
│   └── MapRenderSystem.java     # Add: drawRailPass() called inside renderBordersPass()
├── controller/
│   └── GameInputController.java # Modify: floodFill() to use ×2 scaling for rail cost
└── ui/
    └── SlideMenu.java (or GameHUD) # Add: "Build Railway" action button
```

### Pattern 1: ShapeRenderer Rail Rendering

**What:** Rails are drawn procedurally using `ShapeRenderer` — no sprite assets needed. For each rail tile, draw `rectLine` segments from the tile's isometric center to the midpoint of each connected edge. The existing `drawSmartBorders` method in `MapRenderSystem` uses the exact same technique for territory borders.

**Color:** Steel gray — `shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1.0f)`

**Isometric geometry for a rail tile (matching existing coord math in MapRenderSystem):**

```
Tile diamond corners:       Rail draws to edge MIDPOINTS:
  top (centerX, cy+halfH)     NE edge mid = (cx+halfW/2, cy+halfH/2)
  right (cx+halfW, cy)        SE edge mid = (cx+halfW/2, cy-halfH/2)
  bot (centerX, cy-halfH)     SW edge mid = (cx-halfW/2, cy-halfH/2)
  left (cx-halfW, cy)         NW edge mid = (cx-halfW/2, cy+halfH/2)

Rail line = center → each edge midpoint where neighbor has rail
```

**Implementation — added to `renderBordersPass()` inside the existing ShapeRenderer begin/end block:**

```java
private void drawRailPass() {
    float thick = 2.5f;
    float jointSize = thick / 2f;
    shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1.0f); // steel gray

    for (int x = gameMap.width - 1; x >= 0; x--) {
        for (int y = gameMap.height - 1; y >= 0; y--) {
            if (!gameMap.rails[x][y]) continue;
            if (fogEnabled && !gameMap.visibleTiles[x][y]) continue;

            float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
            float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
            float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
            float cx = isoX - xOffset + (GameConfig.DRAW_WIDTH / 2f);
            float cy = isoY + 10f; // surfaceLift matches existing code
            float halfW = GameConfig.TILE_WIDTH / 2.0f;
            float halfH = GameConfig.TILE_HEIGHT / 2.0f;

            // Edge midpoints (isometric: NE, SE, SW, NW)
            float neX = cx + halfW / 2f, neY = cy + halfH / 2f; // grid +X neighbor direction
            float seX = cx + halfW / 2f, seY = cy - halfH / 2f; // grid -Y neighbor direction
            float swX = cx - halfW / 2f, swY = cy - halfH / 2f; // grid -X neighbor direction
            float nwX = cx - halfW / 2f, nwY = cy + halfH / 2f; // grid +Y neighbor direction

            // Draw center dot
            shapeRenderer.circle(cx, cy, jointSize);

            if (hasRail(x + 1, y)) { // East neighbor (visual NE)
                shapeRenderer.rectLine(cx, cy, neX, neY, thick);
                shapeRenderer.circle(neX, neY, jointSize);
            }
            if (hasRail(x, y - 1)) { // South neighbor (visual SE)
                shapeRenderer.rectLine(cx, cy, seX, seY, thick);
                shapeRenderer.circle(seX, seY, jointSize);
            }
            if (hasRail(x - 1, y)) { // West neighbor (visual SW)
                shapeRenderer.rectLine(cx, cy, swX, swY, thick);
                shapeRenderer.circle(swX, swY, jointSize);
            }
            if (hasRail(x, y + 1)) { // North neighbor (visual NW)
                shapeRenderer.rectLine(cx, cy, nwX, nwY, thick);
                shapeRenderer.circle(nwX, nwY, jointSize);
            }
        }
    }
}

private boolean hasRail(int x, int y) {
    if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) return false;
    return gameMap.rails[x][y];
}
```

**Call site in `renderBordersPass()`** — after `drawSmartBorders`, before `shapeRenderer.end()`:

```java
drawRailPass(); // NEW — inside existing begin/end block, after territory borders
renderSelectionIndicator();
shapeRenderer.end();
```

**No Z-ordering issues:** `renderBordersPass()` runs in the same ShapeRenderer pass as territory and selection. Units are drawn separately by `UnitRenderSystem` (priority=1). Rails are correctly beneath units.

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
| Rail visuals | Sprite sheet with 16 tile variants | `ShapeRenderer.rectLine()` — same as territory borders, zero art assets |
| Rail persistence | New file format or separate save file | Add `boolean[][] railGrid` to existing `GameState` JSON via SaveManager |
| Rail build action UI | New UI system | Reuse `SlideMenu`/`StructurePlacementSystem` pattern from Phase 5 |
| Movement cost math | Float arithmetic in BFS | ×2 integer scaling (no floats in pathfinding) |

**Key insight:** ShapeRenderer is already present and initialized in `MapRenderSystem`. Drawing rail lines is a 20-line method that reuses the exact same geometry helpers as `drawSmartBorders`. No texture atlas, no asset pipeline, no artist needed.

---

## Common Pitfalls

### Pitfall 1: Both-Tile Requirement Forgotten
**What goes wrong:** Developer applies rail bonus whenever the destination tile has a rail, ignoring whether the source tile has one. Units can then exploit a single rail tile to leap into it from any direction at half cost.
**Why it happens:** The intuitive model is "step onto a rail = get the bonus." Polytopia's actual rule is stricter: both the tile you're leaving AND the tile you're entering must be rail.
**How to avoid:** In `getRailStepCost`, check `gameMap.rails[fromX][fromY]` AND `gameMap.rails[toX][toY]`. Both must be true.
**Warning signs:** A unit standing adjacent to a lone rail tile gains unexpected extra reach.

### Pitfall 2: ShapeRenderer Rail Direction Mismatch With Isometric Grid
**What goes wrong:** The edge midpoint coordinates are computed using screen-space assumptions (visual N/S/E/W) rather than grid-space neighbors. A rail connecting grid-east neighbor (+X) must draw toward the visual NE edge midpoint, not the screen-right.
**Why it happens:** Isometric projection rotates the grid 45°. Grid +X = visual NE, grid +Y = visual NW, grid -X = visual SW, grid -Y = visual SE.
**How to avoid:** Comment each neighbor check with both its grid direction AND its visual direction (see Pattern 1 code). Match the edge midpoint formula to the correct visual direction.
**Warning signs:** Rail lines visually point in the wrong diagonal when only one neighbor is connected.

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

### Rail Rendering via ShapeRenderer (no assets required)

See full implementation in **Pattern 1** above (`drawRailPass()`). Summary:

```java
// Steel gray, reuses existing ShapeRenderer in MapRenderSystem
shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1.0f);
// For each connected neighbor: rectLine(center → edge midpoint) + circle joint
// Called inside renderBordersPass() — same begin/end block as territory + selection
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
