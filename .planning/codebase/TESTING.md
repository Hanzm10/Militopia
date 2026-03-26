# Testing Patterns
_Last updated: 2026-03-26_

## Summary
Militopia uses JUnit 5 + Mockito 5 for headless unit testing of game systems. Tests live in `core/src/test/java/com/militopia/` and are organized by package mirroring the main source tree. Coverage is limited to core systems and has no enforcement target. libGDX rendering code is excluded from tests via Mockito mocking of `Gdx.*` statics.

---

## Test Framework

**Runner:** JUnit 5 (junit-bom 5.10.0)
- Config: `core/build.gradle` — `test { useJUnitPlatform() }`

**Mocking:** Mockito 5.11.0

**Dependencies (from `core/build.gradle`):**
```groovy
testImplementation platform('org.junit:junit-bom:5.10.0')
testImplementation 'org.junit.jupiter:junit-jupiter'
testImplementation 'org.mockito:mockito-core:5.11.0'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

**Run Commands:**
```bash
./gradlew test                  # Run all tests
./gradlew test --info           # Verbose output
./gradlew core:test             # Only core module tests
```

Test results are written to `build/reports/tests/test/` (standard Gradle output).

---

## Test File Organization

**Location:** `core/src/test/java/com/militopia/` — mirrors main source package structure

**Packages with tests:**
- `core/src/test/java/com/militopia/systems/` — system logic tests
- `core/src/test/java/com/militopia/ui/` — UI component tests

**Naming:** `{Subject}Test.java` — one test class per feature/system under test

**Current test files:**
- `core/src/test/java/com/militopia/systems/AbilityTest.java`
- `core/src/test/java/com/militopia/systems/ExplorationPersistenceTest.java`
- `core/src/test/java/com/militopia/systems/PortSummonTest.java`
- `core/src/test/java/com/militopia/systems/StructurePlacementTest.java`
- `core/src/test/java/com/militopia/systems/WinConditionTest.java`
- `core/src/test/java/com/militopia/ui/UITest.java`

---

## Test Structure

**Suite setup using `@BeforeEach`:**
```java
@BeforeEach
public void setup() {
    engine = new PooledEngine();
    gameMap = new MapGenerator.GameMap(10, 10);
    gameState = new GameState(12345L, "TestSave", 10, 10);
    combatSystem = new CombatSystem(gameMap, null, gameState);
    engine.addSystem(combatSystem);
}
```

**UI teardown using `@AfterEach` to null out Gdx statics:**
```java
@AfterEach
public void tearDown() {
    Gdx.graphics = null;
    Gdx.gl = null;
    Gdx.gl20 = null;
    Gdx.input = null;
}
```

**Assertion pattern:** JUnit 5 `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull` from `org.junit.jupiter.api.Assertions.*`

---

## Mocking

**Framework:** Mockito (`org.mockito.Mockito`)

**Primary use cases:**
1. Mock `AssetManager` to return dummy `Texture` objects (avoids file I/O in tests):
```java
AssetManager mockAssets = Mockito.mock(AssetManager.class);
Texture mockTex = Mockito.mock(Texture.class);
when(mockAssets.get(anyString())).thenReturn(mockTex);
```

2. Mock `Gdx.app` to prevent `NullPointerException` in `GameLogger`:
```java
Gdx.app = Mockito.mock(Application.class);
```

3. Mock libGDX statics for UI tests (graphics, GL, input):
```java
Gdx.graphics = mock(Graphics.class);
Gdx.gl = mock(GL20.class);
Gdx.gl20 = Gdx.gl;
Gdx.input = mock(Input.class);
when(Gdx.graphics.getWidth()).thenReturn(800);
when(Gdx.graphics.getHeight()).thenReturn(600);
```

4. Mock Scene2D `Table` and `Cell` for UI layout tests using `MockedConstruction`:
```java
try (MockedConstruction<Table> mTable = mockConstruction(Table.class, (mock, context) -> {
    setupTableMock(mock);
})) { ... }
```

5. Mock functional interface callbacks to verify trigger invocations:
```java
WinConditionSystem.GameOverTrigger mockTrigger = Mockito.mock(WinConditionSystem.GameOverTrigger.class);
// ...
verify(mockTrigger).trigger(2);
```

**What to mock:**
- `AssetManager` (requires libGDX file handle context)
- `Gdx.*` statics (require a running application context)
- Scene2D UI widgets when testing pure logic (label updates, layout)
- Callback/trigger interfaces

**What NOT to mock:**
- Ashley ECS `Engine`, `PooledEngine`, `Entity` — use real instances, they are POJO-like
- `GameState`, `MapGenerator.GameMap` — lightweight data containers, use real instances
- Component classes — always use real instances

---

## Helper Factory Pattern

Tests that need multiple entities define a private `createUnit()` helper to avoid duplicating component assembly:

```java
// AbilityTest.java
private Entity createUnit(String type, int x, int y, int owner) {
    Entity e = engine.createEntity();
    e.add(new GridPositionComponent(x, y, 3));
    e.add(new TypeComponent(TypeComponent.Type.UNIT));
    e.add(new AbilitiesComponent());

    // type-specific stat overrides
    if ("TANK".equals(type)) { hp = 30; atk = 12; ... }
    if ("RECRUIT".equals(type)) { hp = 10; atk = 3; ... }

    StatsComponent stats = new StatsComponent(type, hp, atk, def, move, rng, vis, cost, moveType, owner);
    e.add(stats);
    engine.addEntity(e);
    return e;
}
```

Use this pattern for any test class that creates multiple entities of the same logical type.

---

## Reflection for Internal State

`UITest.java` uses a private helper for accessing private fields when the component does not expose state via a public getter:

```java
private Object getInternalField(Object obj, String fieldName) {
    try {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    } catch (Exception e) {
        return null;
    }
}
```

Use this sparingly — only when the UI component intentionally hides its implementation detail.

---

## Test Types

**Unit tests — system logic (majority of tests):**
- Instantiate the system under test with a real `Engine`/`PooledEngine` and `GameMap`
- Directly invoke public system methods (`resolveAttack`, `canBuild`, `performScavenge`, etc.)
- Assert on component state changes, `GameState` field mutations, or callback verifications
- Examples: `AbilityTest`, `WinConditionTest`, `StructurePlacementTest`

**Unit tests — filter/rule logic (no libGDX dependency):**
- Extract the filter logic into a helper method and test it in isolation
- Example: `PortSummonTest.filterForProducer()` mirrors `SlideMenu.populateSummonMenu` filtering
- Useful for testing UI business rules without the Scene2D overhead

**Unit tests — UI components:**
- Mock all libGDX/Scene2D infrastructure
- Use `MockedConstruction` to intercept widget constructors
- Verify label updates and layout interactions via Mockito `verify`
- Example: `UITest`

**Integration tests:** Not used — no multi-system integration test patterns observed.

**E2E tests:** Not used — no framework configured.

---

## Coverage

**Requirements:** None enforced (no Jacoco or coverage plugin in `build.gradle`)

**Covered systems:**
- `CombatSystem` — attack resolution, ability interactions, overwatch, blitz, dig-in, high altitude immunity
- `WinConditionSystem` — base count tracking, win trigger callback
- `ScavengeSystem` — ruin scavenge rewards, entity removal
- `StructurePlacementSystem` — terrain constraint checks, funding checks
- `AbilityStatusSystem` — dig-in expiration on turn start
- `UnitFactory` / `EntityFactory` — base/town creation, capture logic, base count mutation
- `HudTopBar`, `InfoPanel` — label update logic, layout construction

**Not covered (gaps):**
- `MovementSystem` — no tests for pathfinding or move validation
- `FogSystem` — no tests for visibility calculation or jammer mask
- `MapRenderSystem`, `UnitRenderSystem` — rendering systems, skipped (require GL context)
- `AnimationSystem`, `FloatingTextSystem`, `EffectSystem` — no tests
- `AudioManager` — no tests
- `SaveManager` — no tests (persistence layer)
- `TurnHistoryManager` — no tests
- `GameScreen` / `GameInputController` — entry-point orchestration, no tests
- `GameHUD`, `HudBottomBar`, `SlideMenu` — partially covered; summon menu filter tested in `PortSummonTest` but widget construction is not

---

## Async / Engine Update Pattern

When testing systems that use Ashley's entity pending-add/remove queue, call `engine.update(0)` after `addEntity()` and before assertions:

```java
engine.update(0);   // flush pending additions
// ... trigger action ...
engine.update(0.1f); // flush pending removals
engine.update(0.1f); // second pass if needed
```

This is required because Ashley processes entity add/remove lazily on the next update tick.
