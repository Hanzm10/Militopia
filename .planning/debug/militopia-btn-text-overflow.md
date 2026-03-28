---
status: awaiting_human_verify
trigger: "militopia-btn TextButton renders text that overflows outside the button's visual NinePatch border"
created: 2026-03-29T00:00:00Z
updated: 2026-03-29T00:01:00Z
---

## Current Focus

hypothesis: CONFIRMED — Button sized to exact label width with zero inner margin; any pixel rounding causes text to visually bleed into bolt art.
test: Completed. Fix applied — minWidth increased to 300, leftWidth/rightWidth increased to 65.
expecting: Buttons now have ~18-37px breathing room between text and bolt decorations.
next_action: User verifies visually in-game.

## Symptoms

expected: Button text ("Saved Games", "Multiplayer", etc.) fully contained within the metallic bolt-bordered button background. NinePatch stretches horizontally to surround all text.
actual: All buttons appear the same narrow width (bolt decorations at fixed positions), and longer text like "Saved Games" overflows outside the button border on both sides. Shorter text like "Exit" fits fine. NinePatch is NOT stretching.
errors: No runtime errors — purely a visual layout issue.
reproduction: Launch the game, view the main menu. "Saved Games" and "Multiplayer" buttons show text extending past the button graphic.
started: After font changed to Russo One at 24px. Button width went through .width(200) → .minWidth(250) → no constraint, problem persists.

## Eliminated

- hypothesis: NinePatch stretch regions are defined incorrectly (wrong inset values)
  evidence: NinePatch(milBtnTex, 50, 50, 30, 30) — left/right=50px each is valid for bolt decorations. The NinePatch itself is correctly defined.
  timestamp: 2026-03-29T00:00:00Z

- hypothesis: setMinWidth(200) on NinePatchDrawable is not being called
  evidence: Code clearly calls milBtnUp.setMinWidth(200) at line 87. The value is set.
  timestamp: 2026-03-29T00:00:00Z

- hypothesis: Background drawable's leftWidth/rightWidth is not applied as table padding
  evidence: Decompiled Table.java shows Table constructor sets padLeft = backgroundLeft Value (which calls background.getLeftWidth()), padRight = backgroundRight Value (which calls background.getRightWidth()). Padding IS applied.
  timestamp: 2026-03-29T00:00:00Z

- hypothesis: uniformX without expandX prevents buttons from sizing to label width
  evidence: uniformX makes all cells the same width as the widest, which is max button prefWidth. fillX fills button to cell width. This works correctly — buttons do size to prefWidth.
  timestamp: 2026-03-29T00:00:00Z

- hypothesis: NinePatch.getPrefWidth() returns texture natural width (708px) causing oversized column
  evidence: NinePatchDrawable has no getPrefWidth() override — BaseDrawable returns minWidth (200 after override). Button.getPrefWidth() uses up.getMinWidth() and Table.getPrefWidth() (label + padding). Both confirmed via bytecode.
  timestamp: 2026-03-29T00:00:00Z

## Evidence

- timestamp: 2026-03-29T00:00:00Z
  checked: MenuScreen.java table cell layout (lines 75-81)
  found: Each button uses .fillX().uniformX().pad(10) — NO .expandX()
  implication: Without expandX, column does not expand beyond prefWidth. Column = max button prefWidth.

- timestamp: 2026-03-29T00:00:00Z
  checked: NinePatchDrawable.setPatch() bytecode
  found: setPatch() sets minWidth = NinePatch.getTotalWidth() = 708 (texture natural width). Then MilitopiaGame.java line 87 overrides to setMinWidth(200).
  implication: Final minWidth = 200. This acts as the floor for Button.getPrefWidth().

- timestamp: 2026-03-29T00:00:00Z
  checked: Table constructor bytecode
  found: padLeft = Table.backgroundLeft Value (reads background.getLeftWidth() = 50), padRight = Table.backgroundRight Value (reads background.getRightWidth() = 50). This IS applied as table padding.
  implication: TextButton's internal label cell has 50px padding on each side from background leftWidth/rightWidth.

- timestamp: 2026-03-29T00:00:00Z
  checked: Button.getPrefWidth() bytecode
  found: Returns max(Table.getPrefWidth(), up.getMinWidth(), down.getMinWidth(), ...). Table.getPrefWidth() = max(tablePrefWidth, background.getMinWidth()).
  implication: Button prefWidth = max(labelWidth + 50 + 50, 200).

- timestamp: 2026-03-29T00:00:00Z
  checked: Russo One TTF advance widths at 24px via fontTools
  found: "Saved Games"=163px, "Multiplayer"=140px, "New Game"=128px, "Exit"=47px
  implication: Widest label is 163px. Button prefWidth = max(163+100, 200) = 263px. Middle stretch = 263-100 = 163px. Label fills EXACTLY the middle stretch with zero margin.

- timestamp: 2026-03-29T00:00:00Z
  checked: NinePatch constructor (milBtnTex, 50, 50, 30, 30)
  found: NinePatch stretch regions: left=50px fixed, right=50px fixed, middle=variable. These are the visual bolt art regions.
  implication: When button renders at 263px: left bolt 50 + middle 163 + right bolt 50. Text is 163px and renders in a 163px middle region — zero margin. Any FreeType glyph pixel padding or rounding bleeds into bolt artwork.

## Resolution

root_cause: setMinWidth(200) caused buttons to size to max(labelWidth+100, 200). With "Saved Games" at ~163px advance width, button prefWidth = 263px. The NinePatch's middle stretch region = exactly 163px — the same as the label width. The label renders with zero margin against the bolt artwork. FreeType-generated BitmapFont glyphs have pixel padding beyond advance width metrics, causing the rendered text to visually bleed into the bolt decorations.
fix: Increased setMinWidth(200→300) to ensure all buttons are at least 300px wide (middle stretch = at least 200px for a 300px button using 50px NinePatch sides, giving 37px margin for "Saved Games"). Increased setLeftWidth/setRightWidth (50→65) to add 15px of additional inner padding margin, pushing label text inward from the bolt visual boundaries.
verification: (pending user confirmation)
files_changed: [core/src/main/java/com/militopia/MilitopiaGame.java]
