---
phase: 05-specialized-structures
status: COMPLETE
verified: 2026-03-23
---

# Phase 5 UAT: Specialized Structures

## UAT 5.1 — Structure Placement Validation
**Given** a player is in territory at a coastal water tile  
**When** they open the Build Menu  
**Then** only PORT is shown (not Oil Derrick, etc.)  
**Result:** ✅ VERIFIED — `SlideMenu.populateBuildMenu` enforces `isCoastalWater` for PORT.

## UAT 5.2 — Port Sea-Unit Filter
**Given** a player owns a Port and clicks it  
**When** the summon menu opens  
**Then** only Sea units (Gunboat, Destroyer, Carrier) are shown  
**Result:** ✅ VERIFIED — `populateSummonMenu` filters by `StatsComponent.MoveType.SEA` when `producerType == "PORT"`.

## UAT 5.3 — Base No-Sea Filter
**Given** a player owns a Base and clicks it  
**When** the summon menu opens  
**Then** Sea units (Gunboat, Destroyer, Carrier) are NOT shown  
**Result:** ✅ VERIFIED — `populateSummonMenu` excludes `MoveType.SEA` units for `producerType == "BASE"`.

## UAT 5.4 — Structure Economy Per Turn
**Given** a player owns an Oil Derrick (+6 income) linked to a Base  
**When** they end their turn  
**Then** their funding increases by +6 more than a turn without the structure  
**Result:** ✅ VERIFIED — `StructureEconomySystem.processTurn()` distributes `stats.income` via `calculateIncome()` and XP via structure loop.

## UAT 5.5 — Hospital Heals Adjacent Units
**Given** a Hospital is at (3,3) and a friendly damaged unit is at (3,4)  
**When** the player's turn begins  
**Then** the unit gains +3 HP (capped at maxHP)  
**Result:** ✅ VERIFIED — `StructureEconomySystem` heals via chebyshev distance ≤ 1 check. Verified by `PortSummonTest.testHospitalHealsAdjacentFriendlyUnit`.
