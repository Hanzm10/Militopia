# Codebase Structure
_Last updated: 2026-03-26_

## Summary

Militopia is a Gradle multi-module libGDX project. The `core` module contains all game logic and is platform-agnostic. The `lwjgl3` module is the desktop launcher. Assets live at the project root under `assets/`. Planning documents are in `.planning/`.

## Directory Layout

```
Militopia/
├── core/                          # Platform-agnostic game code
│   └── src/
│       ├── main/java/com/militopia/
│       │   ├── MilitopiaGame.java         # Application root (Game subclass)
│       │   ├── components/                # ECS data components
│       │   ├── config/                    # Compile-time constants
│       │   ├── controller/                # Input handling
│       │   ├── data/                      # State & serialization DTOs
│       │   ├── factories/                 # Entity construction
│       │   ├── managers/                  # Asset, audio, save, undo services
│       │   ├── map/                       # Procedural map generation
│       │   ├── screen/                    # libGDX Screen implementations
│       │   ├── systems/                   # ECS systems (all game logic)
│       │   ├── ui/                        # Scene2D HUD components
│       │   └── utils/                     # Logging, rendering, sorting helpers
│       └── test/java/com/militopia/
│           ├── systems/                   # Unit tests for ECS systems
│           └── ui/                        # Unit tests for UI components
├── lwjgl3/                        # Desktop launcher module
│   └── src/main/java/com/militopia/lwjgl3/
│       ├── Lwjgl3Launcher.java            # main() entry point
│       └── StartupHelper.java             # macOS/Windows JVM helper
├── assets/                        # All game assets (shared across platforms)
│   ├── bases/                     # Base sprites (lvl1–10, blue/red)
│   ├── displays/                  # Unit/animal info panel images
│   ├── game-system/               # Background, font (.ttf)
│   ├── objects/                   # Map object sprites (trees, animals, etc.)
│   ├── saves/                     # JSON save files (written at runtime)
│   ├── structures/                # Specialized structure sprites
│   ├── tiles/                     # Terrain tile sprites + fog of war
│   ├── ui/                        # UI icons and markers
│   └── units/                     # Unit sprites (left/right facing)
├── .planning/                     # GSD planning docs
│   ├── codebase/                  # Codebase analysis (this dir)
│   └── phases/                    # Phase plans
├── build.gradle / settings.gradle # Gradle build config
└── gradle/                        # Gradle wrapper
```

## Directory Purposes

**`core/src/main/java/com/militopia/components/`:**
- Purpose: ECS data-only components (implement Ashley `Component`)
- Key files:
  - `StatsComponent.java` — all unit and structure numeric stats; also carries `owner`, `hasActed`, `hasMoved`, income, XP, level fields
  - `GridPositionComponent.java` — tile coordinates (x, y) and draw priority (z)
  - `TypeComponent.java` — entity classification enum: `UNIT`, `OBJECT`, `MARKER`, `ATTACK_MARKER`, `EFFECT`
  - `AbilitiesComponent.java` — ability state: dig-in, overwatch, cloak, fuel, nuke cooldown
  - `MovementComponent.java` — tween state for smooth unit sliding
  - `FacingComponent.java` — left/right texture regions for directional sprites
  - `AnimationComponent.java`, `SpriteAnimationComponent.java` — animation playback state
  - `TextureComponent.java` — current display `TextureRegion`
  - `FloatingTextComponent.java` — floating damage text position and lifetime
  - `DeathAnimComponent.java` — death animation marker

**`core/src/main/java/com/militopia/config/`:**
- Purpose: Compile-time constants; no instances needed
- Key files:
  - `GameConfig.java` — tile dimensions (16×10), world size (640×360), zoom/drag params, UI width, `TESTING_MODE` flag
  - `BaseLevelConfig.java` — per-level XP thresholds and income values for the 10-level base upgrade system

**`core/src/main/java/com/militopia/controller/`:**
- Purpose: Translate raw input events into game actions
- Key files:
  - `GameInputController.java` — extends `InputAdapter`; handles tile clicks, drag-to-pan, scroll-to-zoom, unit selection, move/attack dispatch, ability targeting mode

**`core/src/main/java/com/militopia/data/`:**
- Purpose: Plain Java objects for game state and serialization
- Key files:
  - `GameState.java` — mutable root state object (seed, map size, player names, funds, XP, turn, lists of units/structures/animals, `mapObjects` array)
  - `TurnSnapshot.java` — immutable undo checkpoint
  - `UnitSnapshot.java`, `StructureSnapshot.java` — per-entity records within a `TurnSnapshot`
  - `UnitData.java`, `StructureData.java`, `AnimalData.java` — save file serialization DTOs

**`core/src/main/java/com/militopia/factories/`:**
- Purpose: Construct and register complete entities into the Ashley `PooledEngine`
- Key files:
  - `UnitFactory.java` — creates all unit types (14 unit types across land/air/sea), map objects, base entities; handles base texture swaps on level-up; takes `TurnSnapshot` captures
  - `EntityFactory.java` — creates lightweight transient entities: movement markers, attack markers, floating text, explosion effects

**`core/src/main/java/com/militopia/managers/`:**
- Purpose: Long-lived services for resources and cross-cutting operations
- Key files:
  - `AssetManager.java` — wraps libGDX `AssetManager`; all asset path string constants defined here; synchronous `finishLoading()` on startup
  - `AudioManager.java` — singleton; manages BGM and SFX; polled each frame from `MilitopiaGame.render()`
  - `SaveManager.java` — serializes `GameState` to JSON (`assets/saves/`); deserializes on load
  - `TurnHistoryManager.java` — bounded `ArrayDeque` (max 50) of `TurnSnapshot`; push/pop for undo

**`core/src/main/java/com/militopia/map/`:**
- Purpose: Procedural map generation
- Key files:
  - `MapGenerator.java` — 5-pass generator; defines `TerrainType` and `ObjectType` enums; `GameMap` inner class holds all grid arrays
  - `SimpleNoise.java` — seeded noise for deterministic terrain generation

**`core/src/main/java/com/militopia/screen/`:**
- Purpose: libGDX `Screen` implementations — one per game state
- Key files:
  - `GameScreen.java` — main game loop coordinator; owns `PooledEngine`, all systems, `GameInputController`, `GameHUD`; manages turn fade state machine
  - `MenuScreen.java` — main menu with New Game / Resume / Exit
  - `NewGameScreen.java` — map size and player name configuration before starting
  - `LoadGameScreen.java` — lists save files for loading
  - `FirstScreen.java` — unused / placeholder

**`core/src/main/java/com/militopia/systems/`:**
- Purpose: All ECS system logic
- Key files (13 systems total):
  - `CombatSystem.java` — event-driven; resolves attacks via `resolveAttack()` / `launchNuke()`; no per-frame iteration
  - `MapRenderSystem.java` — isometric terrain rendering with fog overlay and territory border ShapeRenderer
  - `UnitRenderSystem.java` — renders units, objects, markers, HP bars; respects fog visibility
  - `FogSystem.java` — resets and recomputes `visibleTiles[][]` / `detectedTiles[][]` per player per turn
  - `StructureEconomySystem.java` — called manually at turn start; distributes XP, heals via hospitals, triggers level-ups
  - `WinConditionSystem.java` — checks `p1BaseCount == 0` or `p2BaseCount == 0` each frame; uses `GameOverTrigger` callback interface
  - `MovementSystem.java` — IteratingSystem; interpolates units during slide animation
  - `AnimationSystem.java` — IteratingSystem; advances sprite animation frames
  - `AbilityStatusSystem.java` — resets overwatch, ticks cooldowns at `onTurnStart(playerID)`
  - `EffectSystem.java` — processes death animations and removes dead entities from engine
  - `FloatingTextSystem.java` — ages floating text entities; removes them when expired
  - `ScavengeSystem.java` — handles animal scavenging by units
  - `StructurePlacementSystem.java` — handles placing new structures near friendly bases

**`core/src/main/java/com/militopia/ui/`:**
- Purpose: Scene2D-based HUD rendered over the game world
- Key files:
  - `GameHUD.java` — facade; owns the shared `Stage`; delegates to sub-components
  - `HudTopBar.java` — XP / Funding / Turn number display
  - `HudBottomBar.java` — Settings / End Turn / Undo buttons
  - `InfoPanel.java` — selected tile and unit details with ability action buttons
  - `SlideMenu.java` — slide-in panel for summon/hunt/capture/build actions
  - `SummonButton.java` — individual summon action button widget
  - `LevelUpPopup.java` — modal dialog; blocks map input while displayed
  - `GameOverPopup.java` — game over overlay with winner display

**`core/src/main/java/com/militopia/utils/`:**
- Purpose: Shared utilities with no game-state dependencies
- Key files:
  - `GameLogger.java` — tagged logger; `setContext(turn, player)` prepends turn/player info; tag constants: `ECONOMY`, `COMBAT`, `INPUT`, `ABILITY`
  - `RenderUtils.java` — isometric coordinate math helpers
  - `ZComparator.java` — `Comparator<Entity>` sorting by `GridPositionComponent.z` for proper draw order
  - `HoverListener.java` — Scene2D `InputListener` for button hover cursor/color changes

**`assets/saves/`:**
- Purpose: Runtime-written JSON save files
- Generated: Yes (by `SaveManager`)
- Committed: No (runtime data)

## Key File Locations

**Entry Points:**
- `lwjgl3/src/main/java/com/militopia/lwjgl3/Lwjgl3Launcher.java` — desktop `main()`
- `core/src/main/java/com/militopia/MilitopiaGame.java` — libGDX `Game.create()`

**Configuration:**
- `core/src/main/java/com/militopia/config/GameConfig.java` — all magic numbers
- `core/src/main/java/com/militopia/config/BaseLevelConfig.java` — base upgrade table

**Core Loop:**
- `core/src/main/java/com/militopia/screen/GameScreen.java` — render loop + turn state machine

**Asset Registry:**
- `core/src/main/java/com/militopia/managers/AssetManager.java` — all asset path constants

**Game State Root:**
- `core/src/main/java/com/militopia/data/GameState.java`

**Testing:**
- `core/src/test/java/com/militopia/systems/` — system unit tests
- `core/src/test/java/com/militopia/ui/UITest.java` — HUD tests

## Naming Conventions

**Files:**
- Systems: `{Name}System.java` (e.g., `CombatSystem.java`, `FogSystem.java`)
- Components: `{Name}Component.java` (e.g., `StatsComponent.java`)
- Screens: `{Name}Screen.java` (e.g., `GameScreen.java`)
- Managers: `{Name}Manager.java`
- Asset paths: `UPPER_SNAKE_CASE` constants in `AssetManager.java`

**Packages:**
- All under `com.militopia.*`
- Sub-packages match directory names: `components`, `systems`, `ui`, `data`, `factories`, `managers`, `screen`, `controller`, `map`, `config`, `utils`

## Where to Add New Code

**New Unit Type:**
- Add asset path constants: `core/src/main/java/com/militopia/managers/AssetManager.java`
- Load assets in `loadAssets()`: same file
- Register unit data + factory method: `core/src/main/java/com/militopia/factories/UnitFactory.java`
- Add display sprite under: `assets/units/` (left and right facing) + `assets/displays/`

**New ECS System:**
- Implement in: `core/src/main/java/com/militopia/systems/`
- Register with engine: `core/src/main/java/com/militopia/screen/GameScreen.java` constructor

**New Component:**
- Implement in: `core/src/main/java/com/militopia/components/`
- Add to relevant factory methods in `UnitFactory.java` or `EntityFactory.java`

**New Screen:**
- Implement in: `core/src/main/java/com/militopia/screen/`
- Navigate via `game.setScreen(new MyScreen(game))`

**New HUD Element:**
- Add sub-component in: `core/src/main/java/com/militopia/ui/`
- Register in `GameHUD.java` build/render/resize/dispose lifecycle

**New Config Constant:**
- Add to: `core/src/main/java/com/militopia/config/GameConfig.java`

**New Asset:**
- Add path constant to `AssetManager.java`
- Add `manager.load(...)` call in `loadAssets()`
- Place file under appropriate `assets/` subdirectory

**Tests:**
- System tests: `core/src/test/java/com/militopia/systems/`
- UI tests: `core/src/test/java/com/militopia/ui/`
