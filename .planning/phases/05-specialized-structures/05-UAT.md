---
phase: 05-specialized-structures
status: COMPLETE
verified: 2026-03-23
---

# Phase 5 UAT: Specialized Structures

### 1. Structure Placement Validation
expected: In coastal water territory, only PORT is shown in the Build Menu (no Oil Derrick, etc.).
result: verified
reported: `SlideMenu.populateBuildMenu` enforces `isCoastalWater` for PORT.

### 2. Port Sea-Unit Filter
expected: When opening the summon menu for a Port, only Sea units (Gunboat, Destroyer, Carrier) are shown.
result: verified
reported: `populateSummonMenu` filters by `StatsComponent.MoveType.SEA` when `producerType == "PORT"`.

### 3. Base No-Sea Filter
expected: When opening the summon menu for a Base, Sea units are NOT shown.
result: verified
reported: `populateSummonMenu` excludes `MoveType.SEA` units for `producerType == "BASE"`.

### 4. Structure Economy Per Turn
expected: Oil Derrick linked to a Base increases funding by +6 more than a turn without the structure.
result: verified
reported: `StructureEconomySystem.processTurn()` distributes `stats.income` via `calculateIncome()`.

### 5. Hospital Heals Adjacent Units
expected: Friendly damaged unit adjacent to a Hospital at turn start gains +3 HP (capped at maxHP).
result: verified
reported: `StructureEconomySystem` heals via chebyshev distance ≤ 1 check.

### 6. Solar Array Stats
expected: Solar Array provides +75 XP and +3 Income per turn.
result: verified
reported: `UnitFactory.createStructure` sets stats, `StructureEconomySystem` processes XP, `GameScreen.calculateIncome` handles funding.
