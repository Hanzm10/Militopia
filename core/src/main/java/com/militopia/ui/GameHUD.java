package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.managers.AssetManager;
import com.militopia.managers.TurnHistoryManager;
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
    private SuperUnitChoicePopup superUnitChoicePopup;
    private GameOverPopup gameOverPopup;
    private GameStatsPopup gameStatsPopup;
    private EconomyPopup economyPopup;
    private DisconnectPopup disconnectPopup;

    private GameScreen screen;
    private GameInputController inputController;
    private UnitFactory unitFactory;
    private GameState gameState;

    private AssetManager assets;
    private MilitopiaGame game;

    // Right panel: turn history overlay (shown only when enabled from settings)
    private Table snapshotPanel;
    private Table snapshotList;
    private Table snapshotRedoList;
    private TurnHistoryManager turnHistory;
    private boolean snapshotPanelVisible = false;

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
        build(screen, inputController, unitFactory, state, null);
    }

    /**
     * Builds all components and wires them to the Stage.
     * Pass a {@link TurnHistoryManager} to enable the right-panel snapshot overlay.
     */
    public void build(final GameScreen screen,
            final GameInputController inputController,
            final UnitFactory unitFactory,
            final GameState state,
            final TurnHistoryManager history) {

        this.screen = screen;
        this.inputController = inputController;
        this.unitFactory = unitFactory;
        this.turnHistory = history;
        this.gameState = state;

        // 1. Create components
        ScavengeSystem scavengeSystem = new ScavengeSystem(screen.getEngine(), unitFactory, screen.getEntityFactory(),
                state, screen.getGameMap());
        StructurePlacementSystem placementSystem = new StructurePlacementSystem(screen.getEngine(), unitFactory, state,
                screen.getGameMap());

        topBar = new HudTopBar(game, assets);
        bottomBar = new HudBottomBar(game, assets, screen, stage, inputController, this);
        infoPanel = new InfoPanel(game, assets, stage, bottomBar);
        slideMenu = new SlideMenu(game, assets, stage, bottomBar, infoPanel,
                screen, inputController, unitFactory, scavengeSystem, placementSystem);
        levelUpPopup = new LevelUpPopup(game, assets, stage, inputController, bottomBar);
        superUnitChoicePopup = new SuperUnitChoicePopup(game, assets, stage, inputController, bottomBar);
        gameOverPopup = new GameOverPopup(game, screen, stage, inputController, bottomBar);
        gameStatsPopup = new GameStatsPopup(game, stage, inputController, bottomBar);
        economyPopup = new EconomyPopup(game, stage, inputController, bottomBar);
        disconnectPopup = new DisconnectPopup(game, screen, stage, inputController, bottomBar);

        // Expose summonMenu for callers that still reference gameHUD.summonMenu
        summonMenu = slideMenu.menuTable;

        // 2. Assemble root table
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.add(topBar.getActor()).growX().top().row();
        rootTable.add().expandY().row();
        rootTable.add(bottomBar.getActor()).growX().bottom();
        stage.addActor(rootTable);

        // 3. Build right-panel snapshot overlay (mounted as a stage overlay, not in rootTable)
        buildSnapshotPanel(screen);

        // 4. Prime the HUD with the initial state
        updateTurn(state.turnCount, state.currentPlayer, screen.getActiveLocalPlayer());
        updateXP(state.p1XP);
        int startIncome = screen.calculateIncome(1);
        updateFunding(state.p1Funding, startIncome);
    }

    /**
     * Builds the right-side turn history panel (full screen height).
     * Hidden by default — shown only when toggled on from the settings overlay.
     */
    private void buildSnapshotPanel(final GameScreen screen) {
        snapshotPanel = new Table();
        snapshotPanel.setFillParent(true);

        Table card = new Table();
        card.setBackground(game.skin.newDrawable("white", new Color(0f, 0f, 0f, 0.75f)));

        Label title = new Label("Turn History", game.skin);
        title.setFontScale(0.6f);
        title.setColor(Color.WHITE);
        title.setAlignment(Align.center);

        // Redo section
        Label redoLabel = new Label("↪ Redo", game.skin);
        redoLabel.setFontScale(0.5f);
        redoLabel.setColor(Color.CYAN);

        snapshotRedoList = new Table();
        snapshotRedoList.top().left();
        ScrollPane redoScrollPane = new ScrollPane(snapshotRedoList, game.skin);
        redoScrollPane.setScrollingDisabled(true, false);
        redoScrollPane.setFadeScrollBars(true);

        // Undo section
        Label undoLabel = new Label("↩ Undo", game.skin);
        undoLabel.setFontScale(0.5f);
        undoLabel.setColor(Color.LIGHT_GRAY);

        snapshotList = new Table();
        snapshotList.top().left();
        ScrollPane undoScrollPane = new ScrollPane(snapshotList, game.skin);
        undoScrollPane.setScrollingDisabled(true, false);
        undoScrollPane.setFadeScrollBars(true);

        // Buttons
        final TextButton undoBtn = new TextButton("↩ Undo", game.skin);
        undoBtn.getLabel().setFontScale(0.6f);
        undoBtn.getLabel().setColor(Color.YELLOW);
        undoBtn.pad(6, 8, 6, 8);
        undoBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                screen.undoTurn();
                refreshSnapshotPanel();
            }
        });

        final TextButton redoBtn = new TextButton("↪ Redo", game.skin);
        redoBtn.getLabel().setFontScale(0.6f);
        redoBtn.getLabel().setColor(Color.CYAN);
        redoBtn.pad(6, 8, 6, 8);
        redoBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                screen.redoTurn();
                refreshSnapshotPanel();
            }
        });

        Table btnRow = new Table();
        btnRow.add(undoBtn).growX().pad(4);
        btnRow.add(redoBtn).growX().pad(4);

        card.add(title).growX().padTop(8).padBottom(4).row();
        card.add(redoLabel).left().padLeft(6).padTop(4).row();
        card.add(redoScrollPane).growX().growY().pad(4).row();
        card.add(undoLabel).left().padLeft(6).padTop(4).row();
        card.add(undoScrollPane).growX().growY().pad(4).row();
        card.add(btnRow).growX().padBottom(8);

        snapshotPanel.right();
        snapshotPanel.add(card).right().growY().width(130);

        snapshotPanel.setVisible(false);
        stage.addActor(snapshotPanel);
        refreshSnapshotPanel();
    }

    /**
     * Toggles the snapshot panel on/off. Called from the settings overlay.
     * Returns the new visible state.
     */
    public boolean toggleSnapshotPanel() {
        snapshotPanelVisible = !snapshotPanelVisible;
        if (snapshotPanel != null) {
            snapshotPanel.setVisible(snapshotPanelVisible);
        }
        return snapshotPanelVisible;
    }

    /**
     * Refreshes both undo and redo lists. Call after any undo/redo or turn transition.
     */
    public void refreshSnapshotPanel() {
        if (snapshotList == null || snapshotRedoList == null) return;

        // --- Undo list ---
        snapshotList.clear();
        java.util.List<com.militopia.data.TurnSnapshot> undoSnaps =
                (turnHistory != null) ? turnHistory.getSnapshots() : java.util.Collections.emptyList();

        if (undoSnaps.isEmpty()) {
            Label empty = new Label("No history", game.skin);
            empty.setFontScale(0.45f);
            empty.setColor(Color.DARK_GRAY);
            snapshotList.add(empty).left().padLeft(6);
        } else {
            for (int i = 0; i < undoSnaps.size(); i++) {
                com.militopia.data.TurnSnapshot snap = undoSnaps.get(i);
                final int index = i;
                TextButton btn = new TextButton("P" + snap.currentPlayer + " T" + snap.turn, game.skin);
                btn.getLabel().setFontScale(0.55f);
                btn.getLabel().setColor(Color.LIGHT_GRAY);
                btn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        screen.undoToSnapshot(index);
                    }
                });
                snapshotList.add(btn).growX().padLeft(4).padRight(4).padBottom(2).row();
            }
        }

        // --- Redo list ---
        snapshotRedoList.clear();
        java.util.List<com.militopia.data.TurnSnapshot> redoSnaps =
                (turnHistory != null) ? turnHistory.getRedoSnapshots() : java.util.Collections.emptyList();

        if (redoSnaps.isEmpty()) {
            Label empty = new Label("Nothing to redo", game.skin);
            empty.setFontScale(0.45f);
            empty.setColor(Color.DARK_GRAY);
            snapshotRedoList.add(empty).left().padLeft(6);
        } else {
            for (int i = 0; i < redoSnaps.size(); i++) {
                com.militopia.data.TurnSnapshot snap = redoSnaps.get(i);
                final int index = i;
                TextButton btn = new TextButton("P" + snap.currentPlayer + " T" + snap.turn, game.skin);
                btn.getLabel().setFontScale(0.55f);
                btn.getLabel().setColor(Color.CYAN);
                btn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        screen.redoToSnapshot(index);
                    }
                });
                snapshotRedoList.add(btn).growX().padLeft(4).padRight(4).padBottom(2).row();
            }
        }
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

    public void updateTurn(int turn, int currentPlayer, int localPlayerID) {
        topBar.updateTurn(turn);
        bottomBar.updateTurnState(currentPlayer, localPlayerID);
    }

    public void updateFunding(int funding, int income) {
        topBar.updateFunding(funding, income);
        slideMenu.setCurrentIncome(income);
    }

    // -------------------------------------------------------------------------
    // Info-panel delegation
    // -------------------------------------------------------------------------

    public void showTileInfo(String name, TextureRegion region) {
        showTileInfo(name, region, true);
    }

    public void showTileInfo(String name, TextureRegion region, boolean animate) {
        infoPanel.showTileInfo(name, region, animate);
    }

    public void showBaseInfo(final Entity base, String name, TextureRegion region) {
        showBaseInfo(base, name, region, true);
    }

    public void showBaseInfo(final Entity base, String name, TextureRegion region, boolean animate) {
        infoPanel.showBaseInfo(base, name, region,
                slideMenu.getInputController(), slideMenu.getUnitFactory(), slideMenu.getGameScreen(), animate);
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

    public void showBaseInfoUnified(Entity base, GameState state, int level, String producerType) {
        infoPanel.showBaseInfoUnified(base, inputController, unitFactory, screen);
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

    /**
     * Shows the super unit choice popup when a base reaches level 5+.
     * The player must pick one of three super units — no dismiss.
     */
    public void showSuperUnitChoicePopup(Entity baseEntity, GameState state, UnitFactory factory) {
        superUnitChoicePopup.show(baseEntity, state, factory, screen.getGameMap());
    }

    // -------------------------------------------------------------------------
    // Game-over popup delegation
    // -------------------------------------------------------------------------

    public void showGameOverPopup(int winnerID) {
        gameOverPopup.show(winnerID, gameState);
    }

    // -------------------------------------------------------------------------
    // Stats & Economy popups
    // -------------------------------------------------------------------------

    public void showGameStats(GameState state) {
        gameStatsPopup.show(state);
    }

    public void showEconomyPopup(int turnCount, int income, int xpGain, int currentFunds,
            java.util.LinkedHashMap<String, Integer> incomeBreakdown,
            java.util.LinkedHashMap<String, Integer> xpBreakdown) {
        economyPopup.show(turnCount, income, xpGain, currentFunds, incomeBreakdown, xpBreakdown);
    }

    public void showDisconnectPopup(String message) {
        disconnectPopup.show(message);
    }
}
