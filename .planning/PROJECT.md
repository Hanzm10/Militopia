# Militopia

## What This Is

Militopia is a two-player, turn-based military strategy game played on an isometric hex-adjacent grid. Players capture territory, manage an economy, build structures, and summon military units to destroy the enemy's bases.

## Core Value

A tactically deep, heterogeneous unit combat system on a procedurally generated isometric map.

## Requirements

### Validated

- ✓ Isometric tile renderer (Ashley ECS)
- ✓ Procedural map generation
- ✓ Turn system & Economy core
- ✓ Base progression & Level-up system
- ✓ Capture & Territory mechanics
- ✓ Fog of War visibility system
- ✓ 10 playable units with Land/Sea/Air domains
- ✓ Unit abilities (Phase 4.1)
- ✓ Specialized structure logic (Phase 5)
- ✓ Win / Loss conditions (Phase 6)

### Active

- [/] Exploration & Persistence (Phase 7)
- [/] Polish & UX (Animations, SFX, BGM) (Phase 8) - Wave 1 Finished <!-- id: 7 -->

### Out of Scope

- AI opponent (v2)
- Online multiplayer (v2)
- Map editor (v2)

## Context

The game uses libGDX with Ashley ECS for high-performance entity management. The map is generated using Simplex noise. Combat resolution follows a deterministic damage formula influenced by terrain and range.

## Constraints

- **Tech Stack**: Java 8+, libGDX, Ashley ECS
- **Platform**: Desktop (LWJGL3)
- **Controls**: Mouse-driven (drag-to-pan, scroll-to-zoom)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Ashley ECS | Decoupled data/logic for game systems | ✓ Good |
| Isometric Grid | Visual depth and tactical clarity | ✓ Good |
| Damage Formula | ATK - DEF + terrain - range penalty | ✓ Good |

---
*Last updated: 2026-02-23 after project migration to GSD*
