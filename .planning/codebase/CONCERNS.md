# Codebase Concerns

_Last updated: 2026-03-26_

## Summary

Militopia is in active early development with a healthy ECS foundation, but several structural and gameplay concerns exist. The biggest risks are hardcoded unit/structure stats scattered across large switch blocks, a brittle linear-scan entity lookup pattern used throughout instead of spatial indexing, and a save format that embeds raw Java enum names making save migration very fragile. AI (single-player) is fully absent.

---

## Tech Debt

**Unit and structure stats are hardcoded in giant switch blocks:**
- Issue: All unit stat definitions live in a single `switch` in `UnitFactory.createUnit()` and a parallel switch in `getUnitCost()`. Adding or rebalancing a unit requires touching multiple methods in sync, and costs can drift out of sync.
- Files: `core/src/main/java/com/militopia/factories/UnitFactory.java` (lines 250–440)
- Impact: Easy to introduce bugs where a unit's displayed cost differs from its actual deduction. Structure niceName mapping uses yet another block of `if/equals` strings in `createStructure()`.
- Fix approach: Extract unit definitions to a data class or JSON config loaded at startup. `UnitFactory` becomes a lookup rather than a stats factory.

**Structure name matching uses raw string contains/equals throughout:**
- Issue: Indestructible check (`dStats.name.contains("Oil Derrick") || dStats.name.contains("Nuclear Plant")`), hospital check (`sStats.name.equals("Field Hospital")`), base detection (`stats.name.contains("Base")`), solar array check (`stats.name.contains("Solar Array")`) — all rely on human-readable display strings, not a type key.
- Files: `core/src/main/java/com/militopia/systems/CombatSystem.java` (lines 86, 314, 363), `core/src/main/java/com/militopia/systems/StructureEconomySystem.java` (line 133), `core/src/main/java/com/militopia/screen/GameScreen.java` (lines 457, 489, 514)
- Impact: Renaming a structure display name silently breaks game logic. `unitTypeKey` exists on `StatsComponent` but is not consistently used for these checks.
- Fix approach: Use `stats.unitTypeKey` for all logic branching; reserve `stats.name` for display only.

**Linear O(n) entity scans for position lookup are pervasive:**
- Issue: `findEntityAt()` iterates all entities every call. Called from `StructureEconomySystem.findEntityAt()`, `GameScreen.findEntityAt()`, `GameScreen.findUnitAt()`, `GameScreen.undoTurn()` (nested loop), and `GameScreen.calculateGroupedBaseIncome()`.
- Files: `core/src/main/java/com/militopia/screen/GameScreen.java` (lines 241–266, 464–475, 519–533), `core/src/main/java/com/militopia/systems/StructureEconomySystem.java` (lines 197–208)
- Impact: Performance degrades with entity count. Particularly noticeable in `undoTurn()` which runs nested scans to restore structures.
- Fix approach: Maintain a `Map<GridPoint2, Entity>` spatial index updated on entity add/remove.

**`TurnHistoryManager.push()` has an O(n) trim algorithm:**
- Issue: When the history stack exceeds 50 entries, it converts to an array, clears the deque, and rebuilds it. This runs every push after the cap is hit.
- Files: `core/src/main/java/com/militopia/managers/TurnHistoryManager.java` (lines 20–28)
- Impact: Minor performance issue but incorrect use of `ArrayDeque` — a simple `pollLast()` would trim in O(1).
- Fix approach: Replace the manual trim with `stack.pollLast()` (removes oldest entry from tail of deque).

**`GameConfig.MAP_WIDTH` / `MAP_HEIGHT` are unused at runtime:**
- Issue: Map dimensions are driven by `GameState.mapWidth` / `mapHeight` set in `NewGameScreen`. `GameConfig.MAP_WIDTH = 16` and `MAP_HEIGHT = 16` exist but are not passed into `MapGenerator.generateMap()`.
- Files: `core/src/main/java/com/militopia/config/GameConfig.java` (lines 6–7)
- Impact: Config constants are misleading; actual map size is controlled elsewhere.
- Fix approach: Either remove the dead constants or enforce them as defaults in `NewGameScreen`.

**`TESTING_MODE` flag in `GameConfig` is unused:**
- Issue: `public static final boolean TESTING_MODE = false;` exists with no references in the codebase.
- Files: `core/src/main/java/com/militopia/config/GameConfig.java` (line 46)
- Fix approach: Remove or wire up to conditionally disable audio/rendering in headless tests.

**`AudioManager` is a singleton that never gets disposed on screen transitions:**
- Issue: `AudioManager.getInstance()` is a lazy singleton. `GameScreen.dispose()` does not call `AudioManager.getInstance().dispose()`. Sounds accumulate across game sessions within the same process.
- Files: `core/src/main/java/com/militopia/managers/AudioManager.java`, `core/src/main/java/com/militopia/screen/GameScreen.java` (lines 748–752)
- Fix approach: Call `AudioManager.getInstance().dispose()` in `GameScreen.dispose()`, or restructure as a managed resource passed via `MilitopiaGame`.

**Structure niceName mapping is duplicated between `UnitFactory` and a separate `createStructure` block:**
- Issue: `createStructure()` maps type keys to display names via a series of `if/equals` blocks. The same mapping exists implicitly in the HUD icon logic. No single source of truth.
- Files: `core/src/main/java/com/militopia/factories/UnitFactory.java` (lines 557–570)
- Fix approach: Centralize in a `Map<String, String>` constant or enum in `GameConfig` / a dedicated `StructureRegistry`.

---

## Known Bugs / Fragile Logic

**WRAITH unit type referenced but never defined:**
- Issue: `UnitFactory.createUnit()` checks `if (unitType.equals("WRAITH") || unitType.equals("SUBMARINE"))` to set `isCloaked = true`, but there is no "WRAITH" case in the unit switch block, no texture registered for it, and no cost defined.
- Files: `core/src/main/java/com/militopia/factories/UnitFactory.java` (line 398)
- Impact: If "WRAITH" is ever summoned via a save file or scavenge reward, it will silently fall back to a RECRUIT texture with broken stats.

**JUGGERNAUT, B2, SUBMARINE have cost = 0 — summoning gating is absent:**
- Issue: These units have `cost = 0` in `createUnit()` and `getUnitCost()`. They appear to be late-game unlocks gated by base level, but the port summon logic simply checks funding; a cost of 0 means they are always affordable.
- Files: `core/src/main/java/com/militopia/factories/UnitFactory.java` (lines 294–303, 334–343, 374–383)
- Impact: If summon UI ever lists them without a level gate they are free to produce every turn.

**Undo re-pushes the restored snapshot, preventing multi-step undo:**
- Issue: `undoTurn()` ends with `turnHistory.push(unitFactory.captureSnapshot(...))`, which re-pushes the state just restored. A second undo call returns to the same state rather than going back another turn.
- Files: `core/src/main/java/com/militopia/screen/GameScreen.java` (lines 447–448)
- Impact: Undo is effectively single-step only despite the 50-entry history stack.

**Save deserialization uses raw enum `valueOf()` for animal types without error handling:**
- Issue: `GameScreen` loads animals via `MapGenerator.ObjectType.valueOf(a.type)`. If a save file contains an animal type that no longer exists in the enum (after a rename/removal), the game throws `IllegalArgumentException` and crashes on load.
- Files: `core/src/main/java/com/militopia/screen/GameScreen.java` (line 133)
- Impact: Save files are fragile across enum changes.
- Fix approach: Wrap in try/catch and skip unknown animal types with a log warning.

**`SaveManager` uses `System.out.println` instead of `GameLogger`/`Gdx.app.log`:**
- Issue: `SaveManager.saveGame()` uses `System.out.println("Game Saved: " + file.path())`.
- Files: `core/src/main/java/com/militopia/managers/SaveManager.java` (line 76)
- Fix approach: Replace with `Gdx.app.log("SaveManager", ...)`.

---

## Missing Features / Incomplete Systems

**No AI / single-player mode:**
- Issue: The game is two-player only. `GameState` tracks `currentPlayer` (1 or 2) but there is no AI system, no turn automation, and no difficulty setting.
- Files: All systems assume human input via `GameInputController`.
- Impact: Game is only playable as hotseat multiplayer.

**No in-game settings / volume controls accessible during gameplay:**
- Issue: `AudioManager` has `setMasterVolume()` and a hardcoded `bgmVolume = 0.5f`, but there is no settings UI wired to these in `GameScreen` or any menu.
- Files: `core/src/main/java/com/militopia/managers/AudioManager.java` (lines 26–27, 119–124)

**No BGM during gameplay:**
- Issue: `AudioManager.playBGM()` exists and works, but no call site in `GameScreen` starts background music. SFX plays correctly; BGM is silent.
- Files: `core/src/main/java/com/militopia/screen/GameScreen.java`

**`MovementSystem` does not actually move entities — positional interpolation is missing:**
- Issue: `MovementSystem.processEntity()` tracks `move.time` and removes `MovementComponent` when complete, but never updates the entity's world position between start and end. The `startX/startY/targetX/targetY` on `MovementComponent` are written by `CombatSystem.triggerAttackAnimation()` as pixel offsets, but `MovementSystem` never reads them to interpolate.
- Files: `core/src/main/java/com/militopia/systems/MovementSystem.java`, `core/src/main/java/com/militopia/components/MovementComponent.java`
- Impact: The lunge animation in combat does not visually move the sprite; only the bounce effect in `UnitRenderSystem` produces movement feedback.

---

## Security Considerations

**Save files are plain-text JSON with no integrity check:**
- Issue: Save files in `saves/*.json` are written with `libGDX Json` and loaded without validation. A corrupt or manually edited save causes a crash or silent corruption.
- Files: `core/src/main/java/com/militopia/managers/SaveManager.java`, `core/src/main/java/com/militopia/screen/LoadGameScreen.java`
- Current mitigation: None. `LoadGameScreen` wraps the read in a try/catch per file.
- Recommendation: Add schema version field; validate required fields before constructing `GameScreen`.

---

## Test Coverage Gaps

**No tests for save/load round-trip:**
- What's not tested: `SaveManager.saveGame()` → `LoadGameScreen` deserialization → `GameScreen` restoration.
- Files: `core/src/test/java/com/militopia/systems/ExplorationPersistenceTest.java` tests map object persistence but not the full save file path.
- Risk: Regressions in serialization go undetected until manual playtest.
- Priority: High

**No tests for `UnitFactory.createUnit()` stat values:**
- What's not tested: Unit stat correctness (HP, ATK, DEF, range) for any unit type.
- Files: `core/src/test/java/com/militopia/systems/` — existing tests use units but assume correct stats.
- Risk: Stat rebalancing silently breaks gameplay without regression signal.
- Priority: Medium

**No tests for `StructurePlacementSystem.canBuild()` terrain rules:**
- What's not tested: Port placement requires coastal water; Nuclear Plant requires coastal land; OIL_DERRICK requires oil reservoir.
- Files: `core/src/main/java/com/militopia/systems/StructurePlacementSystem.java`
- Risk: Terrain constraint logic changes break valid/invalid build detection silently.
- Priority: Medium

**`FogSystem` has no test coverage:**
- What's not tested: Fog clearing radius, jammer zone suppression, stealth detection tiles.
- Files: `core/src/main/java/com/militopia/systems/FogSystem.java`
- Risk: Fog-related gameplay features (jammer, radar, cloaking) have no regression safety net.
- Priority: Medium

---

## Performance Bottlenecks

**`logBaseXPStatus()` runs every turn transition and iterates all entities:**
- Issue: `GameScreen.logBaseXPStatus()` scans all entities to build the status log, then calls `calculateGroupedBaseIncome()` per base, which itself scans all entities. This is O(n²) per turn transition.
- Files: `core/src/main/java/com/militopia/screen/GameScreen.java` (lines 655–727)
- Impact: Low at current map sizes (16×16), but will degrade on larger maps or with many structures.
- Fix approach: Cache income values per structure; invalidate on capture/build only.

---

*Concerns audit: 2026-03-26*
