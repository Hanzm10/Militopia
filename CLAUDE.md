# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew run              # Run the game
./gradlew build            # Build project
./gradlew jar              # Create JAR
./gradlew jarWin           # Windows-specific JAR
./gradlew jarMac           # macOS-specific JAR
./gradlew jarLinux         # Linux-specific JAR
```

No test suite exists — manual play-testing is the verification method.

Entry point: `com.militopia.lwjgl3.Lwjgl3Launcher.main()` → `MilitopiaGame`

## Tech Stack

- **Java 8** (source compatibility), **libGDX 1.14.0**, **Ashley ECS 1.7.4**, **Box2D**, **FreeType**
- **Gradle 8.x** multi-project: `core/` (game logic) + `lwjgl3/` (desktop launcher)
- See `STACK.md` for full dependency versions

## Architecture

**ECS (Entity Component System)** via Ashley framework.

### Layers

| Layer | Location | Role |
|---|---|---|
| Screens | `screen/` | libGDX Screen lifecycle — `GameScreen` is the main one |
| Systems | `systems/` | Ashley `EntitySystem` subclasses — all game logic lives here |
| Components | `components/` | Pure data; no logic |
| Managers | `managers/` | Singleton-style services (assets, audio, saves, turn history) |
| Factories | `factory/` | Entity creation — `UnitFactory`, `EntityFactory` |
| Data | `data/` | Static definitions loaded from JSON (`UnitData`, `StructureData`) |
| UI | `ui/` | Custom libGDX Scene2D widgets |
| Map | `map/` | `MapGenerator`, `SimpleNoise` for procedural isometric maps |

### Key Systems

- `CombatSystem` — attack resolution, abilities
- `MovementSystem` — pathfinding
- `FogSystem` — Fog of War
- `MapRenderSystem` / `UnitRenderSystem` — isometric rendering with Z-ordering (`ZComparator`)
- `AnimationSystem` — frame updates
- `StructureEconomySystem` — resources
- `WinConditionSystem` — victory/defeat

### Persistence

`SaveManager` serializes `GameState` (containing `UnitSnapshot`, `StructureSnapshot`, `TurnSnapshot`) to JSON in `assets/saves/`.

### Input Flow

`GameInputController` → `GameScreen` → dispatches to systems/UI

## Conventions

- Components are pure data — no methods beyond getters/setters
- Systems are stateless processors — don't store transient game state in system fields
- Use `AssetManager` for all texture/font/skin loading; never load assets directly
- Isometric rendering requires Z-ordering; use `ZComparator` and `RenderUtils`
- `GameLogger` for all logging (not `System.out.println`)
