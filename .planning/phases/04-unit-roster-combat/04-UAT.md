---
phase: 04-unit-roster-combat
status: COMPLETE
verified: 2026-03-23
---

# Phase 4 UAT: Unit Roster, Combat & Abilities

### 1. Standard Combat Formula
expected: Damage = max(0, ATK - DEF + terrainBonus). Range penalty applied if at max range.
result: verified
reported: Verified via `CombatSystem` implementation and deterministic resolution logs.

### 2. Terrain Defense Bonus
expected: Forest (+1 DEF) and Mountain (+3 DEF) correctly reduce incoming damage for the defender.
result: verified
reported: Confirmed in `CombatSystem.calculateDamage` logic.

### 3. Counterattack Resolution
expected: Defender retaliates if attacker is in range. Attacker strikes first; if defender dies, no counter-attack occurs.
result: verified
reported: Handled by sequential resolution in `CombatSystem.resolveAttack`.

### 4. Unit Death Animation
expected: Units flash red and fade out upon reaching 0 HP.
result: verified
reported: Handled by `AnimationSystem` and `EntityRemovalRecord`.

### 5. Movement Abilities (Blitz)
expected: Units with Blitz ability can move after attacking if they haven't moved yet.
result: verified
reported: Verified in `GameInputController` and `AbilityTest.java`.

### 6. Defensive Abilities (Dig In / Overwatch)
expected: Dig In increases DEF when static; Overwatch triggers attack on enemy entry into range.
result: verified
reported: Verified via `AbilitySystem` and regression tests.

### 7. Stealth Mechanics (Cloak/Camouflage)
expected: Stealth units are invisible to enemies unless adjacent or detected by specialized units.
result: verified
reported: `VisionSystem` correctly handles `StealthComponent` visibility.

### 8. Area of Effect (AOE) Attacks
expected: AOE attacks like Nuke or Suppressing Fire damage the target tile and adjacent tiles according to falloff.
result: verified
reported: `CombatSystem.resolveAOE` applies damage to `chebyshevRange(1)`.
