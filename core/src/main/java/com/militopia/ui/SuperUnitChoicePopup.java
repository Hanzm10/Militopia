package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.utils.HoverListener;

/**
 * Modal popup shown when a base reaches level 5+.
 * Forces the player to choose one of three super units.
 * No dismiss — the player must pick. The chosen unit spawns immediately
 * and is permanently available in that base's summon menu.
 */
public class SuperUnitChoicePopup {

    private static final String[] SUPER_UNITS = { "JUGGERNAUT", "B2", "SUBMARINE" };
    private static final String[] FLAVOR = {
            "Land Juggernaut\nJumps to any tile · AoE on landing · Range 4",
            "Stealth Bomber\nCloaked until attacks · Explosive damage · Range 3",
            "Deep Diver\nCloaked · Nuke on 3-turn cooldown · Range 4"
    };

    private final Stage stage;
    private final MilitopiaGame game;
    private final AssetManager assets;
    private final GameInputController inputController;
    private final HudBottomBar bottomBar;

    private Table popupTable;

    public SuperUnitChoicePopup(MilitopiaGame game, AssetManager assets, Stage stage,
            GameInputController inputController, HudBottomBar bottomBar) {
        this.game = game;
        this.assets = assets;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    public boolean isVisible() {
        return popupTable != null && popupTable.hasParent();
    }

    public void show(final Entity baseEntity, final GameState state,
            final UnitFactory factory, final MapGenerator.GameMap map) {
        buildPopup(baseEntity, state, factory, map);
        popupTable.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        stage.addActor(popupTable);
        popupTable.toFront();
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);
    }

    private void buildPopup(final Entity baseEntity, final GameState state,
            final UnitFactory factory, final MapGenerator.GameMap map) {
        popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.92f)));

        // Block all input — player must choose
        popupTable.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) { return true; }
            @Override
            public void touchUp(InputEvent e, float x, float y, int p, int b) {}
            @Override
            public boolean mouseMoved(InputEvent e, float x, float y) { return true; }
            @Override
            public boolean keyDown(InputEvent e, int keycode) { return true; }
            @Override
            public boolean keyUp(InputEvent e, int keycode) { return true; }
            @Override
            public boolean keyTyped(InputEvent e, char c) { return true; }
        });

        Table modal = new Table();

        Label title = new Label("Choose Your Super Unit", game.skin, "default-font", Color.YELLOW);
        title.setFontScale(1.2f);
        modal.add(title).pad(20).row();

        Label sub = new Label("Base reached Level 5 — pick one to unlock forever", game.skin, "default-font", Color.LIGHT_GRAY);
        sub.setFontScale(0.75f);
        modal.add(sub).padBottom(24).row();

        Table choicesRow = new Table();
        for (int i = 0; i < SUPER_UNITS.length; i++) {
            final String unitType = SUPER_UNITS[i];
            final String flavor = FLAVOR[i];
            choicesRow.add(buildChoiceCard(unitType, flavor, factory,
                    baseEntity, state, map)).pad(16);
        }
        modal.add(choicesRow).row();

        popupTable.add(modal);
    }

    private Table buildChoiceCard(final String unitType, String flavor,
            final UnitFactory factory,
            final Entity baseEntity, final GameState state,
            final MapGenerator.GameMap map) {

        Table card = new Table();
        card.setBackground(game.skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f)));

        // Unit icon
        TextureRegion icon = factory.getTextureForPopup(unitType);
        if (icon != null) {
            TextureRegionDrawable bg;
            try {
                bg = new TextureRegionDrawable(new TextureRegion(game.assets.get(AssetManager.CIRCLE_UI2)));
            } catch (Exception e) {
                bg = (TextureRegionDrawable) game.skin.newDrawable("white", Color.DARK_GRAY);
            }
            com.badlogic.gdx.scenes.scene2d.ui.Stack stack = new com.badlogic.gdx.scenes.scene2d.ui.Stack();
            stack.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(bg));
            com.badlogic.gdx.scenes.scene2d.ui.Container<com.badlogic.gdx.scenes.scene2d.ui.Image> c =
                    new com.badlogic.gdx.scenes.scene2d.ui.Container<>(
                            new com.badlogic.gdx.scenes.scene2d.ui.Image(icon));
            c.size(60, 60).center();
            stack.add(c);
            card.add(stack).size(90, 90).padTop(12).row();
        }

        // Name
        Label nameLabel = new Label(factory.toNiceName(unitType), game.skin, "default-font", Color.WHITE);
        nameLabel.setFontScale(0.85f);
        nameLabel.setAlignment(Align.center);
        card.add(nameLabel).padTop(6).row();

        // Flavor text
        Label flavorLabel = new Label(flavor, game.skin, "default-font", Color.LIGHT_GRAY);
        flavorLabel.setFontScale(0.55f);
        flavorLabel.setWrap(true);
        flavorLabel.setAlignment(Align.center);
        card.add(flavorLabel).width(130).padTop(4).padBottom(10).row();

        // Choose button
        com.badlogic.gdx.scenes.scene2d.ui.TextButton chooseBtn =
                new com.badlogic.gdx.scenes.scene2d.ui.TextButton("Choose", game.skin);
        chooseBtn.addListener(new HoverListener());
        chooseBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                factory.chooseSuperUnit(baseEntity, unitType, state, map);
                dismiss();
            }
        });
        card.add(chooseBtn).size(120, 44).padBottom(14);

        return card;
    }

    private void dismiss() {
        if (popupTable != null) popupTable.remove();
        inputController.setInputEnabled(true);
        bottomBar.setBlocked(false);
    }
}
