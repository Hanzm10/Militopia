package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.utils.HoverListener;

/**
 * Modal "Level Up!" popup.
 *
 * While visible, {@code popupTable} sits on top of all stage actors and
 * consumes every input event type so neither the map nor the HUD bottom bar
 * can receive any interaction.
 *
 * Root cause of the previous pass-through bug: {@code setFillParent(true)}
 * defers actual bounds to the next layout pass, leaving a zero-size actor on
 * the first frame. Fix: call {@code setBounds} explicitly in {@link #show}.
 */
public class LevelUpPopup {

    private final Stage stage;
    private final MilitopiaGame game;
    private final AssetManager assets;
    private final GameInputController inputController;
    private final HudBottomBar bottomBar;

    /** Full-screen dark modal — also acts as the input blocker. */
    private Table popupTable;

    /** Queued popups shown one at a time; each entry re-calls show logic. */
    private final java.util.Queue<Runnable> pendingPopups = new java.util.LinkedList<>();

    public LevelUpPopup(MilitopiaGame game, AssetManager assets, Stage stage,
            GameInputController inputController, HudBottomBar bottomBar) {
        this.game = game;
        this.assets = assets;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public boolean isVisible() {
        return popupTable != null && popupTable.hasParent();
    }

    /**
     * Builds and shows the popup. Explicit {@code setBounds} is called before
     * adding to stage so the input block is effective on the very first frame.
     */
    public void show(int owner, String baseName, int newLevel, int bonusFunds,
            String[] units, String[] structs, UnitFactory factory, boolean isLocal) {
        if (isVisible()) {
            // Another popup is on screen — queue this one for after dismiss.
            pendingPopups.add(() -> showImmediate(owner, baseName, newLevel, bonusFunds, units, structs, factory, isLocal));
            return;
        }
        showImmediate(owner, baseName, newLevel, bonusFunds, units, structs, factory, isLocal);
    }

    private void showImmediate(int owner, String baseName, int newLevel, int bonusFunds,
            String[] units, String[] structs, UnitFactory factory, boolean isLocal) {
        buildPopup(owner, baseName, newLevel, bonusFunds, units, structs, factory, isLocal);

        // Set explicit pixel bounds immediately — do NOT rely solely on
        // setFillParent which only resolves after layout.
        popupTable.setBounds(0, 0, stage.getWidth(), stage.getHeight());

        stage.addActor(popupTable);
        popupTable.toFront();

        // Freeze the map input controller AND the bottom bar buttons.
        // The InputMultiplexer bypasses Scene2D for raw InputAdapter events, so
        // Scene2D z-ordering alone cannot block map clicks/drag/scroll.
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private void buildPopup(int owner, String baseName, int newLevel, int bonusFunds,
            String[] units, String[] structs, UnitFactory factory, boolean isLocal) {
        popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));

        // Consume ALL input so nothing below (map, bottom bar) gets events.
        popupTable.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                return true;
            }

            @Override
            public void touchUp(InputEvent e, float x, float y, int p, int b) {
            }

            @Override
            public boolean mouseMoved(InputEvent e, float x, float y) {
                return true;
            }

            @Override
            public boolean keyDown(InputEvent e, int keycode) {
                return true;
            }

            @Override
            public boolean keyUp(InputEvent e, int keycode) {
                return true;
            }

            @Override
            public boolean keyTyped(InputEvent e, char c) {
                return true;
            }
        });

        // --- Modal content ---
        Table modal = new Table();

        Color titleColor = isLocal ? Color.YELLOW : Color.RED;
        Label title = new Label(baseName + " Leveled Up!", game.skin, "default-font", titleColor);
        title.setFontScale(1.2f);
        modal.add(title).pad(20).row();

        Label sub = new Label("Reached Level " + newLevel, game.skin, "default-font", Color.WHITE);
        sub.setFontScale(0.9f);
        modal.add(sub).padBottom(20).row();

        // Incentives row
        Table incentives = new Table();
        if (bonusFunds > 0) {
            TextureRegion fundingIcon = null;
            try {
                fundingIcon = new TextureRegion(assets.get(AssetManager.FUNDING_ICON2));
            } catch (Exception ignored) {
            }
            incentives.add(createBubble("+" + bonusFunds + " Funding", fundingIcon)).pad(10);
        }
        for (String u : units) {
            incentives.add(createBubble("Unlock:\n" + factory.toNiceName(u),
                    factory.getTextureForPopup(u))).pad(10);
        }
        for (String s : structs) {
            incentives.add(createBubble("Unlock:\n" + factory.toNiceName(s),
                    factory.getTextureForPopup(s))).pad(10);
        }
        modal.add(incentives).row();

        TextButton okBtn = new TextButton("Awesome!", game.skin, "militopia-btn");
        okBtn.addListener(new HoverListener());
        okBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                dismiss();
            }
        });
        modal.add(okBtn).fillX().width(200).padTop(30);

        popupTable.add(modal);
    }

    private void dismiss() {
        if (popupTable != null)
            popupTable.remove();
        popupTable = null;
        // If another level-up is queued, show it; otherwise restore interactivity.
        Runnable next = pendingPopups.poll();
        if (next != null) {
            next.run();
        } else {
            inputController.setInputEnabled(true);
            bottomBar.setBlocked(false);
        }
    }

    private Table createBubble(String text, TextureRegion icon) {
        com.badlogic.gdx.scenes.scene2d.ui.Stack stack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
        TextureRegionDrawable bg;
        try {
            bg = new TextureRegionDrawable(new TextureRegion(game.assets.get(AssetManager.CIRCLE_UI2)));
        } catch (Exception e) {
            bg = (TextureRegionDrawable) game.skin.newDrawable("white", Color.GRAY);
        }
        stack.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(bg));
        if (icon != null) {
            com.badlogic.gdx.scenes.scene2d.ui.Container<com.badlogic.gdx.scenes.scene2d.ui.Image> c = new com.badlogic.gdx.scenes.scene2d.ui.Container<>(
                    new com.badlogic.gdx.scenes.scene2d.ui.Image(icon));
            c.size(50, 50).center();
            stack.add(c);
        }
        Table t = new Table();
        t.add(stack).size(80, 80).row();
        Label l = new Label(text, game.skin, "default-font", Color.WHITE);
        l.setFontScale(0.6f);
        l.setWrap(true);
        l.setAlignment(Align.center);
        t.add(l).width(90).padTop(5);
        return t;
    }
}
