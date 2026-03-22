# Debug Session: COMBAT-CTOR-001

## Symptom
`AbilityTest.java` fails to compile because the `CombatSystem` constructor is called with 2 arguments, but 3 are required.

**When:** Running `./gradlew build --stacktrace`
**Expected:** Successful compilation and test execution.
**Actual:** Compilation failed for task ':core:compileTestJava'.

## Evidence
- `AbilityTest.java:28`: `combatSystem = new CombatSystem(gameMap, null);`
- `CombatSystem.java:32`: `public CombatSystem(MapGenerator.GameMap gameMap, EntityFactory entityFactory, com.militopia.data.GameState gameState)`
- Error message: `required: GameMap,EntityFactory,GameState`
- Error message: `found: GameMap,<null>`

## Hypotheses

| # | Hypothesis | Likelihood | Status |
|---|------------|------------|--------|
| 1 | `CombatSystem` constructor was recently changed to require `GameState` but tests weren't updated. | 95% | UNTESTED |
| 2 | `EntityFactory` (the second param) is also required and shouldn't be null in tests. | 40% | UNTESTED |

## Attempts

### Attempt 1
**Testing:** H1 — Update `AbilityTest.java` to provide a `GameState`.
**Action:** Inspect `CombatSystem` constructor to see how `GameState` is used.
**Result:** `CombatSystem` uses `gameState` to track base counts in `flagDeath`.
**Conclusion:** `AbilityTest.java` needs a `GameState` instance.
