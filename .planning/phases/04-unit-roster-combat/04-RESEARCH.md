# Phase 4: Unit Roster & Combat — Research

**Researched:** 2026-03-26
**Domain:** libGDX / Ashley ECS — combat resolution, unit stats, death animation, targeting UI
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Damage Formula**
- Formula: `damage = max(0, ATK - DEF + terrainBonus)`
- DEF can fully negate — no minimum damage floor (0 is valid)
- Terrain defense bonus applied to the defender's tile:
  - Forest (TREE object on GRASS) = +1 DEF
  - Mountain = +3 DEF
  - All other terrain = 0
- Range penalty: -1 damage when attacker fires at max range (max only, not gradient)

**Attack Targeting UI**
- Context-sensitive targeting: clicking an enemy tile within range triggers the attack — no explicit "Attack mode" button needed
- Move range = blue overlay, Attack range = red overlay — both shown simultaneously on unit selection
- Attacking without moving first forfeits movement for that turn (move is locked)
- No confirmation dialog — click enemy → attack resolves immediately

**Counterattack**
- Defender retaliates only if attacker is within the defender's attack range (range-gated)
- Counterattack uses the same full damage formula: `max(0, ATK - DEF + terrainBonus)`
- Resolution order: attacker strikes first — if defender dies, no counter fires
- Terrain applies symmetrically: attacker's tile gives the attacker a DEF bonus during the counter

**Unit Death & Removal**
- Death animation: flash red 2-3 times → fade out over ~0.5s
- Animation is non-blocking — player may continue acting while it plays
- Tile after removal: clean — no debris or wreck marker
- One-shot kills prevent counterattack entirely — mutual kills are impossible given this order

**Combat Feedback (HUD)**
- Floating damage number pops above the target, floats up and fades over ~1s (red text)
- When DEF fully negates the hit: "BLOCKED" text floats instead of `0`
- Counterattack feedback: same floating number style, but positioned over the attacker
- HUD unit info panel HP: instant snap to new value when damage lands — no drain animation

### Claude's Discretion
- Input blocking during death animation: non-blocking (0.5s is too short to warrant locking input)

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within Phase 4 combat scope.
</user_constraints>

---

## Summary

Phase 4 is the combat loop for an already-populated unit roster. The codebase audit reveals that **all core Phase 4 features are fully implemented** as of the UAT sign-off (2026-03-23). `CombatSystem.resolveAttack` implements the exact ATK-DEF-terrain formula, counterattack resolution, and range-penalty from the decisions. `DeathAnimComponent` + `UnitRenderSystem` drive the flash-red/fade death animation. `FloatingTextSystem` + `FloatingTextComponent` handle floating damage numbers and "BLOCKED" text. `GameInputController` wires context-sensitive attack triggers without confirmation dialogs.

The four codebase concerns flagged in the phase brief are real but nuanced: WRAITH is referenced in render logic but never defined as a summonable unit (stealth fallback); string-switch stat blocks in `UnitFactory` are the accepted pattern for this codebase, not a defect to fix in this phase; name-string matching in `CombatSystem` already uses `unitTypeKey` (not `stats.name`) for all ability branches; and `MovementSystem`'s position interpolation works correctly for movement but `AnimationSystem.updateLunge` does drive the attack lunge via `visualOffsetX/Y` on `GridPositionComponent`.

**Primary recommendation:** Phase 4 implementation is already complete. Plan tasks should focus on verifying the known codebase debt items, writing the missing unit-combat JUnit tests, and confirming the 10-unit summon roster is fully wired to the SlideMenu.

---

## Standard Stack

### Core (already in project — no installation needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| libGDX | 1.13.0 | Rendering, input, SpriteBatch, BitmapFont | Project foundation |
| Ashley ECS | 1.7.4 | Entity/Component/System architecture | All game logic uses it |
| JUnit Jupiter | 5.10.0 | Unit tests for headless logic | Existing test infra |
| Java 8 | 8 | Language target | Build constraint |

### No new dependencies for this phase.

---

## Architecture Patterns

### ECS Combat Event Flow (as implemented)

```
Player click (GameInputController.touchDown)
  → performAttack(attacker, defender)
    → CombatSystem.resolveAttack(attacker, defender)
        1. terrainDefBonus(defender tile)
        2. dmg = max(0, ATK - DEF - terrainBonus - rangePenalty)
        3. dStats.currentHP -= dmg
        4. spawnFloatingText(dmg, dPos)   [entityFactory.createFloatingText]
        5. if defender.HP <= 0 → flagDeath(defender) → entity.add(DeathAnimComponent)
        6. else counter = max(0, dATK - aStats.defense - atkTerrainBonus)
        7. spawnFloatingText(counter, aPos, isCounter=true)
        8. exhaustAttacker(hasMoved=true, hasActed=true)
  → gameHUD.snapHP(newHP, maxHP)   [instant HUD update, no animation]
```

### Death Animation (as implemented)

```
UnitRenderSystem.drawEntity per frame:
  if DeathAnimComponent present:
    timer += deltaTime
    progress = timer / 0.5f
    alpha = 1 - progress
    flashRed = ((int)(timer / 0.1f) % 2 == 0)   → alternates white/red every 0.1s
    if timer >= 0.5f → toRemove.add(entity)       → engine.removeEntity on next pass
```

Entity removal is deferred to end of the render pass via `toRemove` list — safe for concurrent iteration.

### Floating Text Lifecycle (as implemented)

```
FloatingTextSystem.processEntity per frame:
  ft.timer += deltaTime
  ft.alpha = 1 - (timer / 1.0f)
  ft.worldY += deltaTime * 18f    → drifts upward 18 world units over 1s
  if timer >= 1.0f → removeEntity

UnitRenderSystem.drawFloatingTexts:
  DAMAGE type → red
  BLOCKED type → gold
  isCounter=true → positioned above attacker tile (different grid coords passed)
```

### Unit Stat Definition Pattern (existing — switch-case in UnitFactory)

All 13 unit types (RECRUIT, RANGER, SNIPER, TANK, JUGGERNAUT, RECON_DRONE, SUICIDE_DRONE, APACHE, B2, GUNBOAT, DESTROYER, CARRIER, SUBMARINE) are defined as `case` branches inside `UnitFactory.createUnit`. Stats constructor takes `(name, hp, atk, def, move, rng, vis, cost, moveType, owner)`.

`unitTypeKey` is set on `StatsComponent` immediately after construction — this is the canonical identifier used by all ability branches in `CombatSystem` and `AbilityStatusSystem`.

### Anti-Patterns to Avoid

- **stats.name string matching for logic:** `CombatSystem` already uses `stats.unitTypeKey` for all ability checks. The only `stats.name` checks are for structure names ("Oil Derrick", "Nuclear Plant", "Base") which are intentionally string-matched because structures don't have a typed enum.
- **Adding new stat fields to StatsComponent directly:** The component is shared between units and structures; income/XP/level fields are already there. Don't conflate unit and structure concerns further.
- **Calling engine.removeEntity mid-iteration:** Always defer to `toRemove` list as `UnitRenderSystem` already does.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Floating text fade + float | Custom timer | `FloatingTextSystem` + `FloatingTextComponent` | Already exists, handles timer, alpha, worldY drift |
| Death animation | New system | `DeathAnimComponent` + `UnitRenderSystem.drawEntity` | Already exists with 0.5s flash-fade |
| Attack lunge animation | Manual offset code | `AnimationComponent` (Type.LUNGE) + `AnimationSystem.updateLunge` | Already sets `visualOffsetX/Y` via sine wave |
| Terrain bonus lookup | Inline terrain checks | `CombatSystem.terrainDefBonus(x, y)` | Already handles MOUNTAIN and TREE object cases |
| Range calculation | Manual abs() math | `CombatSystem.chebyshev(ax, ay, bx, by)` | Chebyshev distance matches game's diagonal movement |

---

## Codebase Debt Analysis (the four flagged concerns)

### 1. WRAITH unit type — undefined but referenced

**Location:** `UnitRenderSystem.java:138`, `UnitFactory.java:398`

**What it does:** `UnitRenderSystem` checks `stats.unitTypeKey.equals("WRAITH")` and applies stealth (isStealth = true). `UnitFactory.createUnit` sets `abilities.isCloaked = true` for WRAITH. But there is no `case "WRAITH":` in the stat switch, no texture region loaded, and no summon button. If `createUnit("WRAITH", ...)` is called, `unitRegions.get("WRAITH")` returns null and falls back to RECRUIT texture regions.

**Impact:** WRAITH is currently unreachable by the player — it cannot be summoned. The stealth logic in `UnitRenderSystem` is dead code for this phase. No silent stat fallback occurs in normal gameplay.

**Resolution:** Either add full WRAITH definition (stats, textures, summon slot) or delete the dead references. If it belongs to a future phase, leave with a TODO comment.

### 2. MovementSystem position interpolation

**What it does:** `MovementSystem` updates `move.time` and removes `MovementComponent` at completion. The visual lerp from `startX/Y` to `targetX/Y` is handled in `UnitRenderSystem.drawEntity` using `MathUtils.lerp`. This is correct — the render system drives the visual, the `MovementSystem` only manages the timer and facing direction.

**The no-op concern:** The lunge animation (`AnimationComponent` Type.LUNGE) uses `pos.visualOffsetX/Y` via `AnimationSystem.updateLunge` — this IS active. `CombatSystem.triggerAttackAnimation` sets `AnimationComponent` on the attacker when attacking. The sine wave lunge fires correctly.

**Conclusion:** Movement interpolation works. Lunge animation works. Not a blocker.

### 3. Name-string matching for game logic

**Actual state:** `CombatSystem` uses `unitTypeKey.equals(...)` for all unit ability checks (JUGGERNAUT, RECON_DRONE, DESTROYER, TANK, GUNBOAT, SUICIDE_DRONE, RANGER). The `stats.name.contains(...)` pattern is used only for indestructible structure checks ("Oil Derrick", "Nuclear Plant") and base tracking ("Base") — these are structures, not units, and don't have a `unitTypeKey` enum equivalent. This is acceptable for the current architecture.

**Not a Phase 4 defect.** If a unit type enum is wanted for structures, that is a refactor task, not a combat task.

### 4. Hardcoded stat switch blocks in UnitFactory

**Actual state:** The switch-case pattern in `UnitFactory.createUnit` is the deliberate pattern for this project. There is no external JSON or `UnitData` driving unit stats (that class exists but holds save snapshot data, not stat definitions). Cost duplication between `createUnit` and `getUnitCost` is real debt — a cost change requires two edits.

**Phase 4 concern:** If the task is to verify "10 units are summonable with correct costs," then the test must compare costs between `createUnit` switch and `getUnitCost` switch. A test can verify both return identical values for each key.

**Counted units in summon roster (cost > 0):** RECRUIT (2), RANGER (5), SNIPER (8), TANK (15), RECON_DRONE (4), SUICIDE_DRONE (7), APACHE (18), GUNBOAT (6), DESTROYER (13), CARRIER (25) = **10 units**. B2, JUGGERNAUT, SUBMARINE have cost 0 (special unlock only, not standard summon).

---

## Common Pitfalls

### Pitfall 1: Removing entities inside an ECS iteration
**What goes wrong:** Calling `engine.removeEntity(e)` while iterating an `ImmutableArray<Entity>` causes ConcurrentModificationException or skipped entities.
**Why it happens:** Ashley's `ImmutableArray` is a live view; modification invalidates the iterator.
**How to avoid:** Queue to `toRemove` list, flush after iteration ends. `UnitRenderSystem` already does this correctly.

### Pitfall 2: Floating text world coordinates vs grid coordinates
**What goes wrong:** Passing raw grid integers to `createFloatingText` produces text at (0,0) in world space.
**Why it happens:** Grid coords and isometric world coords are different coordinate systems.
**How to avoid:** Always convert via `EntityFactory.gridToIsoX(gx, gy)` and `gridToIsoY(gx, gy)` before passing world position. `CombatSystem.spawnFloatingText` already does this.

### Pitfall 3: Death animation double-removal
**What goes wrong:** An entity with `DeathAnimComponent` gets added to `toRemove` twice if it passes through multiple render/removal passes, causing an Ashley warning or no-op double remove.
**Why it happens:** `flagDeath` adds the component; if `resolveAttack` is called again on the same entity (e.g. AoE) the component is added again and the entity may be queued twice.
**How to avoid:** Guard in `triggerExplosion`: `if (v.getComponent(DeathAnimComponent.class) == null)` — already present in `CombatSystem.triggerExplosion`. Apply the same guard in any new AoE path.

### Pitfall 4: Counterattack triggering after attacker death
**What goes wrong:** Attacker dies from counterattack; caller code still references attacker's `StatsComponent` expecting it to be alive.
**Why it happens:** `resolveAttack` calls `flagDeath(attacker)` on HP <= 0 but does not return early — `exhaustAttacker` still runs after.
**How to avoid:** This is by design (exhaustAttacker marks `hasActed`/`hasMoved` which is irrelevant after death). The caller (`performAttack` in `GameInputController`) checks `aStats.currentHP > 0` before calling `gameHUD.snapHP`. Maintain this guard.

### Pitfall 5: Range penalty applied to melee units
**What goes wrong:** Formula applies `-1` for max range, but melee units (attackRange = 1) always attack at max range, creating an unintended -1 penalty.
**Why it happens:** `maxRange = (dist == aStats.attackRange)` is always true at dist=1/rng=1.
**How to avoid:** The guard `(maxRange && aStats.attackRange > 1 ? 1 : 0)` is already in `CombatSystem.resolveAttack`. Range penalty only fires when `attackRange > 1`. Do not remove this guard.

---

## Code Examples

### Damage formula (from CombatSystem.java)
```java
// Source: CombatSystem.java lines 124-136
int dist = chebyshev(aPos.x, aPos.y, dPos.x, dPos.y);
boolean maxRange = (dist == aStats.attackRange);
int defTerrainBonus = terrainDefBonus(dPos.x, dPos.y);

int dmg = Math.max(0, (aStats.attack + shoreBonus)
    - (dStats.defense + digInBonus)
    - defTerrainBonus
    - (maxRange && aStats.attackRange > 1 ? 1 : 0));
dStats.currentHP -= dmg;
```

### Spawning floating text for counter (from CombatSystem.java)
```java
// isCounter=true passes attacker's grid position so text appears above attacker
spawnFloatingText(ctrDmg, aPos.x, aPos.y, true);
```

### Flagging death (from CombatSystem.java)
```java
// Adds DeathAnimComponent; UnitRenderSystem drives animation and removal
public void flagDeath(Entity entity) {
    entity.add(new DeathAnimComponent());
}
```

### JUnit test harness for CombatSystem (from AbilityTest.java)
```java
// No libGDX init needed — CombatSystem is headless-safe
engine = new PooledEngine();
gameMap = new MapGenerator.GameMap(10, 10);
// Initialize terrain...
combatSystem = new CombatSystem(gameMap, null, gameState);
engine.addSystem(combatSystem);
```
Pass `null` for `EntityFactory` to skip floating-text entity creation in tests.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | `core/build.gradle` (useJUnitPlatform()) |
| Quick run command | `./gradlew :core:test --tests "com.militopia.systems.AbilityTest"` |
| Full suite command | `./gradlew :core:test` |

### Phase Requirements → Test Map

| Req | Behavior | Test Type | Automated Command | File Exists? |
|-----|----------|-----------|-------------------|--------------|
| SC-1 | 10 units summonable with correct costs | unit | `./gradlew :core:test --tests "com.militopia.systems.CombatRosterTest"` | ❌ Wave 0 |
| SC-2 | ATK-DEF formula with terrain/range modifiers | unit | `./gradlew :core:test --tests "com.militopia.systems.AbilityTest"` | ✅ partial |
| SC-3 | Death animation plays before entity removal | manual | n/a — visual | manual-only |

### Sampling Rate
- Per task commit: `./gradlew :core:test --tests "com.militopia.systems.AbilityTest"`
- Per wave merge: `./gradlew :core:test`
- Phase gate: Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `core/src/test/java/com/militopia/systems/CombatRosterTest.java` — covers SC-1 (verify 10 units summonable, costs match between createUnit and getUnitCost)
- [ ] Extend `AbilityTest.java` to cover terrain bonus (+1 TREE, +3 MOUNTAIN) and range penalty (maxRange && rng>1 → -1)

---

## Project Constraints (from CLAUDE.md)

| Directive | Constraint |
|-----------|------------|
| No test suite in base instructions | Manual play-testing is listed as primary verification. JUnit tests exist in practice — use them. |
| Java 8 source compatibility | No lambdas with streams that aren't Java 8 compatible. No `var` keyword. |
| Ashley ECS conventions | Components are pure data. Systems are stateless processors. No transient game state in system fields. |
| Use AssetManager for all textures | Never `new Texture(...)` directly — use `assets.get(AssetManager.CONSTANT)`. |
| GameLogger for all logging | Never `System.out.println`. Use `GameLogger.log(channel, owner, message)`. |
| Isometric rendering | Use `ZComparator` and `RenderUtils` for any new rendered entities. |
| Component naming | Classes ending with `Component`, systems with `System`. |

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| stats.name string matching | unitTypeKey enum-style string | Pre-Phase 4 | Safer — keys are factory-canonical, not display names |
| Separate attack button mode | Context-sensitive click on enemy tile | Phase 4 decision | Faster hot-seat play, no modal state to manage |

---

## Open Questions

1. **WRAITH unit status**
   - What we know: Referenced in stealth render logic and UnitFactory cloaking, but no stats/texture defined
   - What's unclear: Is WRAITH planned for a future phase or is it dead code to be removed?
   - Recommendation: Plan a task to either formally define WRAITH (add to Phase 9 or later) or remove the orphaned references and document the decision

2. **Cost duplication between createUnit and getUnitCost**
   - What we know: Two switch statements must stay in sync manually; a change to one doesn't update the other
   - What's unclear: Is this considered acceptable debt or should it be refactored?
   - Recommendation: The CombatRosterTest (Wave 0 gap) should assert both return identical values per key — this serves as a regression guard without requiring a refactor

---

## Environment Availability

Step 2.6: SKIPPED — Phase 4 is code-only. No external tools, databases, or services are required beyond the existing Gradle build.

---

## Sources

### Primary (HIGH confidence)
- Direct codebase read: `CombatSystem.java`, `UnitRenderSystem.java`, `UnitFactory.java`, `AnimationSystem.java`, `FloatingTextSystem.java`, `GameInputController.java`, `StatsComponent.java`, `DeathAnimComponent.java`, `FloatingTextComponent.java`, `AnimationComponent.java`, `AbilityTest.java`
- `.planning/phases/04-unit-roster-combat/04-CONTEXT.md` — locked user decisions
- `.planning/phases/04-unit-roster-combat/04-UAT.md` — phase verified complete 2026-03-23

### Secondary (MEDIUM confidence)
- `ARCHITECTURE.md`, `STACK.md`, `CLAUDE.md` — project conventions

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in project, no new deps
- Architecture: HIGH — all patterns read directly from implemented source
- Pitfalls: HIGH — identified from actual code guards and test patterns in the codebase
- Debt analysis: HIGH — all four flagged concerns traced to exact file and line

**Research date:** 2026-03-26
**Valid until:** 2026-04-26 (stable codebase — no fast-moving deps)
