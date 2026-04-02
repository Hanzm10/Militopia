# Militopia — SPEC.md

> **Version:** 0.1 · **Engine:** libGDX + Ashley ECS · **Platform:** Desktop (LWJGL3)

---

## 1. Vision

Militopia is a **two-player, turn-based military strategy game** played on an isometric hex-adjacent grid. Players capture territory, manage an economy, build structures, and summon military units to destroy the enemy's bases.

---

## 2. Core Pillars

1. **Territory & Expansion** — Capture bases and towns to grow income and unlock units.
2. **Economy & Funding** — Every strategic decision has a funding cost. Income scales with level.
3. **Tactical Combat** — Heterogeneous unit roster (Land/Sea/Air) with range, vision, and move-type constraints.
4. **Fog of War** — Incomplete information forces scouting and forward positioning.
5. **Base Progression** — Bases level up via XP, unlocking stronger units and structures.

---

## 3. World

### 3.1 Map
- Procedurally generated per seed. Size configurable at game start (default 16×16).
- **Terrain types:** `DEEP_WATER`, `WATER`, `SAND`, `GRASS`, `MOUNTAIN`
- **Objects:** `BASE_P1`, `BASE_P2`, `TOWN`, `OIL`, `RUINS`, `TREE`, `CACTUS`, `MOUNTAIN_OBJ`
- **Wild Animals:** `HORSE`, `DEER`, `ZEBRA`, `FISH` — aesthetic fauna tied to terrain type, spawn near bases.
- Map generation rules:
  - Bases placed in opposite quadrants, minimum separation enforced.
  - 8 Towns placed on grass, at least 2.5 tiles apart, 3-tile margin from edges.
  - 1–2 OIL deposits and 0–1 RUINS spawned near every base/town.
  - 20% chance of Tree on Grass, 5% Cactus on Sand.

### 3.2 Isometric Rendering
- Tiles rendered in isometric perspective. Z-order: terrain → structures (z=1) → animals (z=2) → units (z=3).
- Camera: `OrthographicCamera` with zoom range 0.2–2.0, drag-to-pan, auto-center on active player's base on turn change.

---

## 4. Players

- **2 players**, alternating turns on the same machine (hot-seat).
- Player 1 = **Blue**. Player 2 = **Red**.
- Custom player names set at game start.

---

## 5. Turn System

1. Active player takes all actions (move, attack, capture, build, summon).
2. Player clicks **"End Turn"**.
3. Fade-out → economy resolved → fog updated → camera snaps to new player's base → Fade-in.
4. Turn counter increments when Player 1's turn begins again (after Player 2 ends).
5. **Income is not distributed on Turn 1** (first round free).
6. **`TESTING_MODE`** flag in `GameConfig` disables the `hasActed` enforcement for debugging.

---

## 6. Economy

### 6.1 Funding
- Each player has `pXFunding` (starting value: 5).
- At turn start (after Turn 1), funding increases by the player's total `income`.
- `income` = sum of `StatsComponent.income` for all entities owned by the player.

### 6.2 Income Sources
| Source | Income/Turn |
|---|---|
| Base (any level) | 2–3 (scales with level) |
| Town (Captured) | 1 |
| Munitions Factory | +2 |
| Solar Array | +3 |
| Oil Derrick | +6 |
| Nuclear Plant | +15 |

### 6.3 Level-Up Funding Bonus
- Some base level-ups grant an **immediate cash bonus** on top of regular income (see Section 8).

---

## 7. Units

### 7.1 Action Rules
- Each unit may **move once** and **act once** per turn (attack or capture, not both after moving).
- After acting, `hasActed = true`; unit is visually grayed out and unselectable.
- `hasActed` resets for all units at the start of each player's turn.

### 7.2 Movement Types
| Type | Terrain |
|---|---|
| `LAND` | Grass, Sand (blocked by Water, Deep Water) |
| `SEA` | Water, Deep Water (blocked by Land) |
| `AIR` | All terrain (ignores ground obstacles) |

### 7.3 Unit Roster

**Land Units** *(Spawn at Base)*

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Unlock Level |
|---|---|---|---|---|---|---|---|---|
| Recruit | 10 | 3 | 1 | 1 | 1 | 1 | 2 | 1 |
| Ranger | 12 | 5 | 1 | 1 | 2 | 2 | 5 | 2 |
| Sniper | 8 | 15 | 0 | 1 | 3 | 3 | 8 | 3 |
| Tank (MBT) | 30 | 12 | 5 | 2 | 3 | 3 | 15 | 4 |
| Juggernaut | 50 | 12 | 6 | 3 | 1 | 3 | — | 5 |

**Air & Sea Units**

| Unit | HP | ATK | DEF | MOV | RNG | VIS | Cost | Domain | Unlock Level |
|---|---|---|---|---|---|---|---|---|---|
| Recon Drone | 5 | 0 | 0 | 3 | 0 | 3 | 4 | AIR | 2 |
| Suicide Drone | 5 | 20 | 0 | 2 | 2 | 2 | 7 | AIR | 3 |
| Apache | 20 | 15 | 2 | 3 | 2 | 3 | 18 | AIR | 4 |
| Wraith (B2) | 45 | 18 | 3 | 3 | 3 | 3 | — | AIR | 5 |
| Gunboat | 10 | 5 | 2 | 2 | 2 | 2 | 6 | SEA | 2 |
| Destroyer | 30 | 15 | 3 | 3 | 3 | 3 | 13 | SEA | 3 |
| Carrier | 45 | 5 | 4 | 3 | 3 | 3 | 25 | SEA | 4 |
| Submarine | 40 | 25 | 3 | 4 | 4 | 3 | — | SEA | 5 |

> Cost `—` = unlocked via Base level-up, not purchasable directly.


### 7.4 Summoning
- Units can only be summoned from a **friendly base** the player owns.
- Cost is deducted from `pXFunding` immediately.
- Summoned units have `hasActed = true` (cannot act the turn they are summoned).

---

## 8. Bases & Progression

### 8.1 Base XP
- Bases accumulate XP each turn:
  - **Natural growth:** `250 + (level - 1) × 10` XP/turn.
  - **Structure bonus:** Each linked structure contributes `xpGain` per turn.
- XP from bases is also added to the player's **global XP pool** (`pXXP`).

### 8.2 Level Table

| Level | XP Required | Income | Vision | Funding Bonus | Units Unlocked | Structures Unlocked |
|---|---|---|---|---|---|---|
| 1 | 2000 | 2 | 1 | 5 | Recruit | Munitions Factory |
| 2 | 3000 | 3 | 1 | 0 | Ranger, Recon Drone, Gunboat | Port, Hospital |
| 3 | 4500 | 3 | 1 | 10 | Sniper, Suicide Drone, Destroyer | Oil Derrick, Radar |
| 4 | 6750 | 3 | 2 | 0 | Tank, Apache, Carrier | Solar Array, Jammer |
| 5 | 10125 | 3 | 2 | 10 | Juggernaut, Wraith, Submarine | Nuclear Plant |
| 6+ | × 1.5 each | 3 | 2 | 10 | — | — |

### 8.3 Level-Up Popup
- On base level-up: a popup displays the new level, funding bonus, and newly unlocked units/structures.

---

## 9. Structures

Structures are built within a base's border zone. They link to their parent base and contribute XP per turn.

| Structure | HP | Cost | Income | XP Gain | Build Zone | Special Effect |
|---|---|---|---|---|---|---|
| Base (City) | N/A | N/A | +2 | +250 | N/A | Core territory anchor |
| Munitions Factory | N/A | 5 | +2 | +50 | Inside Borders | **Adjacency Bonus:** eligible neighbor target |
| Solar Array | N/A | 8 | +3 | +75 | Inside Borders | **Adjacency Bonus:** +1 income for each adjacent friendly structure |
| Oil Derrick | N/A | 10 | +6 | +100 | Oil Reservoir | **Volatile:** explodes on death (15 dmg to 8 tiles). Can only be built on "Oil Reservoir" tiles. |
| Nuclear Plant | N/A | 40 | +15 | +150 | Coastline Only | **Meltdown:** tiles become Wasteland on death (3×3) |
| Field Hospital | N/A | 15 | 0 | +50 | Inside Borders | Heals adjacent units at turn start |
| Radar Station | N/A | 20 | 0 | +75 | Inside Borders | **Scanner:** reveals invisible units in radius |
| Signal Jammer | 10 | 25 | 0 | +75 | Inside Borders | **Static:** blocks enemy vision in 3-tile radius |
| Port | N/A | 7 | 0 | +50 | Coastline | Enables sea unit summoning |

---

## 10. Unit & Building Abilities

Each unit and structure has one unique ability. These define the tactical depth of the game.

| Unit / Building | Ability Name | Type | Effect | Implementation Note |
|---|---|---|---|---|
| Recruit | **Dig In** | Active | Spend 1 turn to gain +3 Defense | Creates temporary "Sandbag" object on tile |
| Ranger | **Overwatch** | Passive | Auto-attacks first enemy entering range during enemy turn | Limit 1 trigger per turn |
| Sniper | **Camouflage** | Passive | Invisible while on Forest/Ruins tiles | Revealed on attack or adjacent enemy |
| Tank (MBT) | **Blitz** | Passive | Can move again if attack kills a unit | Reset movePoints if `target.hp <= 0` |
| Juggernaut | **Suppressing Fire** | Passive | Attack hits all 8 adjacent tiles | Loop `x-1..x+1, y-1..y+1` |
| Recon Drone | **High Altitude** | Passive | Immune to melee (Range 1) land unit attacks | `if (attacker.range == 1 && attacker.isLand) damage = 0` |
| Suicide Drone | **Kamikaze** | Passive | Destroyed upon attacking; flies to target tile on attack | `this.kill()` after `attack()` |
| Apache | **Fuel Gauge & High Altitude** | Passive | 5 turns of fuel; crashes at 0. Immune to land melee. | `fuel--` / `isUnreachable` |
| Wraith (B2) | **Stealth Cloak** | Passive | Invisible on map; revealed only during attack | `isVisible = false` unless `isAttacking` |
| Gunboat | **Skirmish** | Passive | Can move 1 tile after attacking | Grant +1 Move Point after combat |
| Destroyer | **Shore Bombardment** | Passive | +5 bonus damage to Land Units | `if (target.isLand) damage += 5` |
| Carrier | **Mobile Airfield** | Passive | Heals & refuels adjacent Air units | Check adjacent tiles at turn start |
| Submarine | **Deep Dive** | Mixed | Moves under ships; invisible; Nuke (area damage, 3-turn cooldown) | Nuke needs cooldown timer |
| Solar Array | **Adjacency Bonus** | Passive | +1 income for each adjacent friendly structure | Loop adjacent entities; `if (owner == self && (income > 0 || isBase)) income += 1` |
| Oil Derrick | **Volatile** | Passive | Explodes on death (15 dmg, 8 tiles) | Trigger `explosion(x,y)` on `onDeath()` |
| Nuclear Plant | **Meltdown** | Passive | Explodes on death; tiles become Wasteland | Change `TileType` of 3×3 area to `WASTELAND` |
| Radar Station | **Scanner** | Passive | Reveals invisible units in radius | Overrides Stealth and Camouflage flags |
| Signal Jammer | **Static** | Passive | Blocks enemy vision in 3-tile radius | Forces `isVisible = false` for enemies in radius |
| Ruins | **Scavenge** | Active | Randomized effects upon unit entry | See Section 10.1 |

---

## 10.1 Ruins Mechanics

When a unit is on a Ruins tile, it can perform an explicit **Scavenge** action via the interaction menu. This triggers one of the following rewards:
- **Funding:** +15 Funding Points.
- **Experience:** +1000 XP (Global Pool).
- **Recon Drone:** Spawns a Recon Drone that auto-moves toward the Enemy Base. It becomes visible to the enemy at Turn 2 and can be attacked.
- **Sniper:** Spawns a friendly Sniper on the ruins tile (if empty).
- **Destroyer:** Spawns a friendly Destroyer at the nearest valid water tile (if the ruins are coastal/in water).

---

## 11. Capture Mechanics

- A unit on the same tile as an **unowned or enemy Base/Town** may open the Capture menu.
- Capturing transfers ownership, resets the structure's level, and spawns wild animals around it.
- Capturing a **Base** awards +250 XP to the capturing player.
- After capturing, the unit's `hasActed = true`.
- **Win Condition (TBD):** Eliminate all enemy bases.

---

## 12. Visibility and Information Warfare (Jamming Mask)

The game employs a dynamic **Jamming Algorithm** to manage the Fog of War. The visibility state of a tile is calculated as follows:

1.  **Reset:** Global visibility is cleared at the start of each turn calculation.
2.  **Jamming (Enemy EW):** A **Jamming Mask** is generated around enemy Electronic Warfare units (e.g., Signal Jammers) within a 4-tile radius.
3.  **Vision Calculation (Ally Units):** Vision radii are calculated for all allied units. A tile is revealed only if it is outside the jammer mask, or if it is within a 1-tile "suppressed vision" radius of an allied unit (overriding the mask for immediate proximity).
4.  **Suppression:** Units positioned inside a jammed zone have their own vision radius forcibly reduced to 1 tile.

- `FogSystem` recomputes visibility on every turn change.
- `FogToggle` button in HUD for debugging.

---

## 13. Save / Load

- Game state saved to JSON via libGDX's `Json` API under `saves/<saveName>.json`.
- Saved data: seed, player names, turn count, XP, funding, unit list (including `hasMoved` and `hasActed` flags), structure list, animal list.
- **Persistence:** All unit movements and actions are serialized. This ensures that if a player exits mid-turn, the "once per turn" action limits are still enforced upon reload.
- Loading reconstructs all entities from the save file.

---

## 14. HUD

- **Top bar:** Turn counter, Active player indicator, Funding display `[current] (+income)`, XP counter.
- **Unit info panel:** Shows selected unit's name, HP, stats, and action buttons.
- **Summon menu:** Slide-in panel listing available units with costs; disabled if insufficient funds.
- **Capture menu:** Slide-in panel for capture action on valid targets.
- **Build menu:** Slide-in panel for structure construction within base territory.
- **Level-up popup:** Animated popup showing new level, bonuses, and unlocks.
- **Fog toggle button.**

---

## 15. Non-Functional Requirements

- **Target platform:** Desktop (Windows primary).
- **Language:** Java 8+, libGDX framework, Ashley ECS.
- **Save location:** `saves/` in the local app directory.
- **Testing mode:** `GameConfig.TESTING_MODE = true` disables turn action limits.
