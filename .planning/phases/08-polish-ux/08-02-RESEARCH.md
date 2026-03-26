# Phase 08: Polish & UX (Wave 2) - Research

## Standard Stack
- **Audio**: `com.badlogic.gdx.audio.Sound` for SFX, `com.badlogic.gdx.audio.Music` for BGM.
- **Animations**: `com.badlogic.gdx.math.Interpolation` for smooth movement curves.
- **State**: `com.militopia.data.TurnSnapshot` for full game state persistence.

## Architecture Patterns

### 1. Combat Animation System (ECS)
To decouple logic from visuals, we will use a dedicated `CombatAnimationSystem`.
- **Melee Lunge**: When an attack starts, add an `AnimationComponent` to the attacker. The system will modify a `visualOffset` field (to be added to `GridPositionComponent` or handled via a new `VisualOffsetComponent`).
- **Hit Sprites**: Spawn a temporary entity with a `TextureComponent` and `LifetimeComponent` at the defender's location upon impact.
- **Projectile Entities**: Ranged attacks spawn a `PROJECTILE` entity. A `ProjectileSystem` will move it using linear interpolation towards the target's world coordinates.

### 2. Audio Management (`AudioManager`)
A singleton-style utility or a managed class in `MilitopiaGame` to handle sound loading and playback.
- **Placeholder Assets**: Use `Gdx.audio.newSound(Gdx.files.internal("sounds/placeholder.wav"))`.
- **System Triggers**: `CombatSystem` and `MovementSystem` will call `AudioManager.play()` when events occur.

### 3. Action Undo (Snapshot-per-Action)
- **Trigger**: Move the `turnHistory.push()` call from turn-transitions to `GameInputController`.
- **Capture Point**: Immediately before `resolveAttack()`, `moveUnit()`, or `summonUnit()`.
- **Constraint Compliance**: Since `captureSnapshot` already performs a deep copy of the map and unit list, this pattern is robust for individual action reverts.

## Don't Hand-Roll
- **Tweening Math**: Use `Interpolation.pow2Out` for the lunge forward and `Interpolation.pow2In` for the return.
- **Audio Mixing**: Rely on libGDX's internal `Sound` mixer for simultaneous playbacks (with logic to prevent "volume blowing" by capping concurrent same-sound plays).

## Common Pitfalls
- **Animation Blocking**: If we wait for a 1.0s animation to finish before allowing the next input, the game will feel sluggish. Input should be locked only for the *duration* of the active unit's anim, but overall game state should remain responsive.
- **Sound Overlap**: Ensure we don't play 20 unit-move sounds at once if a group is moving. (Though currently, only 1 unit moves at a time).

## Code Examples

### Melee Lunge Logic
```java
float progress = timer / duration;
float lungeDist = Interpolation.sine.apply(progress) * MAX_LUNGE;
// apply to visual offset...
```

### Projectile Creation
```java
public void createProjectile(float startX, float startY, float endX, float endY) {
    Entity e = engine.createEntity();
    e.add(new ProjectileComponent(startX, startY, endX, endY, speed));
    e.add(new TextureComponent(bulletRegion));
    engine.addEntity(e);
}
```
