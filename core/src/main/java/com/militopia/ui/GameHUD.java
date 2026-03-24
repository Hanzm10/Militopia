package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.systems.ScavengeSystem;
import com.militopia.systems.StructurePlacementSystem;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;

/**
 * Thin coordinator / facade for all HUD components.
 *
 * Public method signatures are identical to the previous monolith so that
 * {@link GameScreen}, {@link GameInputController}, and {@link UnitFactory}
 * require zero changes.
 *
 * Component ownership:
 * <ul>
 * <li>{@link HudTopBar} — XP / Funding / Turn strip</li>
 * <li>{@link HudBottomBar} — Settings / End-Turn / Undo + pause overlay</li>
 * <li>{@link InfoPanel} — Tile info, unit HP, stats, ability buttons</li>
 * <li>{@link SlideMenu} — Summon / Hunt / Capture / Build sliding panel</li>
 * <li>{@link LevelUpPopup} — Modal popup (blocks map input while visible)</li>
 * </ul>
 */
public class GameHUD {

    // -------------------------------------------------------------------------
    // Life-cycle
    // -------------------------------------------------------------------------

    public Stage stage;

    /** Kept for compatibility — points to the SlideMenu's internal Table. */
    public com.badlogic.gdx.scenes.scene2d.ui.Table summonMenu;

    private HudTopBar topBar;
    private HudBottomBar bottomBar;
    private InfoPanel infoPanel;
    private SlideMenu slideMenu;
    private LevelUpPopup levelUpPopup;
    private GameOverPopup gameOverPopup;

    private AssetManager assets;
    private MilitopiaGame game;

    public GameHUD(MilitopiaGame game) {
        this.game = game;
        this.assets = game.assets;
        stage = new Stage(new ScreenViewport());
    }

    /**
     * Builds all components and wires them to the Stage.
     * Must be called once after the game map and systems are ready.
     */
    public void build(final GameScreen screen,
            final GameInputController inputController,
            final UnitFactory unitFactory,
            final GameState state) {

        // 1. Create components
        ScavengeSystem scavengeSystem = new ScavengeSystem(screen.getEngine(), unitFactory, state, screen.getGameMap());
        StructurePlacementSystem placementSystem = new StructurePlacementSystem(screen.getEngine(), unitFactory, state,
                screen.getGameMap());

        topBar = new HudTopBar(game, assets);
        bottomBar = new HudBottomBar(game, assets, screen, stage, inputController);
        infoPanel = new InfoPanel(game, assets, stage, bottomBar);
        slideMenu = new SlideMenu(game, assets, stage, bottomBar, infoPanel,
                screen, inputController, unitFactory, scavengeSystem, placementSystem);
        levelUpPopup = new LevelUpPopup(game, assets, stage, inputController, bottomBar);
        gameOverPopup = new GameOverPopup(game, screen, stage, inputController, bottomBar);

        // Expose summonMenu for callers that still reference gameHUD.summonMenu
        summonMenu = slideMenu.menuTable;

        // 2. Assemble root table
        com.badlogic.gdx.scenes.scene2d.ui.Table rootTable = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        rootTable.setFillParent(true);
        rootTable.add(topBar.getActor()).growX().top().row();
        rootTable.add().expandY().row();
        rootTable.add(bottomBar.getActor()).growX().bottom();
        stage.addActor(rootTable);

        // 3. Prime the top bar with the initial state
        updateTurn(state.turnCount);
        updateXP(state.p1XP);
        int startIncome = screen.calculateIncome(1);
        updateFunding(state.p1Funding, startIncome);
    }

    // -------------------------------------------------------------------------
    // Stage life-cycle
    // -------------------------------------------------------------------------

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        infoPanel.resize(width, height);
        slideMenu.resize(width, height);
        if (summonMenu != null) {
            summonMenu.setSize(width, 140);
            summonMenu.setX(0);
        }
    }

    public void dispose() {
        stage.dispose();
    }

    // -------------------------------------------------------------------------
    // Top-bar delegation
    // -------------------------------------------------------------------------

    public void updateXP(int xp) {
        topBar.updateXP(xp);
    }

    public void updateTurn(int turn) {
        topBar.updateTurn(turn);
    }

    public void updateFunding(int funding, int income) {
        topBar.updateFunding(funding, income);
        slideMenu.setCurrentIncome(income);
    }

    // -------------------------------------------------------------------------
    // Info-panel delegation
    // -------------------------------------------------------------------------

    public void showTileInfo(String name, TextureRegion region) {
        infoPanel.showTileInfo(name, region);
    }

    public void showUnitInfo(final Entity unit, String name, TextureRegion region,
            int currentHP, int maxHP) {
        // We need controller, factory, and screen — retrieve from slideMenu's captured
        // refs
        // However, InfoPanel already has them via its own build params; we delegate
        // with
        // the full signature forwarded through GameHUD.build().
        // To keep backward compat, the internal showUnitInfo wiring is stored in
        // infoPanel.
        infoPanel.showUnitInfo(unit, name, region, currentHP, maxHP,
                slideMenu.getInputController(), slideMenu.getUnitFactory(), slideMenu.getGameScreen());
    }

    public void hideTileInfo() {
        infoPanel.hideTileInfo();
    }

    public void snapHP(int currentHP, int maxHP) {
        infoPanel.snapHP(currentHP, maxHP);
    }

    // -------------------------------------------------------------------------
    // Slide-menu delegation
    // -------------------------------------------------------------------------

    public void openSummonMenu(int owner, GameState state, int level, String producerType) {
        slideMenu.openSummonMenu(owner, state, level, producerType);
    }

    public void hideSummonMenu() {
        slideMenu.hide();
    }

    public void openHuntMenu(final Entity animalEntity, final Entity hunterUnit,
            final MapGenerator.ObjectType animalType,
            final UnitFactory factory,
            final GameInputController controller) {
        slideMenu.openHuntMenu(animalEntity, hunterUnit, animalType, factory, controller);
    }

    public void openCaptureMenu(final Entity townEntity, final Entity capturingUnit,
            final UnitFactory factory,
            final GameInputController controller,
            final MapGenerator.GameMap map,
            final GameState state) {
        slideMenu.openCaptureMenu(townEntity, capturingUnit, factory, controller, map, state);
    }

    public void openScavengeMenu(final Entity ruinsEntity, final Entity unit,
            final UnitFactory factory, final GameInputController controller) {
        slideMenu.openScavengeMenu(ruinsEntity, unit, factory, controller);
    }

    public void openBuildMenu(int x, int y, int owner, int maxLevel,
            boolean isWater, boolean isCoastalWater, boolean isCoastalLand,
            GameState state, int parentX, int parentY,
            MapGenerator.TerrainType terrain, com.militopia.factories.UnitFactory unitFactory) {
        slideMenu.openBuildMenu(x, y, owner, maxLevel,
                isWater, isCoastalWater, isCoastalLand, state, parentX, parentY,
                terrain, unitFactory);
    }

    // -------------------------------------------------------------------------
    // Level-up popup delegation
    // -------------------------------------------------------------------------

    public void showLevelUpPopup(int owner, String baseName, int newLevel, int bonusFunds,
            String[] units, String[] structs, UnitFactory factory) {
        levelUpPopup.show(owner, baseName, newLevel, bonusFunds, units, structs, factory);
    }

    /** Returns true if the level-up popup is currently on screen. */
    public boolean isLevelUpPopupVisible() {
        return levelUpPopup.isVisible();
    }

    // -------------------------------------------------------------------------
    // Game-over popup delegation
    // -------------------------------------------------------------------------

    public void showGameOverPopup(int winnerID) {
        gameOverPopup.show(winnerID);
    }
}
