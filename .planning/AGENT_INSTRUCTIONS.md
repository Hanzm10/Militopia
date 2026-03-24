# Agent Implementation Guide: Militopia

This document provides explicit instructions for AI agents working on **Militopia**, a Java-based 2-player turn-based strategy game built with **libGDX** and the **Ashley Entity Component System (ECS)**. 

When a user requests a new feature, mechanic, or system, strictly follow this architectural sequence to maintain code health and convention.

---

## 1. Architectural Core Precepts (READ FIRST)

Militopia rigorously separates concerns using ECS. As an agent, you must not mix these layers:
*   **Data Models (`com.militopia.data`)**: Pure Java POJOs used for JSON serialization/deserialization by the `SaveManager` and `TurnHistoryManager`. (e.g., `UnitData`, `GameState`).
*   **Components (`com.militopia.components`)**: Pure data containers for the ECS. They hold **state only**, no logic. (e.g., `StatsComponent`, `TextureComponent`).
*   **Systems (`com.militopia.systems`)**: The logic layer. Systems process entities that contain specific Component families. (e.g., `CombatSystem`, `MovementSystem`).
*   **Factories (`com.militopia.factories`)**: The bridge. Factories (like `UnitFactory`) take raw configurations or Data POJOs and assemble Ashley `Entity` objects with the correct `Component`s.
*   **UI / Input (`com.militopia.ui`, `com.militopia.controller`)**: libGDX Scene2D for UI (`GameHUD`, `SlideMenu`), and `GameInputController` for capturing mouse/touch events and translating screen coordinates to the isometric grid.

---

## 2. Step-by-Step Feature Implementation Workflow

When adding a new feature, execute the following steps in order.

### Step 1: Data Modeling & Persistence
If the feature requires state to persist across turn undos, game saves, or requires tracking new stats:
1.  Locate the relevant data object in `com.militopia.data` (e.g., `UnitData.java`, `StructureData.java`, or `GameState.java`).
2.  Add the new fields. Ensure they have default values or update the necessary constructors.
3.  *Note:* The `SaveManager` uses libGDX `Json` reflection, so public fields in these POJOs are automatically serialized.

### Step 2: ECS Components
If the feature introduces new entity state (e.g., "Shield Points", "Status Effects"):
1.  Check `com.militopia.components` to see if existing components (like `StatsComponent` or `AbilitiesComponent`) can be extended.
2.  If extending logically clutters the component, create a new Component class implementing `com.badlogic.ashley.core.Component`.
3.  Keep it simple: public fields, no game logic methods.

### Step 3: Game Logic via ECS Systems
If the feature introduces a new gameplay rule (e.g., Railway movement, special attack rules):
1.  Create or modify a system in `com.militopia.systems`.
2.  Use `IteratingSystem` if processing entities one-by-one, or `EntitySystem` if it's broad logic.
3.  Define the `Family` of components required: `Family.all(GridPositionComponent.class, ...).get()`.
4.  Use `ComponentMapper` to fetch components rapidly within the `processEntity()` loop:
    ```java
    private final ComponentMapper<StatsComponent> statsMapper = ComponentMapper.getFor(StatsComponent.class);
    ```

### Step 4: Entity Assembly (UnitFactory)
When the game creates units or structures, it must know about your new Components or Data.
1.  Open `com.militopia.factories.UnitFactory`.
2.  Locate `createUnit()`, `createStructure()`, or `createObjectEntity()`.
3.  Attach your new Component to the `Entity` before `engine.addEntity(entity)` is called.
4.  If modifying `StatsComponent` defaults (e.g., cost, vision), update the switch statements in `UnitFactory`.

### Step 5: Input & UI Layer
If the player interacts with the new feature:
1.  **Map Interaction:** Modify `com.militopia.controller.GameInputController`. This class handles raycasting from screen pixels to the isometric grid. Pass the grid coordinates to your new ECS System.
2.  **UI Menus/Overlays:** Modify `com.militopia.ui.GameHUD` (for fixed overlays like TopBar/Popups) or `com.militopia.ui.SlideMenu` (for contextual actions).
3.  *Rule:* UI classes must not process direct game logic. They should trigger a method in a System or modify a Component state that a System will process on the next engine tick.

### Step 6: Headless Testing (CRITICAL)
Militopia uses JUnit 5. Since libGDX requires an OpenGL context, tests run "headless" and native calls will throw `NullPointerException`s if not mocked.
1.  Place tests in `core/src/test/java/com/militopia/systems/`.
2.  **Mocking libGDX:** If a component or factory touches `Gdx.app` or `Gdx.files` (like `UnitFactory` loading textures), you must mock it, or initialize a `HeadlessApplication`.
3.  **Mocking UI:** Use Mockito's `mockConstruction()` for isolating complex UI objects (like `GameOverPopup` or `GameHUD`) when testing core logic.
4.  Always run `./gradlew test` to ensure your new feature hasn't broken the build.

---

## 3. Common Agent Pitfalls (Gotchas)

1.  **Ashley Deferred Operations:** 
    Calling `engine.addEntity()` or `engine.removeEntity()` does **not** take effect immediately. It happens at the end of the `engine.update()` cycle. If your logic relies on checking if an entity is at a location immediately after creating/destroying one, use custom tracking (e.g., query a factory or manager directly) or wait for the engine tick.
2.  **Isometric Coordinates:**
    Grid X/Y (e.g., `(3, 4)`) represent the logical tile map. Screen pixels require conversion. Do not mix grid coordinates with pixel coordinates in the systems. Renderers (`MapRenderSystem`) handle the specific visual offsets.
3.  **Null-Safety in `GameLogger` during Tests:**
    `GameLogger` formats strings using `String.format`. If you pass `null` values into ECS names, it can crash the testing console. Ensure entities have valid `StatsComponent.name` strings.
4.  **Floating Text vs PNG Assets:**
    Phase 8 specifies moving away from `FloatingTextComponent` string overlays to using actual TextureRegions (PNGs) or structured Scene2D UI widgets. Prefer extending `TextureComponent` or utilizing `GameHUD` over drawing raw SpriteBatch fonts.
