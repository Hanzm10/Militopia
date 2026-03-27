---
phase: 08-polish-ux
verified: 2026-03-27T00:00:00Z
status: gaps_found
score: 6/8 must-haves verified
gaps:
  - truth: "Sound effects play for movement, attack, defend, and background music"
    status: partial
    reason: "Defend (counterattack) SFX is not wired — no playSFX call exists in the counterattack resolution branch of CombatSystem. Background music is never triggered — playBGM() exists in AudioManager but is called from nowhere in the codebase, and the assets/audio/bgm/ directory does not exist."
    artifacts:
      - path: "core/src/main/java/com/militopia/systems/CombatSystem.java"
        issue: "Counterattack branch (lines 230-258) fires no SFX. Only hit.wav is played when the defender survives, but no separate defend/counter sound plays from the defender's perspective."
      - path: "core/src/main/java/com/militopia/managers/AudioManager.java"
        issue: "playBGM() is fully implemented but never called from any game screen, system, or entry point."
    missing:
      - "Add AudioManager.getInstance().playSFX() call inside the counterattack block (CombatSystem lines ~240-257) for a defend SFX."
      - "Create assets/audio/bgm/ directory with at least one BGM file."
      - "Add a playBGM() call in GameScreen.show() or MilitopiaGame to start background music on game entry."
human_verification:
  - test: "Verify attack and defend animations are visually distinct per unit type at runtime"
    expected: "Melee units (RECRUIT) lunge toward target; ranged units (SNIPER, RANGER, TANK) show a recoil pop-back. Counterattacking defender should produce a visible response."
    why_human: "Animation rendering requires a running game instance; cannot verify visual output from static code."
  - test: "Verify floating text appears correctly above bases and structures"
    expected: "At turn start, +$N income and +N XP text floats upward and fades above each owned base. XP from structures also produces floating text."
    why_human: "World-space rendering requires a running game instance."
  - test: "Verify Ctrl+Z undoes the immediately preceding action"
    expected: "After moving a unit, pressing Ctrl+Z returns it to the previous tile. After an attack, pressing Ctrl+Z restores both units to pre-attack HP. Right panel reflects the reduced history count."
    why_human: "Stateful interaction requires a running game instance."
---

# Phase 8: Polish UX — Verification Report

**Phase Goal:** Comprehensive UI overhaul, dynamic visual feedback, animations, and sound effects.
**Verified:** 2026-03-27
**Status:** gaps_found — 6/8 truths verified
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Base UI uses PNG assets and features a sliding info panel instead of floating text | VERIFIED | `InfoPanel.java` exists (469 lines), uses `Actions.moveTo` with `Interpolation.pow2Out/In` (lines 437, 324). PNG assets `BTN_SLIDEDOWN` and `FUNDING_ICON` loaded via `AssetManager`. `slidedown_btn.png`, `funding_icon.png` exist on disk. |
| 2 | Territory indicator properly expands when a base reaches Level 4 | VERIFIED | `BaseLevelConfig` Level 4 entry uses `BORDER_RADIUS + 1`. `UnitFactory.checkAndApplyLevelUp()` sets `stats.vision = data.borderRadius` (line 687). `MapRenderSystem` uses `s.vision` as the territory radius (line 78). Chain is complete. |
| 3 | Attack animations play for each unit type | VERIFIED | `AnimationComponent.java` defines LUNGE/PROJECTILE/HIT_FLASH types. `CombatSystem.triggerAttackAnimation()` (lines 459-487) sets LUNGE for range-1 units and a recoil LUNGE for ranged units. `AnimationSystem` processes `stateTime += deltaTime` per entity. |
| 4 | Sound effects play for movement, attack, defend, and background music | FAILED | Movement SFX wired (move-land.WAV, move-water.WAV in GameInputController lines 705/707). Attack SFX wired per unit type (recruit-knife, sniper-awp, tank-fire, ranger-ak47, explode). **Defend SFX: missing** — counterattack block (CombatSystem lines 230-258) fires no playSFX. **BGM: missing** — playBGM() is never called anywhere; `assets/audio/bgm/` directory does not exist. |
| 5 | HUD top bar updates live when players receive additional income/funding mid-turn | VERIFIED | `HudTopBar.updateFunding()` (lines 81-107) and `updateXP()` (lines 58-74) both fire `Actions.scaleTo` + `Actions.color` immediately on value change. Called via `GameHUD.updateFunding()` whenever funding changes (e.g. after summoning a unit, `screen.gameHUD.updateFunding(remaining, income)` in InfoPanel line 238). |
| 6 | Floating text popups appear above bases when gaining income, funding, or XP | VERIFIED | `StructureEconomySystem.processTurn()` calls `entityFactory.createFloatingText()` for structure XP (line 116), base income (line 171), and base natural XP (line 176). `FloatingTextSystem` drifts worldY upward and fades alpha. `UnitRenderSystem.drawFloatingTexts()` renders FUNDING/XP/DAMAGE types with color coding. |
| 7 | Action undo allows canceling moves before committing | VERIFIED | `GameInputController` calls `snapshot()` before every discrete action (move line 683, attack line 297, hunt line 463, ability lines 502/521/533). `Ctrl+Z` detected in `keyDown()` (lines 130-132), calls `screen.undoTurn()`. Right panel in `GameHUD` shows history via `refreshSnapshotPanel()`, includes always-visible "Undo" TextButton. |
| 8 | Units with attack range 1 automatically advance into defender's tile on lethal attack | VERIFIED | `CombatSystem.resolveAttack()` lines 207-213: when `aStats.attackRange <= 1` and defender HP drops to 0, `aPos.x = dPos.x; aPos.y = dPos.y` executes after `flagDeath()`. RECRUIT also gets a `MovementComponent` for the visual glide animation. |

**Score:** 6/8 truths verified

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `core/src/main/java/com/militopia/ui/InfoPanel.java` | Sliding info panel with PNG assets | VERIFIED | 469 lines, Actions.moveTo wired, PNG close button and icon loaded |
| `core/src/main/java/com/militopia/ui/HudTopBar.java` | Live-updating top bar with scale/color animations | VERIFIED | updateFunding() and updateXP() both fire Actions.scaleTo + Actions.color |
| `core/src/main/java/com/militopia/components/FloatingTextComponent.java` | Ashley Component with FUNDING/XP/DAMAGE types | VERIFIED | 37 lines, Type enum includes DAMAGE/BLOCKED/XP/FUNDING |
| `core/src/main/java/com/militopia/systems/FloatingTextSystem.java` | IteratingSystem that drifts and fades text, removes entities | VERIFIED | 29 lines, `getEngine().removeEntity()` called when timer >= MAX_TIME |
| `core/src/main/java/com/militopia/managers/AudioManager.java` | Singleton SFX/BGM manager with 5-per-frame rate limiting | VERIFIED (partial) | SFX working with rate-limit; BGM capability never triggered |
| `core/src/main/java/com/militopia/components/AnimationComponent.java` | stateTime, loop, LUNGE/PROJECTILE/HIT_FLASH enum | VERIFIED | 36 lines, all fields present |
| `core/src/main/java/com/militopia/systems/AnimationSystem.java` | IteratingSystem processing stateTime += deltaTime | VERIFIED | LUNGE/PROJECTILE/HIT_FLASH cases handled |
| `core/src/main/java/com/militopia/systems/CombatSystem.java` | Melee advance + per-unit SFX + counterattack SFX | STUB (partial) | Melee advance verified. Attack SFX verified. Counterattack SFX absent. |
| `core/src/main/java/com/militopia/controller/GameInputController.java` | Pre-action snapshots + Ctrl+Z handler | VERIFIED | snapshot() before all 5 action types; keyDown Ctrl+Z detected |
| `core/src/main/java/com/militopia/managers/TurnHistoryManager.java` | size() method, 50-snapshot limit | VERIFIED | size() at line 47; MAX_HISTORY = 50 |
| `core/src/main/java/com/militopia/ui/GameHUD.java` | Snapshot right panel + Undo button | VERIFIED | buildSnapshotPanel(), refreshSnapshotPanel(), undoBtn wired |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `GameScreen.build()` | `GameHUD.build()` | Passes `turnHistory` arg | WIRED | Line 211: `gameHUD.build(this, inputController, unitFactory, gameState, turnHistory)` |
| `StructureEconomySystem` | `EntityFactory.createFloatingText()` | Called with FUNDING/XP types | WIRED | Lines 116, 171, 176 |
| `CombatSystem` | `AudioManager.playSFX()` | Attack branch | WIRED | Lines 164-183 per unit type |
| `CombatSystem` | `AudioManager.playSFX()` | Counterattack branch | NOT WIRED | Lines 230-258 contain no playSFX call |
| Any caller | `AudioManager.playBGM()` | Game start/screen transition | NOT WIRED | No call site found in entire codebase |
| `BaseLevelConfig` Level 4 | `MapRenderSystem` territory radius | `UnitFactory.checkAndApplyLevelUp()` sets `stats.vision = borderRadius` | WIRED | Level 4 = `BORDER_RADIUS + 1`; MapRenderSystem reads `s.vision` |
| `MilitopiaGame.render()` | `AudioManager.update()` | Frame-level rate-limit reset | WIRED | MilitopiaGame.java line 83 |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|-------------------|--------|
| `HudTopBar` `fundsLabel` | `funding` int | `GameHUD.updateFunding()` called after every state change | Yes — reads `state.p1Funding` | FLOWING |
| `UnitRenderSystem.drawFloatingTexts()` | `FloatingTextComponent.text` | `EntityFactory.createFloatingText()` from economy values | Yes — reads `stats.income`, `naturalGain` | FLOWING |
| `GameHUD.snapshotList` | `turnHistory.size()` | `TurnHistoryManager.size()` after each push/undo | Yes — reflects real stack depth | FLOWING |

---

## Behavioral Spot-Checks

Step 7b: SKIPPED — project requires a running libGDX desktop instance to verify rendering and audio. No runnable headless entry point exists for these systems (CLAUDE.md: "No test suite exists — manual play-testing is the verification method").

---

## Requirements Coverage

No `requirements:` field declared in any of the three PLAN files (all set to `[]`). Phase 08 maps to no formal REQUIREMENTS.md entries — this was a polish/UX phase driven by the roadmap goal rather than tracked requirements.

---

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `CombatSystem.java` | ~230-258 | Counterattack branch fires no defend SFX | Warning | Success Criterion 4 partially unmet |
| `AudioManager.java` | 69 | `playBGM()` fully implemented but 0 call sites | Blocker | BGM never plays; no `bgm/` asset directory exists |
| `CombatSystem.java` | ~179-183 | `attack_ranged.wav` and `attack_melee.wav` referenced as generic fallbacks but these files do not exist in `assets/audio/sfx/` | Warning | Generic fallback SFX silently fail; only named-unit SFX (recruit-knife, sniper-awp, etc.) actually play |

**Missing SFX files referenced in code:**
- `attack_ranged.wav` — not present in `assets/audio/sfx/`
- `attack_melee.wav` — not present in `assets/audio/sfx/`
- `hit.wav` — not present in `assets/audio/sfx/`

AudioManager silently swallows `Exception` on missing files (line 105), so the game does not crash but the sounds do not play.

---

## Human Verification Required

### 1. Per-unit-type Attack Animations

**Test:** Start a game. Attack with RECRUIT (melee), SNIPER (ranged), TANK, and RANGER in sequence.
**Expected:** RECRUIT lunges toward the defender tile. SNIPER, RANGER, TANK show a brief recoil pop-back. Defender shows a hit flash when damaged.
**Why human:** AnimationSystem and UnitRenderSystem render visual offsets at runtime; cannot verify visual quality from static analysis.

### 2. Floating Text Above Bases

**Test:** End a turn. Observe owned bases.
**Expected:** "+$N" income text and "+N XP" text float upward from the base tile and fade out over ~1 second.
**Why human:** World-space rendering via UnitRenderSystem requires a running game instance.

### 3. Ctrl+Z Undo Correctness

**Test:** Move a unit. Press Ctrl+Z. Then perform an attack. Press Ctrl+Z again.
**Expected:** Move is rewound to pre-move position. Attack is rewound to pre-attack HP on both units. Right panel history count decrements each time.
**Why human:** Multi-step stateful restore requires interactive verification.

---

## Gaps Summary

Two gaps block full success criterion 4 ("Sound effects play for movement, attack, defend, and background music"):

**Gap 1 — Defend SFX not wired:** The counterattack resolution in `CombatSystem.resolveAttack()` (lines 230-258) applies counter-damage and logs it but never calls `AudioManager.getInstance().playSFX()`. There is no defender/counter sound. The plan specified "Sound effects play for... defend."

**Gap 2 — Background music never triggered:** `AudioManager.playBGM()` is fully implemented but has zero call sites in the entire codebase. The `assets/audio/bgm/` directory does not exist. BGM is dead code as-delivered. The plan and success criteria explicitly require background music to play.

**Adjacent issue — Missing generic fallback SFX files:** `attack_ranged.wav`, `attack_melee.wav`, and `hit.wav` are referenced in `CombatSystem` but do not exist on disk. AudioManager catches the exception silently. Only the named-unit SFX files (recruit-knife, sniper-awp, tank-fire, ranger-ak47, explode) actually exist and play.

All other 6 success criteria are fully met with clean, substantive implementations.

---

_Verified: 2026-03-27_
_Verifier: Claude (gsd-verifier)_
