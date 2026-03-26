# Coding Conventions
_Last updated: 2026-03-26_

## Summary
Militopia is a Java 8 libGDX + Ashley ECS game. Conventions follow standard Java community style with no automated linting enforced by the build. Code is organized around ECS principles: thin components (data only), systems (logic), and factories (entity construction). Comments are used liberally on public APIs and non-obvious logic.

---

## Naming Patterns

**Files:**
- Classes use `PascalCase` matching the class name: `CombatSystem.java`, `StatsComponent.java`
- Test files suffix with `Test`: `WinConditionTest.java`, `AbilityTest.java`

**Classes:**
- Components: `{Concept}Component` — `StatsComponent`, `GridPositionComponent`, `AbilitiesComponent`
- Systems: `{Concept}System` — `CombatSystem`, `FogSystem`, `WinConditionSystem`
- Managers: `{Concept}Manager` — `AssetManager`, `AudioManager`, `SaveManager`
- Factories: `{Concept}Factory` — `UnitFactory`, `EntityFactory`
- Screens: `{Concept}Screen` — `GameScreen`, `MenuScreen`, `LoadGameScreen`
- UI components: Descriptive PascalCase — `HudTopBar`, `InfoPanel`, `SlideMenu`, `GameHUD`

**Methods and variables:**
- `camelCase` for all method names and local variables
- Boolean flags use `is` or `has` prefix: `isOverwatchActive`, `hasMoved`, `hasActed`, `isDiggingIn`, `isPlaying`
- Constants (static final): `SCREAMING_SNAKE_CASE` — `TILE_GRASS`, `TAG`, `BASE_LVL1_BLUE`

**Fields:**
- Public fields on ECS components are acceptable and common (components are plain data bags):
  ```java
  public int currentHP;
  public boolean hasMoved = false;
  public String unitTypeKey = "";
  ```
- Systems and managers use `private final` for injected dependencies:
  ```java
  private final MapGenerator.GameMap gameMap;
  private final EntityFactory entityFactory;
  private final com.militopia.data.GameState gameState;
  ```

---

## Code Style

**Formatting:**
- No automated formatter configured (no `.editorconfig`, Checkstyle, or Spotless in `build.gradle`)
- Indentation: 4 spaces (standard Java)
- Braces: Allman-adjacent — opening brace on same line as declaration
- Java source/target compatibility: Java 8 (`java.sourceCompatibility = 8`)

**Encoding:**
- UTF-8 enforced via `[compileJava, compileTestJava]*.options*.encoding = 'UTF-8'` in `core/build.gradle`

**Linting:**
- No static analysis tools (Checkstyle, PMD, SpotBugs) detected

---

## ECS Architecture Conventions

**Components are pure data** — no logic, no methods beyond constructors:
```java
// core/src/main/java/com/militopia/components/StatsComponent.java
public class StatsComponent implements Component {
    public int maxHP;
    public int currentHP;
    public boolean hasMoved = false;
    // constructors only — no behavior
}
```

**Systems hold all logic** — extend `EntitySystem` or `IteratingSystem`:
- Non-iterating systems (event-driven): extend `EntitySystem`, expose explicit public methods:
  ```java
  // CombatSystem — triggered by player input, not the update loop
  public void resolveAttack(Entity attacker, Entity defender) { ... }
  public void checkOverwatch(Entity movingUnit, int tx, int ty) { ... }
  ```
- Iterating systems: extend `IteratingSystem`, override `processEntity(Entity entity, float deltaTime)`

**System priority** is set in constructor when order matters:
```java
super(10); // Priority 10 — runs after most other systems (WinConditionSystem)
this.priority = 0;  // FogSystem runs first
```

**Factories create entities** — `UnitFactory` and `EntityFactory` centralize entity construction and prevent component assembly from leaking into systems.

---

## Import Organization

No enforced import ordering. Mixed ordering observed in `CombatSystem.java` (Ashley imports and project imports interleaved). Wildcard imports appear alongside specific imports:
```java
import com.militopia.components.*;          // wildcard
import com.militopia.components.AbilitiesComponent;  // then specific — redundant
```
This is a known inconsistency in the codebase (see CONCERNS.md).

---

## Comments and Documentation

**Javadoc on public APIs** — all public-facing classes and non-trivial methods have block comments:
```java
/**
 * Handles all combat resolution: damage, counterattack, death flagging, and
 * spawning floating feedback text.
 *
 * This system is NOT an IteratingSystem — combat is event-driven (triggered by
 * player input), so the only public entry point is {@link #resolveAttack}.
 */
public class CombatSystem extends EntitySystem {
```

**Inline comments** explain non-obvious game logic (Chebyshev distance, ability interactions, layer indices).

**Section separators** used in long files:
```java
// -------------------------------------------------------------------------
// Public API
// -------------------------------------------------------------------------
```

---

## Logging

**Framework:** `GameLogger` utility class — `core/src/main/java/com/militopia/utils/GameLogger.java`

**Pattern — structured event log with turn/player context:**
```java
GameLogger.setContext(turn, player);                      // set at turn start
GameLogger.log(GameLogger.MOVE, "message");               // active player context
GameLogger.log(GameLogger.ABILITY, player, "message");    // explicit player override
GameLogger.logScreen("message");                          // pre-game menu navigation
```

**Categories:** `SCREEN`, `INPUT`, `MOVE`, `ATTACK`, `ABILITY`, `SUMMON`, `BUILD`, `CAPTURE`, `SCAVENGE`, `ECONOMY`, `UI`, `GAME_OVER`

**Log format:** `[T{turn}|P{player}] [{category}] {message}` — tag is `"ActionLog"`

**Null-safe:** Falls back to `System.out.println` when `Gdx.app` is null (headless tests).

---

## Error Handling

**Null guard pattern** — explicit null checks before component access:
```java
if (aStats == null || aAbilities == null) return;
```

**No exceptions thrown** for game logic failures — silent returns are preferred.

**No custom exception hierarchy** detected.

---

## Module Design

**Package structure maps to layer:**
- `com.militopia.components` — ECS data
- `com.militopia.systems` — ECS logic
- `com.militopia.managers` — cross-cutting services (assets, audio, save)
- `com.militopia.factories` — entity construction
- `com.militopia.screen` — libGDX screens (entry points)
- `com.militopia.ui` — Scene2D UI components
- `com.militopia.data` — plain data/snapshot objects (`GameState`, `UnitData`, `StructureData`)
- `com.militopia.map` — map generation and tile definitions
- `com.militopia.config` — configuration data (`BaseLevelConfig`)
- `com.militopia.utils` — shared utilities (`GameLogger`, `RenderUtils`, `ZComparator`)

**Testability pattern** — functional interfaces used to inject callbacks and avoid libGDX headless failures:
```java
// WinConditionSystem.java
public interface GameOverTrigger {
    void trigger(int winnerID);
}
```

**Asset paths** are centralized as `public static final String` constants on `AssetManager`:
```java
public static final String TILE_GRASS = "tiles/tile_grass.png";
```

---

## Commit Convention (from PROJECT_RULES.md)

Format: `type(scope): description`

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

Scope for phase work: phase number — e.g., `feat(phase-1): ...`
