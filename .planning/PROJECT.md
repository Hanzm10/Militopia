# Militopia

## What This Is

Militopia is a two-player, turn-based military strategy game played on an isometric hex-adjacent grid. Players capture territory, manage an economy, build structures, and summon military units to destroy the enemy's bases.

## Core Value

A tactically deep, heterogeneous unit combat system on a procedurally generated isometric map.

## Requirements

## Requirements

### Validated

- ✓ Isometric tile renderer (Ashley ECS) — v1.0
- ✓ Procedural map generation (Simplex noise) — v1.0
- ✓ Turn system & Economy core — v1.0
- ✓ Base progression & Level-up system — v1.0
- ✓ Capture & Territory (Fog of War) — v1.0
- ✓ 13 playable units (Land/Sea/Air) — v1.0
- ✓ Unit abilities (Stealth, AOE, Blitz) — v1.0
- ✓ Specialized structures (Ports, Oil Derricks, Hospitals) — v1.0
- ✓ Win / Loss conditions (GameOverPopup) — v1.0
- ✓ Exploration rewards (Ruins) & Persistence — v1.0
- ✓ Polish & UX (SFX, Animations, Undo) — v1.0
- ✓ Railways Infrastructure — v1.0

### Active

- [ ] AI Opponent (v2.0)
- [ ] Online Multiplayer (v2.0)
- [ ] Map Editor (v2.0)

### Out of Scope

- Mobile app — web-first/desktop-first approach
- 3D Rendering — committed to pixel-art isometric aesthetic

## Context

Militopia has reached v1.0 with ~12,000 LOC of Java code. The engine is stable, featuring a modular Ashley ECS architecture and a custom Scene2D-based UI. 

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Ashley ECS | Decoupled data/logic for game systems | ✓ Good |
| Pre-action Snapshots | Enables robust Ctrl+Z rewind logic | ✓ Good |
| x2 Integer Budget | Avoids floating-point errors in movement math | ✓ Good |

---
*Last updated: 2026-04-03 — v1.0 MVP Shipped (all 11 phases complete)*
