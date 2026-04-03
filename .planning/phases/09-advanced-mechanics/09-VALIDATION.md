---
phase: 9
slug: advanced-mechanics
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-03
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Manual play-testing (no automated test suite — per CLAUDE.md) |
| **Config file** | none |
| **Quick run command** | `./gradlew run` — launch game, inspect in-game |
| **Full suite command** | `./gradlew run` — full manual play session |
| **Estimated runtime** | ~2 minutes per verification pass |

---

## Sampling Rate

- **After every task commit:** Build with `./gradlew build` and verify no compile errors
- **After every plan wave:** Run game and manually verify wave deliverables
- **Before `/gsd:verify-work`:** Full manual play session covering all behaviors below
- **Max feedback latency:** 1 plan wave

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Manual Check | Status |
|---------|------|------|-------------|-----------|-------------------|--------------|--------|
| 09-01-01 | 01 | 1 | GameMap rails | build | `./gradlew build` | `boolean[][] rails` compiles | ⬜ pending |
| 09-01-02 | 01 | 1 | GameState persist | build | `./gradlew build` | `railGrid` field compiles | ⬜ pending |
| 09-01-03 | 01 | 2 | Rail rendering | manual | `./gradlew run` | Steel gray lines visible on placed rails | ⬜ pending |
| 09-01-04 | 01 | 2 | Rail connectivity | manual | `./gradlew run` | Lines connect adjacent rail tiles correctly | ⬜ pending |
| 09-01-05 | 01 | 3 | floodFill ×2 | manual | `./gradlew run` | Land unit crosses 4 rails = reaches 4 extra tiles | ⬜ pending |
| 09-01-06 | 01 | 3 | Build action | manual | `./gradlew run` | SlideMenu shows "Build Railway", deducts 3 funding | ⬜ pending |
| 09-01-07 | 01 | 4 | Save/load | manual | `./gradlew run` | Rails persist after save + reload | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- None — no test framework to install. Manual play-testing per CLAUDE.md convention.

*Existing infrastructure covers all phase requirements via manual verification.*

---

## Manual-Only Verifications

| Behavior | Why Manual | Test Instructions |
|----------|------------|-------------------|
| Steel gray rail lines render on tile | No automated rendering test | Place rail on tile, run game, visually confirm gray lines over terrain |
| Rail connectivity lines update when neighbor added | No automated rendering test | Place two adjacent rails, confirm lines connect between them |
| Land unit gets double movement on rail-to-rail steps | No BFS unit test | Move infantry across 4 consecutive rail tiles, confirm it reaches 4 tiles further than normal |
| Both-tile rule enforced | No BFS unit test | Place single isolated rail, move unit adjacent to it, confirm no extra range |
| AIR/SEA units unaffected | No BFS unit test | Move helicopter over rail tiles, confirm range unchanged |
| Rail build blocked on water/mountain | No terrain validation test | Select water or mountain tile, confirm "Build Railway" is absent or disabled |
| Enemy rail gives no bonus | No territory test | Move unit into enemy-owned rail tile, confirm normal movement cost |
| Rails persist after save/load | No serialization test | Build rails, save game, reload, confirm rails still render |
| +3 funding deducted on build | No economy test | Build a rail, confirm player's funds decrease by 3 |

---

## Validation Sign-Off

- [ ] All waves verified manually in-game
- [ ] Both-tile rule confirmed working
- [ ] Save/load persistence confirmed
- [ ] No regression in territory borders or selection indicator (same ShapeRenderer pass)
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
