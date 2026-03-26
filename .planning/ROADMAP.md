# Roadmap: Militopia

## Overview

Militopia is a 2-player turn-based strategy game. This roadmap tracks the development from foundation to release.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions

- [x] **Phase 0: Foundation** - Project scaffolded and core rendering
- [x] **Phase 1: Turn Engine & Economy** - Turn system and funding logic
- [x] **Phase 2: Base Progression** - Base XP and level-up system
- [x] **Phase 3: Capture & Territory** - Structure capturing and Fog of War
- [x] **Phase 4: Unit Roster & Combat** - Core combat loop and unit stats
- [x] **Phase 4.1: Unit & Building Abilities** - Unique skills for units and structures
- [x] **Phase 5: Specialized Structures** - Construction system and building logic
- [x] **Phase 6: Win / Loss Conditions** - Win conditions and game over state
- [x] **Phase 7: Exploration & Persistence** - Ruins mechanics, Oil extraction, and move persistence
- [/] **Phase 8: Polish & UX** - UI overhaul, unit animations, text popups, and sound effects (Wave 1 Complete)
- [ ] **Phase 9: Advanced Mechanics** - Railways infrastructure

## Phase Details

### Phase 0: Foundation
**Goal**: libGDX Ashley ECS setup with isometric tile renderer and camera.
**Depends on**: Nothing
**Success Criteria** (what must be TRUE):
  1. Isometric tiles render correctly with Z-order.
  2. Procedural map generation works with seed.
  3. ECS engine runs with basic systems.
**Plans**: Complete

### Phase 1: Turn Engine & Economy
**Goal**: Multi-player turn management and per-turn income distribution.
**Depends on**: Phase 0
**Success Criteria** (what must be TRUE):
  1. Players can end turns and active player flips.
  2. Income is calculated and distributed at turn start.
  3. Testing mode disables unit action enforcement.
**Plans**: Complete

### Phase 2: Base Progression
**Goal**: Base leveling system with XP growth and stat scaling.
**Depends on**: Phase 1
**Success Criteria** (what must be TRUE):
  1. Bases gain XP every turn.
  2. Level-up triggers income and vision increases.
  3. UI popups show unlocked units/structures.
**Plans**: Complete

### Phase 3: Capture & Territory
**Goal**: Capture towns and bases to expand territory and visibility.
**Depends on**: Phase 2
**Success Criteria** (what must be TRUE):
  1. Units can capture unowned or enemy structures.
  2. Captured structures contribute to the owner's income and XP.
  3. Fog of War correctly hides tiles outside vision range.
**Plans**: Complete

### Phase 4: Unit Roster & Combat
**Goal**: Full unit roster with Land/Sea/Air domains and combat resolution.
**Depends on**: Phase 3
**Success Criteria** (what must be TRUE):
  1. 10 units are summonable with correct costs and stats.
  2. Combat uses ATK-DEF formula with terrain/range modifiers.
  3. Death animations play before entity removal.
**Plans**: Complete

### Phase 4.1: Unit & Building Abilities
**Goal**: Unique tactical abilities for every unit and building.
**Depends on**: Phase 4
**Success Criteria** (what must be TRUE):
  1. Dig In, Overwatch, and Blitz abilities function correctly.
  2. Stealth mechanics (Cloak/Camouflage) hide units from enemies.
  3. AOE attacks (Suppressing Fire, Nuke) hit multiple tiles.
**Plans**: Complete

### Phase 5: Specialized Structures
**Goal**: Implement the building construction system and finalize specialized building interactions.
**Depends on**: Phase 4.1
**Success Criteria** (what must be TRUE):
  1. Build menu allows placing structures within base territory.
  2. Ports enable Sea unit summoning on Water tiles.
  3. Hospitals and Solar Arrays provide per-turn bonuses.
**Plans**: 
  - 05-01: Parent-Base Linking & Placement
  - 05-02: Port Interaction & Naval Spawning
  - 05-03: Per-Turn Economy Bonuses
  - 05-04: Strict Coastal Placement & Adjacent Spawning

### Phase 6: Win / Loss Conditions
**Goal**: Define win condition and handle game termination.
**Depends on**: Phase 5
**Success Criteria** (what must be TRUE):
  1. Destroying all enemy bases triggers victory.
  2. Game over screen displays winner and returns to menu.
**Plans**: TBD

### Phase 7: Exploration & Persistence
**Goal**: Add map exploration rewards and resource extraction constraints.
**Depends on**: Phase 6
**Success Criteria** (what must be TRUE):
  1. Units entering Ruins trigger randomized rewards (+FP, +XP, or unit spawns).
  2. Oil Derricks can only be constructed on "Oil Reservoir" tiles.
  3. Movement state is preserved upon game save/exit mid-turn.
**Plans**: 
  - 07-01: Ruins Mechanics & Random Rewards
  - 07-02: Oil Resource Constraints
  - 07-03: Turn State Persistence

### Phase 8: Polish & UX
**Goal**: Comprehensive UI overhaul, dynamic visual feedback, animations, and sound effects.
**Depends on**: Phase 7
**Success Criteria** (what must be TRUE):
  1. Base UI uses PNG assets and features a sliding info panel instead of floating text.
  2. Territory indicator properly expands when a base reaches Level 4 (Bug Fix).
  3. Attack animations play for each unit type.
  4. Sound effects play for movement, attack, defend, and background music.
  5. HUD top bar updates live when players receive additional income/funding mid-turn.
  6. Floating text popups appear above bases when gaining income, funding, or XP.
  7. Action undo allows canceling moves before committing.
  8. Units with an attack range of 1 automatically advance into the defender's tile upon a lethal attack.
**Plans**: TBD

### Phase 9: Advanced Mechanics
**Goal**: Implement advanced mobility and infrastructure.
**Depends on**: Phase 8
**Success Criteria** (what must be TRUE):
  1. Railway feature (inspired by Polytopia roads) is implemented for enhanced unit mobility.
**Plans**: TBD

---
| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 0. Foundation | 1/1 | Complete | 2025-12-22 |
| 1. Turn Engine | 1/1 | Complete | 2026-01-10 |
| 2. Base Progression | 1/1 | Complete | 2026-01-25 |
| 3. Capture & Territory | 1/1 | Complete | 2026-02-05 |
| 4. Unit Roster | 1/1 | Complete | 2026-02-15 |
| 4.1 Unit Abilities | 1/1 | Complete | 2026-02-22 |
| 5. Specialized Structures | 4/4 | Complete | 2026-03-23 |
| 6. Win / Loss | 1/1 | Complete | 2026-03-23 |
| 7. Exploration | 1/1 | Complete | 2026-03-24 |
| 8. Polish & UX | 1/7 | In Progress | 2026-03-26 (W1) |
| 9. Adv. Mechanics | 0/1 | Planned | - |
