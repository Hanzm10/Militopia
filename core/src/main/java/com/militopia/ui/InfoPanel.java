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
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.militopia.MilitopiaGame;
import com.militopia.components.AbilitiesComponent;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.BaseLevelConfig;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

import java.util.Set;

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
    private ScrollPane abilityScroll;
    private Table statsTable;
    private Table infoStack;
    private Label atkLabel, defLabel, rngLabel, movLabel, visLabel;

    // Base specific labels
    private Label levelLabel, xpLabel, incomeLabel, rewardsLabel;

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
        showTileInfo(name, region, true);
    }

    public void showTileInfo(String name, TextureRegion region, boolean animate) {
        if (abilityTable != null)
            abilityTable.clear();
        atkLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE); // Reset color
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));
        tileInfoLabel.setText(name);
        if (hpLabel != null) {
            hpLabel.setVisible(false);
            infoStack.getCell(hpLabel).height(0);
        }
        if (statsTable != null)
            statsTable.setVisible(false);

        GameLogger.log(GameLogger.UI, "InfoPanel: Show Tile Info | " + name);
        if (animate)
            slideIn();
        else
            snapToIn();
    }

    /**
     * Shows base-specific info including level, XP, income, and next rewards.
     */
    public void showBaseInfo(final Entity base, String name, TextureRegion region,
            final GameInputController controller,
            final UnitFactory factory,
            final GameScreen screen, boolean animate) {
        if (abilityTable != null)
            abilityTable.clear();
        tileInfoLabel.setText(name);
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));

        if (hpLabel != null) {
            hpLabel.setVisible(false);
            infoStack.getCell(hpLabel).height(0);
        }

        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (stats != null && statsTable != null) {
            boolean isBase = stats.name.contains("Base");

            // Level line: only for bases
            if (isBase) {
                atkLabel.setColor(com.badlogic.gdx.graphics.Color.GREEN);
                atkLabel.setText("Level: " + stats.level);
            } else {
                atkLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                atkLabel.setText("Status: Strategic");
            }

            // XP line: show bar for bases, simple text for structures
            if (isBase) {
                defLabel.setText("XP: " + (int) stats.currentBaseXP + " / " + (int) stats.maxBaseXP + " (+"
                        + screen.calculateBaseXPGain(base) + ")");
            } else {
                defLabel.setText("XP Gain: +" + screen.calculateBaseXPGain(base));
            }

            rngLabel.setText("Income: +" + screen.calculateGroupedBaseIncome(base));
            movLabel.setText(""); // Label 4 (unused for objs)

            // Next rewards lookup: only for bases
            if (isBase) {
                BaseLevelConfig.LevelData next = BaseLevelConfig.getLevel(stats.level + 1);
                java.util.List<String> items = new java.util.ArrayList<>();
                if (next.unlockedUnits != null) {
                    for (String u : next.unlockedUnits)
                        items.add(u);
                }
                if (next.unlockedStructures != null) {
                    for (String s : next.unlockedStructures)
                        items.add(s);
                }
                if (next.borderRadius > stats.vision) {
                    items.add("+AREA");
                }

                if (items.isEmpty()) {
                    visLabel.setText("Next: None");
                } else {
                    visLabel.setText("Next: " + String.join(", ", items));
                }
            } else {
                visLabel.setText(""); // Hide rewards for non-bases
            }
            statsTable.setVisible(true);
        }

        GameLogger.log(GameLogger.UI, "InfoPanel: Show Structure Info | " + name
                + (stats != null ? " | Lvl: " + stats.level : ""));
        if (animate)
            slideIn();
        else
            snapToIn();
    }

    public void showBaseInfoUnified(final Entity base,
            final GameInputController controller,
            final UnitFactory factory,
            final GameScreen screen) {
        showBaseInfo(base, base.getComponent(StatsComponent.class).name,
                factory.getHudIcon(screen.getGameMap().objects[base.getComponent(GridPositionComponent.class).x][base
                        .getComponent(GridPositionComponent.class).y]),
                controller, factory, screen, true);

        // Populate summons in the abilityTable (Center)
        final StatsComponent bs = base.getComponent(StatsComponent.class);
        final GameState state = screen.getGameState();
        Set<String> unlocked = BaseLevelConfig.getUnlockedForLevel(bs.level, false);

        String[] allUnits = {
                "RECRUIT", "RANGER", "SNIPER", "TANK", "RECON_DRONE",
                "SUICIDE_DRONE", "APACHE", "GUNBOAT", "DESTROYER", "CARRIER"
        };

        for (final String unit : allUnits) {
            if (!unlocked.contains(unit))
                continue;
            StatsComponent.MoveType moveType = factory.getUnitMoveType(unit);
            // In a BASE, we only show non-SEA units. PORTS (later) will show SEA units.
            if (moveType == StatsComponent.MoveType.SEA)
                continue;

            UnitFactory.UiInfo info = factory.getUnitUi(unit);
            final int cost = factory.getUnitCost(unit);

            SummonButton.addTo(abilityTable, info.region, info.name + " (" + cost + ")", game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            int funds = (bs.owner == 1) ? state.p1Funding : state.p2Funding;
                            if (funds < cost) {
                                GameLogger.log(GameLogger.SUMMON, bs.owner,
                                        "Attempted " + unit + " — insufficient funds ("
                                                + funds + "<" + cost + ")");
                                return;
                            }
                            int tx = controller.getLastClickedX();
                            int ty = controller.getLastClickedY();
                            if (tx == -1 || ty == -1)
                                return;

                            int[] spawn = factory.findValidSpawnPoint(
                                    tx, ty, moveType, screen.getGameMap());
                            if (spawn == null) {
                                GameLogger.log(GameLogger.SUMMON, bs.owner,
                                        "Attempted " + unit + " — no valid spawn point found");
                                return;
                            }
                            if (bs.owner == 1)
                                state.p1Funding -= cost;
                            else
                                state.p2Funding -= cost;

                            factory.createUnit(unit, spawn[0], spawn[1], bs.owner, true);
                            int remaining = (bs.owner == 1) ? state.p1Funding : state.p2Funding;
                            GameLogger.log(GameLogger.SUMMON, bs.owner,
                                    "Summoned " + unit + " at " + GameLogger.pos(spawn[0], spawn[1])
                                            + " | cost=" + cost + " | funds remaining=" + remaining);
                            screen.gameHUD.updateFunding(remaining, bs.income);
                            hideTileInfo();
                            controller.resetLastClicked();
                        }
                    });
        }
    }

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
            infoStack.getCell(hpLabel).height(20);
        }

        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (stats != null && statsTable != null) {
            // Reset label colors for units
            atkLabel.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            atkLabel.setText("Atk: " + stats.attack);
            defLabel.setText("Def: " + stats.defense);
            rngLabel.setText("Rng: " + stats.attackRange);
            movLabel.setText("Mov: " + stats.move);
            visLabel.setText("Vis: " + stats.vision);
            statsTable.setVisible(true);
        } else if (statsTable != null) {
            statsTable.setVisible(false);
        }

        GameLogger.log(GameLogger.UI, "InfoPanel: Show Unit Info | " + name + " | HP: " + currentHP + "/" + maxHP);
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
        GameLogger.log(GameLogger.UI, "InfoPanel: Hide");
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
        infoStack = new Table();
        tileInfoLabel = new Label("Terrain Name", game.skin, "default-font", Color.WHITE);
        tileInfoLabel.setFontScale(0.8f);
        infoStack.add(tileInfoLabel).left().row();

        hpLabel = new Label("", game.skin, "default-font", Color.WHITE);
        hpLabel.setFontScale(0.65f);
        hpLabel.setVisible(false);
        infoStack.add(hpLabel).left().height(0).row();

        // Stats grid
        statsTable = new Table();
        atkLabel = makeStatLabel("Atk: 0");
        defLabel = makeStatLabel("Def: 0");
        rngLabel = makeStatLabel("Rng: 0");
        movLabel = makeStatLabel("Mov: 0");
        visLabel = makeStatLabel("Vis: 0");

        statsTable.add(atkLabel).width(120).left();
        statsTable.add(defLabel).width(120).left().row();
        statsTable.add(rngLabel).width(120).left();
        statsTable.add(movLabel).width(120).left().row();
        statsTable.add(visLabel).width(120).left();
        statsTable.setVisible(false);

        infoStack.add(statsTable).left().padTop(0);
        tileInfoTable.add(infoStack).left().padLeft(20).padRight(60);

        // Ability button row (wrapped in ScrollPane for D-02)
        abilityTable = new Table();
        abilityScroll = new ScrollPane(abilityTable, game.skin);
        abilityScroll.getStyle().background = null;
        abilityScroll.setScrollingDisabled(false, true); // Horizontal scroll only
        tileInfoTable.add(abilityScroll).expandX().fillX().padLeft(20).padRight(10);

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

    private void snapToIn() {
        tileInfoTable.setWidth(stage.getWidth());
        tileInfoTable.setX(0);
        tileInfoTable.setY(0);
        tileInfoTable.clearActions();
        bottomBar.getBottomContainer().clearActions();
        bottomBar.getBottomContainer().setY(-bottomBar.getBottomContainer().getHeight());
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
