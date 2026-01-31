package com.militopia.screen;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
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
import com.militopia.data.AnimalData; // Ensure Import
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.SaveManager;
import com.militopia.map.MapGenerator;
import com.militopia.systems.FogSystem;
import com.militopia.systems.MapRenderSystem;
import com.militopia.systems.MovementSystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.ui.GameHUD;
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
    private FogSystem fogSystem;
    private boolean isFogEnabled = true;
    private BitmapFont font;

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
        saveManager = new SaveManager();

        font = game.skin.getFont("default-font");
        font.getData().setScale(0.5f);

        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, loadedState.seed);

        centerCameraOnBase(gameState.currentPlayer);

        gameState.p1BaseCount = 0;
        gameState.p2BaseCount = 0;

        List<GridPoint2> initialBases = new ArrayList<>();

        // 1. Generate Map Objects
        for (int x = 0; x < GameConfig.MAP_WIDTH; x++) {
            for (int y = 0; y < GameConfig.MAP_HEIGHT; y++) {
                MapGenerator.ObjectType type = gameMap.objects[x][y];
                if (type != MapGenerator.ObjectType.NONE) {
                    unitFactory.createObjectEntity(x, y, type, gameState);

                    if (type == MapGenerator.ObjectType.BASE_P1 || type == MapGenerator.ObjectType.BASE_P2) {
                        initialBases.add(new GridPoint2(x, y));
                    }
                }
            }
        }

        // --- LOAD ANIMALS / NEW GAME ANIMALS ---
        if (loadedState.animals != null && !loadedState.animals.isEmpty()) {
            Gdx.app.log("GameScreen", "Loading " + loadedState.animals.size() + " saved animals.");
            for (AnimalData a : loadedState.animals) {
                MapGenerator.ObjectType type = MapGenerator.ObjectType.valueOf(a.type);

                // --- FIX: DO NOT OVERWRITE THE MAP DATA ---
                // We simply create the entity. The map underneath (Tree/Oil) remains as is.
                // gameMap.objects[a.x][a.y] = type;  <-- REMOVED THIS LINE
                unitFactory.createObjectEntity(a.x, a.y, type, gameState);
            }
        } else {
            // New Game: Spawn fresh animals
            Gdx.app.log("GameScreen", "Generating new animals for initial bases.");
            for (GridPoint2 pos : initialBases) {
                unitFactory.spawnAnimalsAroundBase(pos.x, pos.y, gameMap, gameState);
            }
        }
        // ---------------------------------------

        engine.addSystem(new MovementSystem());

        fogSystem = new FogSystem(gameMap, gameState.currentPlayer);
        engine.addSystem(fogSystem);

        mapRenderSystem = new MapRenderSystem(game.batch, unitFactory, gameMap);
        engine.addSystem(mapRenderSystem);

        unitRenderSystem = new UnitRenderSystem(game.batch, gameMap, font);
        engine.addSystem(unitRenderSystem);

        // 2. Load Saved Units
        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                if ("RECRUIT".equals(u.type)) {
                    unitFactory.createUnit("RECRUIT", u.x, u.y, u.owner, false);
                }
            }
        }

        // 3. Load Saved Structures
        if (loadedState.structures != null) {
            for (com.militopia.data.StructureData s : loadedState.structures) {
                Entity e = findEntityAt(s.x, s.y);
                if (e != null) {
                    unitFactory.updateStructureFromSave(e, s, gameMap);
                }
            }
        }

        gameHUD = new GameHUD(game);

        inputController = new GameInputController(
                this, camera, engine, gameMap, unitFactory, entityFactory, gameHUD
        );

        gameHUD.build(this, inputController, unitFactory, gameState);
        gameHUD.updateTurn(gameState.turnCount);
        gameHUD.updateXP(gameState.p1XP);

        int startIncome = calculateIncome(gameState.currentPlayer);
        gameHUD.updateFunding(gameState.p1Funding, startIncome);

        logBaseXPStatus();

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

    public GameState getGameState() {
        return gameState;
    }

    public int getCurrentPlayer() {
        return gameState.currentPlayer;
    }

    public void endTurnAction() {
        if (turnState == TurnState.PLAYING) {
            turnState = TurnState.FADING_OUT;
            fadeTime = 0f;
            inputController.setInputEnabled(false);
        }
    }

    private void centerCameraOnBase(int playerID) {
        MapGenerator.ObjectType targetBase = (playerID == 1) ? MapGenerator.ObjectType.BASE_P1 : MapGenerator.ObjectType.BASE_P2;
        for (int x = 0; x < GameConfig.MAP_WIDTH; x++) {
            for (int y = 0; y < GameConfig.MAP_HEIGHT; y++) {
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
        //
        saveManager.saveGame(gameState, engine, gameMap);
        game.setScreen(new com.militopia.screen.MenuScreen(game));
    }

    public boolean toggleFog() {
        isFogEnabled = !isFogEnabled;
        mapRenderSystem.setFogEnabled(isFogEnabled);
        unitRenderSystem.setFogEnabled(isFogEnabled);
        return isFogEnabled;
    }

    private void resetUnitActions() {
        ImmutableArray<Entity> units = engine.getEntitiesFor(Family.all(StatsComponent.class).get());
        for (Entity entity : units) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            stats.hasActed = false;
        }
    }

    public int calculateIncome(int playerID) {
        int totalIncome = 0;
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(StatsComponent.class).get());
        for (Entity entity : entities) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            if (stats.owner == playerID) {
                totalIncome += stats.income;
            }
        }
        return totalIncome;
    }

    private int processTurnEconomy(int playerID) {
        int totalIncome = 0;
        int totalXPGain = 0;

        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(StatsComponent.class).get());

        for (Entity entity : entities) {
            StatsComponent stats = entity.getComponent(StatsComponent.class);
            TypeComponent type = entity.getComponent(TypeComponent.class);

            if (stats.owner == playerID) {
                totalIncome += stats.income;

                if (gameState.turnCount > 1) {
                    if (type.type == TypeComponent.Type.OBJECT && stats.income > 0) {
                        int gain = 250;
                        if (stats.currentBaseXP < stats.maxBaseXP) {
                            stats.currentBaseXP += gain;
                            if (stats.currentBaseXP > stats.maxBaseXP) {
                                stats.currentBaseXP = stats.maxBaseXP;
                            }
                        }
                        totalXPGain += gain;
                    }
                }
            }
        }

        if (gameState.turnCount > 1) {
            if (playerID == 1) {
                gameState.p1XP += totalXPGain;
            } else {
                gameState.p2XP += totalXPGain;
            }
        }

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
                    logBaseXPStatus();
                } else {
                    Gdx.app.log("Economy", "Turn: " + gameState.turnCount + " | Player: " + gameState.currentPlayer + " | First Round (No Income) | Total Funds: " + currentTotal);
                }

                resetUnitActions();
                gameHUD.updateTurn(gameState.turnCount);

                int currentXP = (gameState.currentPlayer == 1) ? gameState.p1XP : gameState.p2XP;
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
                inputController.setInputEnabled(true);
            }
        }

        inputController.update(delta);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        mapRenderSystem.updateState(
                inputController.getHoveredX(), inputController.getHoveredY(),
                inputController.getBouncingX(), inputController.getBouncingY(),
                inputController.getBounceTimer()
        );

        unitRenderSystem.updateState(
                inputController.getHoveredX(), inputController.getHoveredY(),
                inputController.getBouncingX(), inputController.getBouncingY(),
                inputController.getBounceTimer()
        );

        engine.update(delta);
        gameHUD.render(delta);

        if (turnState != TurnState.PLAYING) {
            drawFadeOverlay();
        }
    }

    private void logBaseXPStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("       TURN ").append(gameState.turnCount).append(" STATUS REPORT       \n");
        sb.append("========================================\n");

        List<String> p1Logs = new ArrayList<>();
        List<String> p2Logs = new ArrayList<>();

        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(StatsComponent.class, TypeComponent.class).get());

        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);

            if (type.type == TypeComponent.Type.OBJECT && (stats.owner == 1 || stats.owner == 2)) {
                String entry = String.format("  - %-25s : %4.0f / %4.0f XP", stats.name, stats.currentBaseXP, stats.maxBaseXP);
                if (stats.owner == 1) {
                    p1Logs.add(entry);
                } else {
                    p2Logs.add(entry);
                }
            }
        }

        Collections.sort(p1Logs);
        Collections.sort(p2Logs);

        sb.append("PLAYER 1:\n");
        if (p1Logs.isEmpty()) {
            sb.append("  (No Bases)\n");
        }
        for (String s : p1Logs) {
            sb.append(s).append("\n");
        }

        sb.append("\nPLAYER 2:\n");
        if (p2Logs.isEmpty()) {
            sb.append("  (No Bases)\n");
        }
        for (String s : p2Logs) {
            sb.append(s).append("\n");
        }

        sb.append("========================================\n");
        Gdx.app.log("XP Log", sb.toString());
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

    public boolean isFogEnabled() {
        return isFogEnabled;
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
