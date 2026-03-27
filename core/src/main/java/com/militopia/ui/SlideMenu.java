package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.MilitopiaGame;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.config.BaseLevelConfig;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.systems.ScavengeSystem;
import com.militopia.systems.StructurePlacementSystem;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

/**
 * The sliding bottom menu panel — used for summon, hunt, capture, and build
 * menus.
 * Animates up from below the screen; the tile-info panel and bottom bar slide
 * away.
 */
public class SlideMenu {

    private final MilitopiaGame game;
    private final AssetManager assets;
    private final Stage stage;
    private final HudBottomBar bottomBar;
    private final InfoPanel infoPanel;
    private final GameScreen gameScreen;

    /** The actual menu Table added to stage. Also exposed for GameHUD compat. */
    public final Table menuTable;

    // State
    private int currentBaseOwner = 1;
    private int currentBaseLevel = 1;
    private int currentIncome = 0;
    private int buildX, buildY, buildParentX, buildParentY;

    // References kept alive for button callbacks
    private GameInputController inputController;
    private UnitFactory unitFactory;
    private GameState lastState;

    private static final float PANEL_HEIGHT = 140f;
    private static final Color BG_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.95f);

    private final ScavengeSystem scavengeSystem;
    private final StructurePlacementSystem placementSystem;

    public SlideMenu(MilitopiaGame game, AssetManager assets, Stage stage, HudBottomBar bottomBar, InfoPanel infoPanel,
            GameScreen gameScreen, GameInputController inputController, UnitFactory unitFactory,
            ScavengeSystem scavengeSystem, StructurePlacementSystem placementSystem) {
        this.game = game;
        this.assets = assets;
        this.stage = stage;
        this.bottomBar = bottomBar;
        this.infoPanel = infoPanel;
        this.gameScreen = gameScreen;
        this.inputController = inputController;
        this.unitFactory = unitFactory;
        this.scavengeSystem = scavengeSystem;
        this.placementSystem = placementSystem;

        menuTable = new Table();
        menuTable.setSize(stage.getWidth(), PANEL_HEIGHT);
        menuTable.setPosition(0, -PANEL_HEIGHT);
        stage.addActor(menuTable);
    }

    // -------------------------------------------------------------------------
    // Public open-menu API
    // -------------------------------------------------------------------------

    public void openSummonMenu(int owner, GameState state, int level, String producerType) {
        this.currentBaseOwner = owner;
        this.currentBaseLevel = level;
        this.lastState = state;
        populateSummonMenu(state, producerType);
        GameLogger.log(GameLogger.UI,
                "SlideMenu: Open Summon Menu | Owner: P" + owner + " | Lvl: " + level + " | Type: " + producerType);
        slideIn(true);
    }

    public void openHuntMenu(final Entity animalEntity, final Entity hunterUnit,
            final MapGenerator.ObjectType animalType,
            final UnitFactory factory,
            final GameInputController controller) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        UnitFactory.UiInfo info = factory.getObjectUi(animalType);
        GameLogger.log(GameLogger.UI, "SlideMenu: Open Hunt Menu | Target: " + info.name);
        TextureRegion iconRegion = factory.getHudIcon(animalType);
        SummonButton.addTo(content, iconRegion, "Hunt " + info.name, game, assets,
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        controller.performHunt(animalEntity, hunterUnit);
                    }
                });

        menuTable.add(content).expandX().center();
        slideIn(true);
    }

    public void openCaptureMenu(final Entity structureEntity, final Entity capturingUnit,
            final UnitFactory factory,
            final GameInputController controller,
            final MapGenerator.GameMap map,
            final GameState state) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        final int newOwner = capturingUnit.getComponent(StatsComponent.class).owner;
        TextureRegion baseRegion = (newOwner == 1)
                ? factory.getHudIcon(MapGenerator.ObjectType.BASE_P1)
                : factory.getHudIcon(MapGenerator.ObjectType.BASE_P2);
        // Determine label
        StatsComponent sStats = structureEntity.getComponent(StatsComponent.class);
        String label = "Capture Structure";
        if (sStats != null) {
            if (sStats.name.contains("Town"))
                label = "Capture Town";
            else
                label = "Capture Enemy Base";
        }

        GameLogger.log(GameLogger.UI, "SlideMenu: Open Capture Menu | Target: " + label);

        SummonButton.addTo(content, baseRegion, label, game, assets,
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        factory.captureStructure(structureEntity, newOwner, map, state);
                        // Logging
                        StatsComponent struct = structureEntity.getComponent(StatsComponent.class);
                        GridPositionComponent sPos = structureEntity.getComponent(GridPositionComponent.class);
                        String sName = (struct != null) ? struct.name : "Structure";
                        String spr = (sPos != null) ? GameLogger.pos(sPos.x, sPos.y) : "(?,?)";
                        GameLogger.log(GameLogger.CAPTURE, newOwner, "Captured " + sName + " at " + spr);

                        StatsComponent unitStats = capturingUnit.getComponent(StatsComponent.class);
                        if (unitStats != null)
                            unitStats.hasActed = true;

                        int newXP = (newOwner == 1) ? state.p1XP : state.p2XP;
                        int curFunds = (newOwner == 1) ? state.p1Funding : state.p2Funding;
                        int newIncome = gameScreen.calculateIncome(newOwner);
                        gameScreen.gameHUD.updateXP(newXP);
                        gameScreen.gameHUD.updateFunding(curFunds, newIncome);
                        hide();
                        controller.deselect();
                    }
                });

        menuTable.add(content).expandX().center();
        slideIn(true);
    }

    public void openScavengeMenu(final Entity ruinsEntity, final Entity unit,
            final UnitFactory factory,
            final GameInputController controller) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        TextureRegion ruinsRegion = factory.getHudIcon(MapGenerator.ObjectType.RUINS);
        GameLogger.log(GameLogger.UI, "SlideMenu: Open Scavenge Menu");

        SummonButton.addTo(content, ruinsRegion, "Scavenge Ruins", game, assets,
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        performScavenge(ruinsEntity, unit, factory, controller);
                    }
                });

        menuTable.add(content).expandX().center();
        slideIn(true);
    }

    private void performScavenge(Entity ruinsEntity, Entity unit, UnitFactory factory, GameInputController controller) {
        StatsComponent unitStats = unit.getComponent(StatsComponent.class);
        int owner = unitStats.owner;
        GameState state = gameScreen.getGameState();

        ScavengeSystem.ScavengeReward reward = scavengeSystem.performScavenge(ruinsEntity, unit);
        if (reward == null)
            return;

        // Update HUD (ScavengeSystem already updated the GameState numbers, but UI
        // needs snap)
        gameScreen.gameHUD.updateXP((owner == 1) ? state.p1XP : state.p2XP);
        gameScreen.gameHUD.updateFunding((owner == 1) ? state.p1Funding : state.p2Funding, currentIncome);

        hide();
        controller.deselect();
    }

    public void openBuildMenu(int x, int y, int owner, int maxLevel,
            boolean isWater, boolean isCoastalWater, boolean isCoastalLand,
            GameState state, int parentX, int parentY,
            MapGenerator.TerrainType terrain, UnitFactory unitFactory) {
        this.buildX = x;
        this.buildY = y;
        this.currentBaseOwner = owner;
        this.buildParentX = parentX;
        this.buildParentY = parentY;
        this.lastState = state;
        boolean hasItems = populateBuildMenu(state, maxLevel, isWater, isCoastalWater, isCoastalLand);
        if (hasItems) {
            GameLogger.log(GameLogger.UI, "SlideMenu: Open Build Menu | At: " + GameLogger.pos(x, y) + " | Owner: P"
                    + owner + " | Max Lvl: " + maxLevel);
            slideIn(true);
        } else {
            // No buildable structures for this tile — show terrain info in the Info Panel
            // instead.
            infoPanel.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
        }
    }

    /** Slides the menu back down (hides). */
    public void hide() {
        menuTable.clearActions();
        menuTable.addAction(Actions.moveTo(0, -menuTable.getHeight(), 0.3f, Interpolation.pow2In));
        bottomBar.getBottomContainer().clearActions();
        bottomBar.getBottomContainer()
                .addAction(Actions.moveTo(bottomBar.getBottomContainer().getX(), 0,
                        0.3f, Interpolation.pow2In));
        GameLogger.log(GameLogger.UI, "SlideMenu: Hide");
    }

    public void resize(int width, int height) {
        menuTable.setWidth(width);
        menuTable.setX(0);
    }

    // -------------------------------------------------------------------------
    // Private menu builders
    // -------------------------------------------------------------------------

    private void populateSummonMenu(GameState state, String producerType) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();
        populateSummonContent(content, state, producerType);
        menuTable.add(content).expandX().center();
    }

    private void populateSummonContent(Table content, GameState state, String producerType) {
        java.util.Set<String> unlocked = unlockedForLevel(currentBaseLevel, false);
        String[] allUnits = {
                "RECRUIT", "RANGER", "SNIPER", "TANK", "RECON_DRONE",
                "SUICIDE_DRONE", "APACHE", "GUNBOAT", "DESTROYER", "CARRIER"
        };

        for (final String unit : allUnits) {
            if (!unlocked.contains(unit))
                continue;
            StatsComponent.MoveType moveType = unitFactory.getUnitMoveType(unit);
            boolean show = producerType.equals("PORT")
                    ? moveType == StatsComponent.MoveType.SEA
                    : moveType == StatsComponent.MoveType.LAND || moveType == StatsComponent.MoveType.AIR;
            if (!show)
                continue;

            UnitFactory.UiInfo info = unitFactory.getUnitUi(unit);
            final int cost = unitFactory.getUnitCost(unit);

            SummonButton.addTo(content, info.region, info.name + " (" + cost + ")", game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            int funds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            if (funds < cost) {
                                GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                        "Attempted " + unit + " — insufficient funds ("
                                                + funds + "<" + cost + ")");
                                return;
                            }
                            int tx = inputController.getLastClickedX();
                            int ty = inputController.getLastClickedY();
                            if (tx == -1 || ty == -1)
                                return;

                            int[] spawn = unitFactory.findValidSpawnPoint(
                                    tx, ty, unitFactory.getUnitMoveType(unit), gameScreen.getGameMap());
                            if (spawn == null) {
                                GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                        "Attempted " + unit + " — no valid spawn point found");
                                return;
                            }
                            if (currentBaseOwner == 1)
                                state.p1Funding -= cost;
                            else
                                state.p2Funding -= cost;

                            unitFactory.createUnit(unit, spawn[0], spawn[1], currentBaseOwner, true);
                            int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                    "Summoned " + unit + " at " + GameLogger.pos(spawn[0], spawn[1])
                                            + " | cost=" + cost + " | funds remaining=" + remaining);

                            // Use fresh income calculation for HUD update
                            int newIncome = gameScreen.calculateIncome(currentBaseOwner);
                            gameScreen.gameHUD.updateFunding(remaining, newIncome);
                            hide();
                            inputController.resetLastClicked();
                        }
                    });
        }

        // Super unit — only shown if this base has a chosen super unit
        String chosenSuperUnit = null;
        int bx = inputController.getLastClickedX();
        int by = inputController.getLastClickedY();
        if (bx != -1 && by != -1) {
            Entity baseEnt = unitFactory.getEntityAt(bx, by, 1);
            if (baseEnt != null) {
                StatsComponent baseStats = baseEnt.getComponent(StatsComponent.class);
                if (baseStats != null) chosenSuperUnit = baseStats.chosenSuperUnit;
            }
        }
        if (chosenSuperUnit != null) {
            final String superUnit = chosenSuperUnit;
            StatsComponent.MoveType moveType = unitFactory.getUnitMoveType(superUnit);
            boolean show = producerType.equals("PORT")
                    ? moveType == StatsComponent.MoveType.SEA
                    : moveType == StatsComponent.MoveType.LAND || moveType == StatsComponent.MoveType.AIR;
            if (show) {
                UnitFactory.UiInfo info = unitFactory.getUnitUi(superUnit);
                final int cost = unitFactory.getUnitCost(superUnit);
                SummonButton.addTo(content, info.region, "★ " + info.name + " (" + cost + ")", game, assets,
                        new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                int funds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                                if (funds < cost) {
                                    GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                            "Attempted " + superUnit + " — insufficient funds ("
                                                    + funds + "<" + cost + ")");
                                    return;
                                }
                                int tx = inputController.getLastClickedX();
                                int ty = inputController.getLastClickedY();
                                if (tx == -1 || ty == -1) return;

                                int[] spawn = unitFactory.findValidSpawnPoint(
                                        tx, ty, unitFactory.getUnitMoveType(superUnit), gameScreen.getGameMap());
                                if (spawn == null) {
                                    GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                            "Attempted " + superUnit + " — no valid spawn point found");
                                    return;
                                }
                                if (currentBaseOwner == 1) state.p1Funding -= cost;
                                else state.p2Funding -= cost;

                                unitFactory.createUnit(superUnit, spawn[0], spawn[1], currentBaseOwner, true);
                                int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                                GameLogger.log(GameLogger.SUMMON, currentBaseOwner,
                                        "Summoned " + superUnit + " at " + GameLogger.pos(spawn[0], spawn[1])
                                                + " | cost=" + cost + " | funds remaining=" + remaining);

                                int newIncome = gameScreen.calculateIncome(currentBaseOwner);
                                gameScreen.gameHUD.updateFunding(remaining, newIncome);
                                hide();
                                inputController.resetLastClicked();
                            }
                        });
            }
        }

        // Cancel button
        com.badlogic.gdx.scenes.scene2d.ui.TextButton closeBtn = new com.badlogic.gdx.scenes.scene2d.ui.TextButton(
                "Cancel", game.skin);
        closeBtn.addListener(new HoverListener());
        closeBtn.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                inputController.resetLastClicked();
            }

        });
        content.add(closeBtn).pad(10);
    }

    /**
     * Populates the build-menu content table. Returns {@code true} if at least
     * one build button was added, or {@code false} if nothing is buildable on
     * this tile (so the caller can fall back to showTileInfo).
     */
    private boolean populateBuildMenu(GameState state, int maxLevel,
            boolean isWater, boolean isCoastalWater, boolean isCoastalLand) {
        menuTable.clear();
        menuTable.setBackground(game.skin.newDrawable("white", BG_COLOR));
        Table content = new Table();

        java.util.Set<String> unlocked = unlockedForLevel(maxLevel, true);
        int addedCount = 0;

        boolean isOilTile = gameScreen.getGameMap().objects[buildX][buildY] == MapGenerator.ObjectType.OIL;

        for (final String struct : unlocked) {
            boolean show;
            if (isOilTile) {
                // On Oil tiles, ONLY show Oil Derrick
                show = struct.equals("OIL_DERRICK");
            } else {
                // On regular tiles, show everything BUT Oil Derrick (and respect water/land)
                if (struct.equals("PORT"))
                    show = isWater && isCoastalWater;
                else if (struct.equals("NUCLEAR"))
                    show = !isWater && isCoastalLand;
                else if (struct.equals("OIL_DERRICK"))
                    show = false; // Hide on non-oil tiles
                else
                    show = !isWater;
            }
            if (!show)
                continue;

            addedCount++;
            TextureRegion icon = unitFactory.getTextureForPopup(struct);
            final int cost = unitFactory.getStructureCost(struct);
            String niceName = unitFactory.toNiceName(struct) + " (" + cost + ")";

            SummonButton.addToWrapped(content, icon, niceName, game, assets,
                    new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (!placementSystem.canBuild(struct, buildX, buildY, currentBaseOwner, cost, isWater,
                                    isCoastalWater, isCoastalLand)) {
                                return;
                            }

                            placementSystem.performBuild(struct, buildX, buildY, currentBaseOwner, cost, buildParentX,
                                    buildParentY);

                            int remaining = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                            int newIncome = gameScreen.calculateIncome(currentBaseOwner);
                            gameScreen.gameHUD.updateFunding(remaining, newIncome);
                            hide();
                            inputController.resetLastClicked();
                        }
                    });
        }
        if (addedCount > 0) {
            menuTable.add(content).expandX().center();
            menuTable.row();
        }
        return addedCount > 0;
    }

    // -------------------------------------------------------------------------
    // Animation helpers
    // -------------------------------------------------------------------------

    private void slideIn(boolean hideInfoPanel) {
        menuTable.setSize(stage.getWidth(), PANEL_HEIGHT);
        menuTable.setX(0);
        stage.addActor(menuTable);

        if (hideInfoPanel) {
            infoPanel.getTileInfoTable().clearActions();
            infoPanel.getTileInfoTable().addAction(
                    Actions.moveTo(0, -infoPanel.getTileInfoTable().getHeight(),
                            0.3f, Interpolation.pow2In));
        }
        menuTable.clearActions();
        menuTable.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));

        bottomBar.getBottomContainer().clearActions();
        bottomBar.getBottomContainer().addAction(
                Actions.moveTo(bottomBar.getBottomContainer().getX(),
                        -bottomBar.getBottomContainer().getHeight(),
                        0.3f, Interpolation.pow2Out));
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /** Collect unlocked unit OR structure keys for levels 1..maxLevel. */
    private java.util.Set<String> unlockedForLevel(int maxLevel, boolean structs) {
        return BaseLevelConfig.getUnlockedForLevel(maxLevel, structs);
    }

    /**
     * Package-visible accessors used by {@link GameHUD} to forward to InfoPanel.
     */
    GameInputController getInputController() {
        return inputController;
    }

    UnitFactory getUnitFactory() {
        return unitFactory;
    }

    GameScreen getGameScreen() {
        return gameScreen;
    }

    /**
     * Called by GameHUD.updateFunding() so menu buttons know the current income.
     */
    public void setCurrentIncome(int income) {
        this.currentIncome = income;
    }
}
