# Implementation Plan 06-02: UI Logic Verification

## Goal Description
Verify that the UI components related to the Game Over state and the HUD are correctly initialized, wired to the game state, and handle user interactions (like returning to the menu) properly. Since this is a headless environment, we will use Mockito to simulate the libGDX runtime.

## Proposed Changes

### [Component: Testing Infrastructure]

#### [NEW] [UITest.java](file:///c:/Users/Hanz%20Mapua/Workspace/Militopia/core/src/test/java/com/militopia/ui/UITest.java)
- Set up `Gdx` static mocks (`graphics`, `gl20`, `files`, `input`).
- Provide a mocked `Skin` and `BitmapFont` to avoid loading real assets.
- Implement tests for:
    - **GameOverScreen**: Check winner label text and "Return to Main Menu" button logic.
    - **HudTopBar**: Verify that calling `updateXP` and `updateFunding` updates the display labels.

## Verification Plan

### Automated Tests
- Run the new test suite via Gradle:
```powershell
.\gradlew.bat :core:test --tests "com.militopia.ui.UITest"
```

### Manual Verification
- **Visual Check**: Since I cannot see the screen, I will verify the internal state of the `Stage` (actors list, label contents) to confirm what *would* be rendered.
- **Interaction Check**: Simulate `ClickListener` events and verify that the `MilitopiaGame.setScreen` method receives the correct screen transitions.

### Pre-requisites
- Mockito (Already added in previous step).
- `Gdx` statics must be reset after each test to avoid polluting other tests.
