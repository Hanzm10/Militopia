package com.militopia.screen;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.militopia.MilitopiaGame;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
import com.militopia.systems.*;
import com.militopia.data.AnimalData;
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.data.TurnSnapshot;
import com.militopia.data.UnitSnapshot;
import com.militopia.data.StructureSnapshot;
import com.militopia.managers.SaveManager;
import com.militopia.managers.TurnHistoryManager;
import com.militopia.map.MapGenerator;
import com.militopia.systems.CombatSystem;
import com.militopia.systems.FogSystem;
import com.militopia.systems.MapRenderSystem;
import com.militopia.systems.MovementSystem;
import com.militopia.systems.AnimationSystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.systems.FloatingTextSystem;
import com.militopia.systems.AbilityStatusSystem;
import com.militopia.systems.StructureEconomySystem;
import com.militopia.systems.WinConditionSystem;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameScreen implements Screen {

    private final MilitopiaGame game;
    private final GameState gameState;

    private OrthographicCamera camera;
    private ExtendViewport viewport;
    private PooledEngine engine;
    private MapGenerator.GameMap gameMap;

    private UnitFactory unitFactory;
    private EntityFactory entityFactory;
    private SaveManager saveManager;

    private MapRenderSystem mapRenderSystem;
    private UnitRenderSystem unitRenderSystem;
    private GameInputController inputController;
    public GameHUD gameHUD;
    private AbilityStatusSystem abilityStatusSystem;
    private StructureEconomySystem structureEconomySystem;
    private WinConditionSystem winConditionSystem;
    private FogSystem fogSystem;
    private boolean isFogEnabled = true;
    private BitmapFont font;
    private TurnHistoryManager turnHistory = new TurnHistoryManager();

    private enum TurnState {
        PLAYING, FADING_OUT, FADING_IN
    }

    private TurnState turnState = TurnState.PLAYING;
    private float fadeTime = 0f;
    private final float FADE_DURATION = 0.3f;
    private ShapeRenderer shapeRenderer;

    public GameScreen(final MilitopiaGame game, GameState loadedState) {
        this.game = game;
        this.gameState = loadedState;
        this.shapeRenderer = new ShapeRenderer();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        viewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);

        engine = new PooledEngine();
        entityFactory = new EntityFactory(engine, game.assets);
        unitFactory = new UnitFactory(engine, game.assets);
        unitFactory.setEntityFactory(entityFactory);
        saveManager = new SaveManager();

        font = game.skin.getFont("default-font");
        font.getData().setScale(0.5f);

        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(loadedState.mapWidth, loadedState.mapHeight, loadedState.seed);
        if (loadedState.mapObjects != null) {
            gameMap.objects = loadedState.mapObjects;
        }

        centerCameraOnBase(gameState.currentPlayer);

        gameState.p1BaseCount = 0;
        gameState.p2BaseCount = 0;

        List<GridPoint2> initialBases = new ArrayList<>();

        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                MapGenerator.ObjectType type = gameMap.objects[x][y];
                if (type != MapGenerator.ObjectType.NONE) {
                    unitFactory.createObjectEntity(x, y, type, gameState);

                    if (type == MapGenerator.ObjectType.BASE_P1 || type == MapGenerator.ObjectType.BASE_P2) {
                        initialBases.add(new GridPoint2(x, y));
                    }
                }
            }
        }

        if (loadedState.animals != null && !loadedState.animals.isEmpty()) {
            Gdx.app.log("GameScreen", "Loading " + loadedState.animals.size() + " saved animals.");
            for (AnimalData a : loadedState.animals) {
                MapGenerator.ObjectType type = MapGenerator.ObjectType.valueOf(a.type);
                unitFactory.createObjectEntity(a.x, a.y, type, gameState);
            }
        } else {
            Gdx.app.log("GameScreen", "Generating new animals for initial bases.");
            for (GridPoint2 pos : initialBases) {
                unitFactory.spawnAnimalsAroundBase(pos.x, pos.y, gameMap, gameState);
            }
        }

        engine.addSystem(new MovementSystem());
        engine.addSystem(new AnimationSystem());

        CombatSystem combatSystem = new CombatSystem(gameMap, entityFactory, gameState);
        engine.addSystem(combatSystem);
        engine.addSystem(new EffectSystem());

        fogSystem = new FogSystem(gameMap, gameState.currentPlayer);
        engine.addSystem(fogSystem);

        abilityStatusSystem = new AbilityStatusSystem(gameMap);
        engine.addSystem(abilityStatusSystem);

        gameHUD = new GameHUD(game);

        structureEconomySystem = new StructureEconomySystem(loadedState, unitFactory, entityFactory, null);
        engine.addSystem(structureEconomySystem);

        winConditionSystem = new WinConditionSystem(loadedState, winnerID -> gameHUD.showGameOverPopup(winnerID));
        engine.addSystem(winConditionSystem);

        mapRenderSystem = new MapRenderSystem(game.batch, unitFactory, gameMap);
        engine.addSystem(mapRenderSystem);

        unitRenderSystem = new UnitRenderSystem(game.batch, gameMap, font);
        unitRenderSystem.setPlayer(gameState.currentPlayer);
        engine.addSystem(unitRenderSystem);

        engine.addSystem(new FloatingTextSystem());

        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                String key = (u.unitTypeKey != null) ? u.unitTypeKey : u.type;
                if (key == null)
                    key = "RECRUIT";

                unitFactory.createUnit(key, u.x, u.y, u.owner, u.hasActed);

                // Restore HP and moved flag
                Entity freshUnit = findUnitAt(u.x, u.y);
                if (freshUnit != null) {
                    StatsComponent s = freshUnit.getComponent(StatsComponent.class);
                    if (s != null) {
                        if (u.hp > 0)
                            s.currentHP = u.hp;
                        if (u.maxHp > 0)
                            s.maxHP = u.maxHp;
                        s.hasMoved = u.hasMoved;
                        s.hasActed = u.hasActed;
                    }
                }
            }
        }

        if (loadedState.structures != null) {
            for (com.militopia.data.StructureData s : loadedState.structures) {
                Entity e = findEntityAt(s.x, s.y);
                if (e != null) {
                    unitFactory.updateStructureFromSave(e, s, gameMap);
                }
            }
        }

        structureEconomySystem.setGameHUD(gameHUD);

        inputController = new GameInputController(
                this, camera, engine, gameMap, unitFactory, entityFactory, gameHUD, combatSystem);

        gameHUD.build(this, inputController, unitFactory, gameState, turnHistory);
        gameHUD.updateTurn(gameState.turnCount);
        gameHUD.updateXP(gameState.p1XP);

        int startIncome = calculateIncome(gameState.currentPlayer);
        gameHUD.updateFunding(gameState.p1Funding, startIncome);

        logBaseXPStatus(startIncome);

        // --- Prime fog visibility AFTER all entities are spawned ---
        // Without this, the first render frame sees all-false visibleTiles
        // from a new boolean[][], causing incorrect fog state.
        fogSystem.update(0);

        // --- Snapshot the initial state so undo can rewind to turn 1 ---
        turnHistory.push(unitFactory.captureSnapshot(engine, gameState, gameMap));

        // --- NEW: Handle Finished Games on Load ---
        if (gameState.isGameOver) {
            Gdx.app.log("GameScreen", "Loading a finished game. Showing Game Over popup.");
            gameHUD.showGameOverPopup(gameState.winnerID);
            winConditionSystem.setPlaying(false);
        }

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(gameHUD.stage);
        multiplexer.addProcessor(inputController);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private Entity findEntityAt(int x, int y) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y) {
                TypeComponent type = e.getComponent(TypeComponent.class);
                if (type != null && type.type == TypeComponent.Type.OBJECT) {
                    return e;
                }
            }
        }
        return null;
    }

    private Entity findUnitAt(int x, int y) {
        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && type.type == TypeComponent.Type.UNIT) {
                return e;
            }
        }
        return null;
    }

    public PooledEngine getEngine() {
        return engine;
    }

    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    public TurnHistoryManager getTurnHistory() {
        return turnHistory;
    }

    public GameState getGameState() {
        return gameState;
    }

    public MapGenerator.GameMap getGameMap() {
        return gameMap;
    }

    public int getCurrentPlayer() {
        return gameState.currentPlayer;
    }

    public void endTurnAction() {
        if (turnState == TurnState.PLAYING) {
            GameLogger.log(GameLogger.ECONOMY,
                    "P" + gameState.currentPlayer + " ends turn " + gameState.turnCount);
            turnState = TurnState.FADING_OUT;
            fadeTime = 0f;
            inputController.setInputEnabled(false);
            winConditionSystem.setPlaying(false);
        }
    }

    private void centerCameraOnBase(int playerID) {
        MapGenerator.ObjectType targetBase = (playerID == 1) ? MapGenerator.ObjectType.BASE_P1
                : MapGenerator.ObjectType.BASE_P2;
        for (int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {
                if (gameMap.objects[x][y] == targetBase) {
                    float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
                    float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);
                    camera.position.set(isoX, isoY, 0);
                    camera.zoom = 0.6f;
                    camera.update();
                    return;
                }
            }
        }
    }

    public void saveAndExit() {
        saveManager.saveGame(gameState, engine, gameMap);
        game.setScreen(new com.militopia.screen.MenuScreen(game));
    }

    public boolean toggleFog() {
        isFogEnabled = !isFogEnabled;
        mapRenderSystem.setFogEnabled(isFogEnabled);
        unitRenderSystem.setFogEnabled(isFogEnabled);
        return isFogEnabled;
    }

    public boolean isFogEnabled() {
        return isFogEnabled;
    }

    private void resetUnitActions() {
        ImmutableArray<Entity> units = engine.getEntitiesFor(Family.all(StatsComponent.class).get());
        for (Entity entity : units) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            stats.hasActed = false;
            stats.hasMoved = false;
        }
    }

    /**
     * Reverts the game to the start of the most recent turn.
     * Dead units are resurrected; HP, funding, XP, and map ownership are all
     * restored.
     * Does nothing if there is no history.
     */
    public void undoTurn() {
        TurnSnapshot snap = turnHistory.undo();
        if (snap == null)
            return;

        GameLogger.log(GameLogger.INPUT,
                "Undo triggered — reverting to turn " + snap.turn + " | player=" + snap.currentPlayer);

        // 1. Remove all UNIT entities from the engine
        List<Entity> toRemove = new ArrayList<>();
        ImmutableArray<Entity> all = engine.getEntitiesFor(
                Family.all(TypeComponent.class).get());
        for (Entity e : all) {
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (t.type == TypeComponent.Type.UNIT)
                toRemove.add(e);
        }
        for (Entity e : toRemove)
            engine.removeEntity(e);

        // 2. Restore GameState scalars
        gameState.p1Funding = snap.p1Funding;
        gameState.p2Funding = snap.p2Funding;
        gameState.p1XP = snap.p1XP;
        gameState.p2XP = snap.p2XP;
        gameState.turnCount = snap.turn;
        gameState.currentPlayer = snap.currentPlayer;
        gameState.p1BaseCount = snap.p1BaseCount;
        gameState.p2BaseCount = snap.p2BaseCount;

        // 3. Restore map objects array (captures/uncaptures)
        for (int x = 0; x < gameMap.width; x++) {
            System.arraycopy(snap.mapObjects[x], 0, gameMap.objects[x], 0, gameMap.height);
        }

        // 4. Restore structures in-place (update owner, level, XP, income, name)
        ImmutableArray<Entity> objects = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, TypeComponent.class, StatsComponent.class).get());
        for (StructureSnapshot ss : snap.structures) {
            for (Entity e : objects) {
                TypeComponent t = e.getComponent(TypeComponent.class);
                if (t.type != TypeComponent.Type.OBJECT)
                    continue;
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                if (pos.x == ss.x && pos.y == ss.y) {
                    com.militopia.data.StructureData sd = new com.militopia.data.StructureData();
                    sd.x = ss.x;
                    sd.y = ss.y;
                    sd.owner = ss.owner;
                    sd.currentBaseXP = ss.currentBaseXP;
                    sd.baseName = ss.name;
                    sd.baseOrdinal = ss.baseOrdinal;
                    unitFactory.updateStructureFromSave(e, sd, gameMap);
                    StatsComponent s = e.getComponent(StatsComponent.class);
                    s.level = ss.level;
                    s.income = ss.income;
                    break;
                }
            }
        }

        // 5. Recreate unit entities from snapshot
        for (UnitSnapshot us : snap.units) {
            unitFactory.createUnit(us.unitTypeKey, us.x, us.y, us.owner, us.hasActed);
            // Find the newly created entity and restore its HP + hasMoved
            ImmutableArray<Entity> freshUnits = engine.getEntitiesFor(
                    Family.all(GridPositionComponent.class, StatsComponent.class, TypeComponent.class).get());
            for (Entity e : freshUnits) {
                TypeComponent t = e.getComponent(TypeComponent.class);
                if (t.type != TypeComponent.Type.UNIT)
                    continue;
                GridPositionComponent p = e.getComponent(GridPositionComponent.class);
                StatsComponent s = e.getComponent(StatsComponent.class);
                if (p.x == us.x && p.y == us.y && s.owner == us.owner) {
                    s.currentHP = us.currentHP;
                    s.hasActed = us.hasActed;
                    s.hasMoved = us.hasMoved;
                    break;
                }
            }
        }

        // 6. Refresh fog, HUD, camera
        fogSystem.setPlayer(gameState.currentPlayer);
        fogSystem.update(0);
        unitRenderSystem.setPlayer(gameState.currentPlayer);
        int currentFunds = (gameState.currentPlayer == 1) ? gameState.p1Funding : gameState.p2Funding;
        int income = calculateIncome(gameState.currentPlayer);
        gameHUD.updateTurn(gameState.turnCount);
        gameHUD.updateFunding(currentFunds, income);
        gameHUD.updateXP((gameState.currentPlayer == 1) ? gameState.p1XP : gameState.p2XP);
        gameHUD.hideTileInfo();
        inputController.clearMarkersPublic();

        // 7. Refresh snapshot panel so the right-side overlay reflects updated history depth
        gameHUD.refreshSnapshotPanel();

        // Re-push so we can undo again if needed (the restored state is now the
        // "current" turn start)
        turnHistory.push(unitFactory.captureSnapshot(engine, gameState, gameMap));
    }

    public int calculateBaseXPGain(Entity base) {
        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (stats == null)
            return 0;

        int totalGain = 0;
        // Natural gain (Bases only)
        if (stats.name.contains("Base")) {
            totalGain = 250 + ((stats.level - 1) * 10);

            // Add XP from child structures
            GridPositionComponent bPos = base.getComponent(GridPositionComponent.class);
            if (bPos != null) {
                ImmutableArray<Entity> entities = engine
                        .getEntitiesFor(Family.all(StatsComponent.class, GridPositionComponent.class).get());
                for (Entity other : entities) {
                    StatsComponent oStats = other.getComponent(StatsComponent.class);
                    if (oStats.owner == stats.owner && oStats.parentBaseX == bPos.x && oStats.parentBaseY == bPos.y) {
                        totalGain += oStats.xpGain;
                    }
                }
            }
        } else {
            // Specialized structures show their own contribution
            totalGain = stats.xpGain;
        }
        return totalGain;
    }

    public int calculateBaseIncome(Entity entity) {
        StatsComponent stats = entity.getComponent(StatsComponent.class);
        if (stats == null)
            return 0;

        int individualIncome = stats.income;
        GridPositionComponent pos = entity.getComponent(GridPositionComponent.class);

        if (pos != null) {
            // SOLAR ARRAY: Tech Synergy (+1 for each adjacent friendly structure)
            if (stats.name.contains("Solar Array")) {
                ImmutableArray<Entity> entities = engine
                        .getEntitiesFor(Family.all(GridPositionComponent.class, StatsComponent.class).get());
                for (Entity other : entities) {
                    if (other == entity)
                        continue;
                    StatsComponent oStats = other.getComponent(StatsComponent.class);
                    GridPositionComponent oPos = other.getComponent(GridPositionComponent.class);
                    if (oStats.owner == stats.owner && (oStats.income > 0 || oStats.name.contains("Base"))) {
                        if (Math.max(Math.abs(pos.x - oPos.x), Math.abs(pos.y - oPos.y)) <= 1) {
                            individualIncome += 1;
                        }
                    }
                }
            }
        }
        return individualIncome;
    }

    /**
     * Calculates the grouped income for a base, including all linked structures.
     * Used by InfoPanel for display.
     */
    public int calculateGroupedBaseIncome(Entity base) {
        StatsComponent stats = base.getComponent(StatsComponent.class);
        if (stats == null || !stats.name.contains("Base"))
            return calculateBaseIncome(base);

        int totalGroupedIncome = calculateBaseIncome(base);
        GridPositionComponent pos = base.getComponent(GridPositionComponent.class);

        if (pos != null) {
            ImmutableArray<Entity> entities = engine
                    .getEntitiesFor(Family.all(StatsComponent.class, GridPositionComponent.class).get());
            for (Entity other : entities) {
                if (other == base)
                    continue;
                StatsComponent oStats = other.getComponent(StatsComponent.class);
                if (oStats.owner == stats.owner && oStats.parentBaseX == pos.x && oStats.parentBaseY == pos.y) {
                    totalGroupedIncome += calculateBaseIncome(other);
                }
            }
        }
        return totalGroupedIncome;
    }

    public int calculateIncome(int playerID) {
        int totalIncome = 0;
        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(StatsComponent.class).get());

        for (Entity entity : entities) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner == playerID) {
                totalIncome += calculateBaseIncome(entity);
            }
        }
        return totalIncome;
    }

    private int processTurnEconomy(int playerID) {
        int totalIncome = calculateIncome(playerID);
        // XP distribution, Hospital healing, and base leveling are handled
        // by StructureEconomySystem to keep this screen thin.
        structureEconomySystem.processTurn(playerID);
        return totalIncome;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.60f, 0.80f, 1.00f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (turnState == TurnState.FADING_OUT) {
            fadeTime += delta;
            if (fadeTime >= FADE_DURATION) {
                fadeTime = FADE_DURATION;
                gameState.currentPlayer = (gameState.currentPlayer == 1) ? 2 : 1;

                if (gameState.currentPlayer == 1) {
                    gameState.turnCount++;
                }

                int income = processTurnEconomy(gameState.currentPlayer);
                int currentTotal = (gameState.currentPlayer == 1) ? gameState.p1Funding : gameState.p2Funding;

                if (gameState.turnCount > 1) {
                    if (gameState.currentPlayer == 1) {
                        gameState.p1Funding += income;
                        currentTotal = gameState.p1Funding;
                    } else {
                        gameState.p2Funding += income;
                        currentTotal = gameState.p2Funding;
                    }
                }

                // Update GameLogger context for the new active player/turn
                GameLogger.setContext(gameState.turnCount, gameState.currentPlayer);
                int currentXP = (gameState.currentPlayer == 1) ? gameState.p1XP : gameState.p2XP;
                GameLogger.log(GameLogger.ECONOMY,
                        "Turn " + gameState.turnCount
                                + " | P" + gameState.currentPlayer + " starts"
                                + " | income=+" + income
                                + " | funds=" + currentTotal
                                + " | XP=" + currentXP);

                // --- Ability Turn Start Processing ---
                abilityStatusSystem.onTurnStart(gameState.currentPlayer);

                logBaseXPStatus(income);

                resetUnitActions();
                gameHUD.updateTurn(gameState.turnCount);

                gameHUD.updateXP(currentXP);

                gameHUD.updateFunding(currentTotal, income);
                fogSystem.setPlayer(gameState.currentPlayer);
                fogSystem.update(0);
                unitRenderSystem.setPlayer(gameState.currentPlayer);
                centerCameraOnBase(gameState.currentPlayer);
                turnState = TurnState.FADING_IN;
                fadeTime = FADE_DURATION;
            }
        } else if (turnState == TurnState.FADING_IN) {
            fadeTime -= delta;
            if (fadeTime <= 0) {
                fadeTime = 0;
                turnState = TurnState.PLAYING;
                // Only re-enable map input if the level-up popup isn't still on screen.
                // (processTurnEconomy may have shown a popup and already set
                // inputEnabled=false;
                // re-enabling here would silently override that lock.)
                if (!gameHUD.isLevelUpPopupVisible()) {
                    inputController.setInputEnabled(true);
                }
                winConditionSystem.setPlaying(true);
                // Snapshot the start of this new turn (before player acts)
                turnHistory.push(unitFactory.captureSnapshot(engine, gameState, gameMap));
            }
        }

        inputController.update(delta);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        mapRenderSystem.updateState(
                inputController.getHoveredX(), inputController.getHoveredY(),
                inputController.getBouncingX(), inputController.getBouncingY(),
                inputController.getBounceTimer());

        unitRenderSystem.updateState(
                inputController.getHoveredX(), inputController.getHoveredY(),
                inputController.getBouncingX(), inputController.getBouncingY(),
                inputController.getBounceTimer());

        engine.update(delta);

        gameHUD.render(delta);

        if (turnState != TurnState.PLAYING) {
            drawFadeOverlay();
        }
    }

    // --- UPDATED METHOD SIGNATURE & CONTENT ---
    private void logBaseXPStatus(int income) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("       TURN ").append(gameState.turnCount).append(" STATUS REPORT       \n");
        sb.append("========================================\n");

        // --- NEW: Consolidated Economy Header ---
        int currentFunds = (gameState.currentPlayer == 1) ? gameState.p1Funding : gameState.p2Funding;
        String playerLabel = (gameState.currentPlayer == 1) ? "PLAYER 1" : "PLAYER 2";

        sb.append("ACTIVE PLAYER : ").append(playerLabel).append("\n");

        // Handle Turn 1 specific text
        if (gameState.turnCount == 1) {
            sb.append("INCOME        : First Round (No Income)\n");
        } else {
            sb.append("INCOME        : +").append(income).append("\n");
        }

        sb.append("TOTAL FUNDS   : ").append(currentFunds).append("\n");
        sb.append("----------------------------------------\n");
        // ----------------------------------------

        List<String> p1Logs = new ArrayList<>();
        List<String> p2Logs = new ArrayList<>();

        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(StatsComponent.class, TypeComponent.class).get());

        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);

            if (type.type == TypeComponent.Type.OBJECT && (stats.owner == 1 || stats.owner == 2)) {

                // --- FIX: Only list Bases in the log ---
                if (stats.name.contains("Base")) {
                    int bInc = calculateGroupedBaseIncome(e);
                    String entry = String.format("  - %-25s (Lv %d) : %4.0f / %4.0f XP (+%d) | Inc: +%d",
                            stats.name, stats.level, stats.currentBaseXP, stats.maxBaseXP, this.calculateBaseXPGain(e),
                            bInc);
                    if (stats.owner == 1) {
                        p1Logs.add(entry);
                    } else {
                        p2Logs.add(entry);
                    }
                }
            }
        }

        Collections.sort(p1Logs);
        Collections.sort(p2Logs);

        sb.append("PLAYER 1 BASES:\n");
        if (p1Logs.isEmpty()) {
            sb.append("  (No Bases)\n");
        }
        for (String s : p1Logs) {
            sb.append(s).append("\n");
        }

        sb.append("\nPLAYER 2 BASES:\n");
        if (p2Logs.isEmpty()) {
            sb.append("  (No Bases)\n");
        }
        for (String s : p2Logs) {
            sb.append(s).append("\n");
        }

        sb.append("========================================\n");
        // Changing tag to "GameLog" for cleaner filtering if desired
        Gdx.app.log("GameLog", sb.toString());
    }

    private void drawFadeOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(gameHUD.stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float alpha = Math.min(1.0f, Math.max(0.0f, fadeTime / FADE_DURATION));
        shapeRenderer.setColor(0, 0, 0, alpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        gameHUD.resize(width, height);
    }

    @Override
    public void dispose() {
        engine.clearPools();
        gameHUD.dispose();
        shapeRenderer.dispose();
    }

    @Override
    public void show() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }
}
