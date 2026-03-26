# Architecture
_Last updated: 2026-03-26_

## Summary

Militopia is a 2-player turn-based strategy game built on libGDX + Ashley ECS. The game uses an isometric tile grid, a PooledEngine for entity management, and a clear separation between game logic (ECS systems), rendering (render systems), input (controller), UI (HUD components), and persistence (managers). All game state flows through a central `GameState` data object that is serialized to JSON on save.

## Pattern Overview

**Overall:** Entity-Component-System (ECS) via Ashley, layered over a libGDX screen-based game loop.

**Key Characteristics:**
- Entities are composed at runtime from components (`StatsComponent`, `GridPositionComponent`, `TypeComponent`, etc.)
- Systems hold all logic; components hold only data
- `GameScreen` acts as the scene coordinator — it initializes and wires all systems, but delegates work to them
- UI is built with libGDX Scene2D via a facade (`GameHUD`) that delegates to sub-components
- Save/load serializes `GameState` + ECS entity data to JSON via libGDX `Json`

## Layers

**Entry Point (Platform):**
- Purpose: Bootstraps the libGDX application
- Location: `lwjgl3/src/main/java/com/militopia/lwjgl3/Lwjgl3Launcher.java`
- Contains: `main()` method, window config (640×480, vsync, ANGLE GL emulation)
- Depends on: `MilitopiaGame`

**Application Root:**
- Purpose: One-time initialization of shared resources; routes to first screen
- Location: `core/src/main/java/com/militopia/MilitopiaGame.java`
- Contains: `SpriteBatch`, `AssetManager`, `Skin` setup; starts `MenuScreen`
- Depends on: `AssetManager`, `AudioManager`, `MenuScreen`

**Screens (Scene Management):**
- Purpose: Represent distinct game states; manage per-scene lifecycle
- Location: `core/src/main/java/com/militopia/screen/`
- Contains: `MenuScreen`, `NewGameScreen`, `LoadGameScreen`, `GameScreen`, `FirstScreen`
- Depends on: `MilitopiaGame` (shared batch/assets/skin), downstream ECS & UI

**ECS Core (`GameScreen` setup):**
- Purpose: Owns the `PooledEngine`, registers all systems, creates initial entities
- Location: `core/src/main/java/com/militopia/screen/GameScreen.java`
- Depends on: Ashley `PooledEngine`, all systems, factories, `GameState`, `MapGenerator`

**Components (Data Only):**
- Purpose: Plain data bags attached to entities
- Location: `core/src/main/java/com/militopia/components/`
- Key components:
  - `StatsComponent` — HP, atk, def, move, range, vision, cost, income, XP, level, owner flags
  - `GridPositionComponent` — tile (x, y) and draw layer z
  - `TypeComponent` — entity category: `UNIT`, `OBJECT`, `MARKER`, `ATTACK_MARKER`, `EFFECT`
  - `AbilitiesComponent` — ability state: dig-in, overwatch, cloak, fuel, nuke cooldown
  - `AnimationComponent`, `SpriteAnimationComponent` — animation frame state
  - `MovementComponent`, `FacingComponent` — in-progress move tween state + sprite direction
  - `TextureComponent` — current texture region to render
  - `FloatingTextComponent` — floating damage/feedback text lifecycle
  - `DeathAnimComponent` — marks entity for death animation processing

**Systems (Logic):**
- Purpose: Process entities each frame or on-demand; hold all behavioral logic
- Location: `core/src/main/java/com/militopia/systems/`
- System roster and responsibilities:

| System | Type | Responsibility |
|--------|------|----------------|
| `MovementSystem` | IteratingSystem | Tweens units between grid tiles |
| `AnimationSystem` | IteratingSystem | Advances sprite animation frames |
| `CombatSystem` | EntitySystem (event-driven) | Resolves attacks, counters, AoE nukes, death flagging |
| `EffectSystem` | EntitySystem | Processes death animations and entity removal |
| `FogSystem` | EntitySystem | Recomputes `visibleTiles`/`detectedTiles` per active player, handles Signal Jammer masks |
| `AbilityStatusSystem` | EntitySystem | Ticks ability cooldowns/states at turn start |
| `MapRenderSystem` | EntitySystem | Draws isometric terrain, fog overlay, territory borders |
| `UnitRenderSystem` | EntitySystem | Draws units, structures, markers, HP bars, floating text |
| `FloatingTextSystem` | EntitySystem | Ages and removes floating combat feedback entities |
| `StructureEconomySystem` | EntitySystem (manual call) | Per-turn XP distribution, Hospital healing, base level-up checks |
| `WinConditionSystem` | EntitySystem | Checks `p1BaseCount`/`p2BaseCount` each frame; fires `GameOverTrigger` callback |
| `ScavengeSystem` | EntitySystem | Handles animal scavenging mechanics |
| `StructurePlacementSystem` | EntitySystem | Handles placing new structures adjacent to bases |

**Factories:**
- Purpose: Construct fully-assembled entities and inject them into the engine
- Location: `core/src/main/java/com/militopia/factories/`
- `UnitFactory` — creates units (all types), map object entities, animals; handles base level-up texture swaps; captures `TurnSnapshot` for undo
- `EntityFactory` — creates lightweight entities: movement markers, attack markers, floating text, effect sprites

**Managers:**
- Purpose: Singleton-style services for cross-cutting concerns
- Location: `core/src/main/java/com/militopia/managers/`
- `AssetManager` — wraps libGDX `AssetManager`; defines all asset path constants; synchronously loads all textures and font at startup
- `AudioManager` — singleton; manages music and SFX playback; polled each frame from `MilitopiaGame.render()`
- `SaveManager` — serializes `GameState` + ECS snapshot to JSON via libGDX `Json`; saves to `assets/saves/`
- `TurnHistoryManager` — bounded stack (max 50) of `TurnSnapshot` objects; supports single-step undo

**Controllers:**
- Purpose: Handle all player input and translate it to game actions
- Location: `core/src/main/java/com/militopia/controller/GameInputController.java`
- Extends `InputAdapter`; registered in `InputMultiplexer` behind the `Stage`
- Handles: camera drag/zoom, tile selection, unit movement, attack targeting, ability targeting, summon dispatch

**UI (HUD):**
- Purpose: Scene2D-based overlay rendered on top of the game world
- Location: `core/src/main/java/com/militopia/ui/`
- `GameHUD` — facade coordinating all HUD sub-components via a shared `Stage`
- `HudTopBar` — XP / Funding / Turn number strip
- `HudBottomBar` — Settings / End Turn / Undo buttons + pause overlay
- `InfoPanel` — selected tile info, unit stats, ability buttons
- `SlideMenu` — sliding panel for Summon / Hunt / Capture / Build actions
- `LevelUpPopup` — modal blocking map input during base level-up
- `GameOverPopup` — end-of-game overlay

**Data / State:**
- Purpose: Serializable plain-object representations of game state
- Location: `core/src/main/java/com/militopia/data/`
- `GameState` — authoritative mutable state: seed, map dims, player names, funding, XP, turn, unit/structure/animal lists, `mapObjects` array
- `TurnSnapshot` — immutable point-in-time copy for undo (funds, XP, bases, units, structures, map objects)
- `UnitSnapshot`, `StructureSnapshot` — per-entity records inside `TurnSnapshot`
- `UnitData`, `StructureData`, `AnimalData` — serialization DTOs for save files

**Map:**
- Purpose: Procedural map generation
- Location: `core/src/main/java/com/militopia/map/`
- `MapGenerator` — multi-pass generator: terrain noise → player bases → flora → towns → oil resources → ruins → mountains
- `MapGenerator.GameMap` — holds `terrain[][]`, `objects[][]`, `visibleTiles[][]`, `detectedTiles[][]`
- `SimpleNoise` — deterministic noise function seeded by `GameState.seed`

**Config:**
- Purpose: Compile-time constants
- Location: `core/src/main/java/com/militopia/config/`
- `GameConfig` — tile size (16×10), world size (640×360), zoom limits, drag speed, UI width, testing mode flag
- `BaseLevelConfig` — XP thresholds and income values per base level (1–10)

**Utils:**
- Location: `core/src/main/java/com/militopia/utils/`
- `GameLogger` — tagged logging with turn/player context; tag constants: `ECONOMY`, `COMBAT`, `INPUT`, `ABILITY`
- `RenderUtils` — shared isometric coordinate conversion helpers
- `ZComparator` — sorts entities by draw layer for correct overlap
- `HoverListener` — Scene2D listener for button hover effects

## Data Flow

**Turn Execution:**
1. Player interacts → `GameInputController` catches input event
2. Controller calls `CombatSystem.resolveAttack()` or `UnitFactory.createUnit()` or directly modifies entity components
3. `GameScreen.endTurnAction()` triggers fade-out state machine
4. On fade complete: switch `currentPlayer`, call `StructureEconomySystem.processTurn()`, reset unit `hasActed`/`hasMoved`
5. `FogSystem.update()` recomputes visible tiles for new player
6. `TurnHistoryManager.push(snapshot)` saves undo checkpoint
7. `WinConditionSystem.update()` checks base counts each frame; fires callback if zero

**Save/Load:**
1. `SaveManager.saveGame()` iterates ECS entities, populates `GameState` lists, calls `Json.toJson()`
2. Written to `assets/saves/{saveName}.json`
3. On load: `GameState` deserialized → passed to `GameScreen` constructor → entities recreated from lists

**Undo:**
1. `TurnHistoryManager.push(snapshot)` at turn start
2. `GameScreen.undoTurn()` pops snapshot, removes UNIT entities, restores `GameState` scalars, restores map object array, updates structures in-place, recreates units from `UnitSnapshot` list

## Error Handling

**Strategy:** libGDX `Gdx.app.log()` for diagnostics; no exception propagation pattern. Null checks guard component access throughout.

**Patterns:**
- Components accessed via `entity.getComponent(X.class)` with null checks before use
- `GameLogger.log(tag, ...)` used consistently in systems and GameScreen for game-logic events
- Missing save files fail silently (load screen shows empty list)

## Cross-Cutting Concerns

**Logging:** `GameLogger` with static tag constants (`ECONOMY`, `COMBAT`, `INPUT`, `ABILITY`); context set via `GameLogger.setContext(turn, player)`
**Validation:** None enforced — input gating is done by `inputEnabled` flag on `GameInputController`
**Authentication:** Not applicable (local 2-player only)
**Rendering Coordinate System:** Isometric — grid (x, y) → world: `isoX = (x - y) * (TILE_WIDTH / 2)`, `isoY = (x + y) * (TILE_HEIGHT / 2)`
