# Phase 4: Unit Roster & Combat — Context

**Gathered:** 2026-02-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement the core combat loop for the existing unit roster: attack targeting,
damage resolution, counterattack, unit death, and player feedback. Movement,
summoning, and the unit roster itself are already complete.

</domain>

<decisions>
## Implementation Decisions

### Damage Formula

- Formula: `damage = max(0, ATK - DEF + terrainBonus)`
- DEF can fully negate — no minimum damage floor (0 is valid)
- Terrain defense bonus applied to the **defender's tile**:
  - Forest = +1 DEF
  - Mountain = +3 DEF
  - All other terrain = 0
- Range penalty: -1 damage when attacker fires at **max range** (max only, not gradient)

### Attack Targeting UI

- Context-sensitive targeting: clicking an enemy tile within range triggers the attack — no explicit "Attack mode" button needed
- Move range = **blue overlay**, Attack range = **red overlay** — both shown simultaneously on unit selection
- Attacking without moving first **forfeits movement** for that turn (move is locked)
- **No confirmation dialog** — click enemy → attack resolves immediately

### Counterattack

- Defender retaliates **only if attacker is within the defender's attack range** (range-gated)
- Counterattack uses the **same full damage formula**: `max(0, ATK - DEF + terrainBonus)`
- Resolution order: **attacker strikes first** — if defender dies, no counter fires
- Terrain applies **symmetrically**: attacker's tile gives the attacker a DEF bonus during the counter

### Unit Death & Removal

- Death animation: **flash red 2–3 times → fade out over ~0.5s**
- Animation is **non-blocking** — player may continue acting while it plays
- Tile after removal: **clean** — no debris or wreck marker
- One-shot kills (attacker-first resolution) prevent counterattack entirely — mutual kills are impossible given this order

### Combat Feedback (HUD)

- **Floating damage number** pops above the target, floats up and fades over ~1s (red text)
- When DEF fully negates the hit: **"BLOCKED"** text floats instead of `0`
- Counterattack feedback: same floating number style, but positioned over the **attacker** — position distinguishes attack from counter
- HUD unit info panel HP: **instant snap** to new value when damage lands — no drain animation

### Claude's Discretion

- Input blocking during death animation: non-blocking (0.5s is too short to warrant locking input)

</decisions>

<specifics>
## Specific Ideas

- "BLOCKED" label is preferred over showing `0` — makes it clear DEF tanked the hit
- Mountain +3 DEF is intentionally steep — Mountains are already hard to reach, so holding high ground should feel powerful
- No confirm dialogs anywhere in the combat flow — hot-seat play should feel fast

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 4 combat scope.

</deferred>

---

*Phase: 04-unit-roster-combat*
*Context gathered: 2026-02-22*
