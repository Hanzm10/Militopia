# Phase 08: Polish & UX - Research

## Standard Stack
- **UI Framework**: libGDX `scene2d.ui` (Stage, Table, Image, Label).
- **Animations**: libGDX `Actions` (for UI element sliding/fading) and `TextureRegion` / custom `AnimationSystem` for entity sprites in Ashley ECS.
- **Audio**: libGDX `Sound` (for short SFX like attack/move) and `Music` (for BGM).
- **Undo State Management**: Custom Snapshot pattern using `TurnHistoryManager`, `GameState`, `TurnSnapshot` (already existing in `com.militopia.managers`).

## Architecture Patterns
- **Base Info Panel**: Must be a `Table` added to the `scene2d` Stage. It should mirror the sliding approach found in `SlideMenu` (`Actions.moveTo(...)` with `Interpolation.pow2In` / `pow2Out`).
- **Entity Animations**: Use standard Ashley ECS. Add a `RenderStateComponent` or `AnimationComponent` holding state time and the current animation type (IDLE, ATTACK, MOVE). Process this in `RenderSystem`.
- **Audio Delivery**: A centralized `AudioManager` or `SoundSystem` is ideal to cache and play `Sound` instances via `AssetManager`, preventing rapid overlap or memory leaks.
- **Floating Text**: Create temporary UI elements or ECS entities (`FloatingTextComponent`) that update their Y position and alpha over time in a system, then `engine.removeEntity()` when expired.
- **Action Undo**: `TurnHistoryManager` holds a stack of `TurnSnapshot` objects. We must take a snapshot *before* mutating state rather than just at the start of a turn, or ensure that each discrete action pushes a snapshot. Right now, to support undo before committing moves, discrete action snapping is needed.
- **Visual Feedback**: Screen shake can be implemented in the `CameraSystem` by adding a positional offset that decays over time.

## Don't Hand-Roll
- **UI Layouts**: Do not calculate raw X/Y coordinates for text. Use `Table` from `scene2d.ui` for all Base Info Panel layouts and HUD additions.
- **Tweening/Easing**: Do not hand-roll math for sliding the Info Panel or fading floating text. Use `Actions.moveTo`, `Actions.fadeOut`, `Actions.parallel`, etc.
- **Snapshot Math**: Do not hand-roll deep copy structures for GameState; use the existing `TurnSnapshot`, `UnitSnapshot`, and `StructureSnapshot` structures.

## Common Pitfalls
- **Memory Leaks**: Failing to properly `dispose()` `Sound` or `Music` objects if loaded dynamically, or failing to cache them via `AssetManager`.
- **Input Unswallowing**: The new Info Panel or GameOverPopup might let clicks fall through to the map if `addListener(new ClickListener()...)` doesn't consume the event.
- **Animation Sync**: Tying game logic tightly to attack animation speed. The game logic should resolve instantly (e.g., deducting HP), and the visual representation should merely play out decoupling the simulation from rendering.
- **Snapshot Bloat**: Taking too many snapshots or holding large object references in snapshots. `TurnHistoryManager` has a `MAX_HISTORY = 10`, which prevents this.
- **Overlapping SFX**: If an AoE attack hits 5 units, playing the hit sound 5 times simultaneously blows out the audio mixer. We need a capping mechanism for SFX.

## Code Examples

### Sliding UI Pattern (from SlideMenu)
```java
public void show() {
    panelTable.clearActions();
    panelTable.addAction(Actions.moveTo(targetX, targetY, 0.3f, Interpolation.pow2Out));
}
public void hide() {
    panelTable.clearActions();
    panelTable.addAction(Actions.moveTo(offscreenX, offscreenY, 0.3f, Interpolation.pow2In));
}
```

### Playing Audio
```java
// Pre-load in AssetManager
assets.load("sounds/attack.ogg", Sound.class);
// Later
Sound attackSound = assets.get("sounds/attack.ogg", Sound.class);
attackSound.play(0.5f); // 50% volume
```

### Floating Text via scene2d Actions
```java
Label floatText = new Label("+100 Funds", skin);
floatText.setPosition(baseX, baseY);
floatText.addAction(Actions.sequence(
    Actions.parallel(
        Actions.moveBy(0, 50, 1.5f, Interpolation.fade),
        Actions.fadeOut(1.5f)
    ),
    Actions.removeActor()
));
stage.addActor(floatText);
```
