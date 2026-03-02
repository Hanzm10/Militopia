package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.militopia.MilitopiaGame;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.StatsComponent;
import com.militopia.controller.GameInputController;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.utils.HoverListener;

/**
 * Sliding tile/unit info panel anchored to the bottom of the screen.
 * Owns the icon, name label, HP label, Atk/Def/Rng/Mov/Vis stats grid,
 * the ability button row, and the close button.
 */
public class InfoPanel {

    private final MilitopiaGame game;
    private final AssetManager assets;
    private final Stage stage;
    private final HudBottomBar bottomBar;

    // Widget references
    private Table tileInfoTable;
    private com.badlogic.gdx.scenes.scene2d.ui.Image tileInfoImage;
    private Label tileInfoLabel;
    private Label hpLabel;
    private Table abilityTable;
    private Table statsTable;
    private Label atkLabel, defLabel, rngLabel, movLabel, visLabel;

    private static final float PANEL_HEIGHT = 120f;

    public InfoPanel(MilitopiaGame game, AssetManager assets, Stage stage, HudBottomBar bottomBar) {
        this.game = game;
        this.assets = assets;
        this.stage = stage;
        this.bottomBar = bottomBar;
        build();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void showTileInfo(String name, TextureRegion region) {
        if (abilityTable != null)
            abilityTable.clear();
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));
        tileInfoLabel.setText(name);
        if (hpLabel != null)
            hpLabel.setVisible(false);
        if (statsTable != null)
            statsTable.setVisible(false);
        slideIn();
    }

    /**
     * Shows the panel with unit name, HP bar, stats grid, and ability buttons.
     */
    public void showUnitInfo(final Entity unit, String name, TextureRegion region,
            int currentHP, int maxHP,
            final GameInputController controller,
            final UnitFactory factory,
            final GameScreen screen) {
        if (abilityTable != null)
            abilityTable.clear();
        tileInfoLabel.setText(name);
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));

        if (hpLabel != null) {
            hpLabel.setText("HP: " + currentHP + " / " + maxHP);
            hpLabel.setColor(currentHP > maxHP / 2 ? Color.GREEN : Color.YELLOW);
            hpLabel.setVisible(true);
        }

        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (stats != null && statsTable != null) {
            atkLabel.setText("Atk: " + stats.attack);
            defLabel.setText("Def: " + stats.defense);
            rngLabel.setText("Rng: " + stats.attackRange);
            movLabel.setText("Mov: " + stats.move);
            visLabel.setText("Vis: " + stats.vision);
            statsTable.setVisible(true);
        } else if (statsTable != null) {
            statsTable.setVisible(false);
        }

        // Ability buttons for the active player's own units
        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (stats != null && abilities != null
                && stats.owner == screen.getCurrentPlayer()
                && !stats.hasActed) {

            if (stats.unitTypeKey.equals("RECRUIT")
                    && !abilities.hasUsedDigIn && !abilities.isDiggingIn) {
                addAbilityButton("Dig In",
                        factory.getTextureForPopup("RECRUIT"),
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                controller.performAbility(unit, "DIG_IN");
                            }
                        });

            } else if (stats.unitTypeKey.equals("SUBMARINE")
                    && abilities.nukeCooldown == 0) {
                addAbilityButton("Launch Nuke",
                        factory.getHudIcon(MapGenerator.ObjectType.BASE_P1),
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                controller.performAbility(unit, "LAUNCH_NUKE");
                            }
                        });
            }
        }

        slideIn();
    }

    public void hideTileInfo() {
        tileInfoTable.clearActions();
        bottomBar.getBottomContainer().clearActions();
        tileInfoTable.addAction(
                Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));
        bottomBar.getBottomContainer().addAction(
                Actions.moveTo(bottomBar.getBottomContainer().getX(), 0, 0.3f, Interpolation.pow2In));
    }

    /** Snaps HP label immediately after combat resolves. */
    public void snapHP(int currentHP, int maxHP) {
        if (hpLabel != null && hpLabel.isVisible()) {
            hpLabel.setText("HP: " + currentHP + " / " + maxHP);
            hpLabel.setColor(currentHP > maxHP / 2 ? Color.GREEN : Color.YELLOW);
        }
    }

    public void resize(int width, int height) {
        if (tileInfoTable != null) {
            tileInfoTable.setWidth(width);
            tileInfoTable.setX(0);
        }
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    private void build() {
        tileInfoTable = new Table();
        tileInfoTable.setBackground(
                game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.9f)));

        tileInfoImage = new com.badlogic.gdx.scenes.scene2d.ui.Image();
        tileInfoImage.setScaling(Scaling.fit);
        tileInfoTable.add(tileInfoImage).size(70, 70).padLeft(20);

        // Name + HP stacked vertically
        Table infoStack = new Table();
        tileInfoLabel = new Label("Terrain Name", game.skin, "default-font", Color.WHITE);
        tileInfoLabel.setFontScale(0.8f);
        infoStack.add(tileInfoLabel).left().row();

        hpLabel = new Label("", game.skin, "default-font", Color.WHITE);
        hpLabel.setFontScale(0.65f);
        hpLabel.setVisible(false);
        infoStack.add(hpLabel).left().row();

        // Stats grid
        statsTable = new Table();
        atkLabel = makeStatLabel("Atk: 0");
        defLabel = makeStatLabel("Def: 0");
        rngLabel = makeStatLabel("Rng: 0");
        movLabel = makeStatLabel("Mov: 0");
        visLabel = makeStatLabel("Vis: 0");

        statsTable.add(atkLabel).width(50).left();
        statsTable.add(defLabel).width(50).left().row();
        statsTable.add(rngLabel).width(50).left();
        statsTable.add(movLabel).width(50).left().row();
        statsTable.add(visLabel).width(50).left();
        statsTable.setVisible(false);

        infoStack.add(statsTable).left().padTop(2);
        tileInfoTable.add(infoStack).padLeft(20);

        // Ability button row
        abilityTable = new Table();
        tileInfoTable.add(abilityTable).expandX().center();

        // Close button
        ImageButton.ImageButtonStyle closeStyle = new ImageButton.ImageButtonStyle();
        try {
            Texture closeTex = assets.get(AssetManager.BTN_SLIDEDOWN);
            TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(closeTex));
            closeStyle.imageUp = d;
            closeStyle.imageDown = d.tint(Color.GRAY);
        } catch (Exception e) {
            closeStyle.imageUp = game.skin.newDrawable("white", Color.RED);
        }
        ImageButton closeBtn = new ImageButton(closeStyle);
        closeBtn.addListener(new HoverListener());
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hideTileInfo();
            }
        });
        tileInfoTable.add(closeBtn).size(40, 40).padRight(20);

        tileInfoTable.setPosition(0, -PANEL_HEIGHT);
        tileInfoTable.setSize(stage.getWidth(), PANEL_HEIGHT);
        stage.addActor(tileInfoTable);
    }

    private void slideIn() {
        tileInfoTable.setWidth(stage.getWidth());
        tileInfoTable.setX(0);
        tileInfoTable.clearActions();
        bottomBar.getBottomContainer().clearActions();
        tileInfoTable.addAction(
                Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));
        bottomBar.getBottomContainer().addAction(
                Actions.moveBy(0, -bottomBar.getBottomContainer().getHeight(), 0.3f, Interpolation.pow2Out));
    }

    private void addAbilityButton(String text, TextureRegion icon, ClickListener listener) {
        SummonButton.addTo(abilityTable, icon, text, game, assets, listener);
    }

    private Label makeStatLabel(String text) {
        Label l = new Label(text, game.skin, "default-font", Color.WHITE);
        l.setFontScale(0.6f);
        return l;
    }

    /**
     * Package-visible: SlideMenu needs the tileInfoTable to animate it away on
     * open.
     */
    Table getTileInfoTable() {
        return tileInfoTable;
    }
}
