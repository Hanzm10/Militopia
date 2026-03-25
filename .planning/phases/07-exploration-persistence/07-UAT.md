---
phase: 07-exploration-persistence
status: COMPLETE
verified: 2026-03-24
---

# Phase 7 UAT: Exploration & Persistence

### 1. Ruins Interaction (Scavenge)
expected: Unit entering a RUINS tile triggers a randomized reward (+Funding, +XP, or unit spawn).
result: pass
reported: Verified in `ExplorationPersistenceTest` and `07-01-SUMMARY.md`.

### 2. Oil Derrick Constraints
expected: Oil Derricks can only be built on OIL_RESERVOIR tiles.
result: pass
reported: Verified in `StructurePlacementTest` and `StructurePlacementSystem`.

### 3. Coastal Port Constraints
expected: Ports only appear in the build menu for coastal water tiles.
result: pass
reported: Verified in `StructurePlacementTest`.

### 4. Turn-State Persistence
expected: `hasMoved` and `hasActed` flags are correctly saved and restored via JSON.
result: pass
reported: Verified in `ExplorationPersistenceTest` and `SaveManager`.

## Summary

total: 4
passed: 4
issues: 0
pending: 0
skipped: 0
