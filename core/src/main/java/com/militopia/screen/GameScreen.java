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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.militopia.MilitopiaGame;
import com.militopia.components.StatsComponent;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
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

        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, loadedState.seed);

        centerCameraOnBase(gameState.currentPlayer);

        for (int x = 0; x < GameConfig.MAP_WIDTH; x++) {
            for (int y = 0; y < GameConfig.MAP_HEIGHT; y++) {
                MapGenerator.ObjectType type = gameMap.objects[x][y];
                if (type != MapGenerator.ObjectType.NONE) {
                    unitFactory.createObjectEntity(x, y, type);
                }
            }
        }

        engine.addSystem(new MovementSystem());

        fogSystem = new FogSystem(gameMap, gameState.currentPlayer);
        engine.addSystem(fogSystem);

        mapRenderSystem = new MapRenderSystem(game.batch, unitFactory, gameMap);
        engine.addSystem(mapRenderSystem);

        unitRenderSystem = new UnitRenderSystem(game.batch, gameMap);
        engine.addSystem(unitRenderSystem);

        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                if ("RECRUIT".equals(u.type)) {
                    unitFactory.createUnit("RECRUIT", u.x, u.y, u.owner, false); // false = not new summon
                }
            }
        }

        gameHUD = new GameHUD(game);
        font = game.skin.getFont("default-font");
        font.getData().setScale(0.5f);

        inputController = new GameInputController(
                this, camera, engine, gameMap, unitFactory, entityFactory, gameHUD
        );

        gameHUD.build(this, inputController, unitFactory, gameState);
        gameHUD.updateTurn(gameState.turnCount);
        gameHUD.updateXP(gameState.p1XP);

        // Initial Income Calculation (For Display Only)
        int startIncome = calculateIncome(gameState.currentPlayer);
        gameHUD.updateFunding(gameState.p1Funding, startIncome);

        // --- NEW: Print P1 Stats at Game Start ---
        Gdx.app.log("Economy", "Turn: " + gameState.turnCount
                + " | Player: " + gameState.currentPlayer
                + " | Game Start(No Income) "
                + " | Total Funds: " + gameState.p1Funding);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(gameHUD.stage);
        multiplexer.addProcessor(inputController);
        Gdx.input.setInputProcessor(multiplexer);
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
        MapGenerator.ObjectType targetBase = (playerID == 1)
                ? MapGenerator.ObjectType.BASE_P1
                : MapGenerator.ObjectType.BASE_P2;

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
        saveManager.saveGame(gameState, engine);
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

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.60f, 0.80f, 1.00f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (turnState == TurnState.FADING_OUT) {
            fadeTime += delta;
            if (fadeTime >= FADE_DURATION) {
                fadeTime = FADE_DURATION;

                // 1. Switch Player
                gameState.currentPlayer = (gameState.currentPlayer == 1) ? 2 : 1;

                // --- FIX: Round-Based Turn Counting ---
                // Only increment turn count when P1 starts (End of Round)
                if (gameState.currentPlayer == 1) {
                    gameState.turnCount++;
                }

                // 2. Calculate Income
                int income = calculateIncome(gameState.currentPlayer);
                int currentTotal = (gameState.currentPlayer == 1) ? gameState.p1Funding : gameState.p2Funding;

                // --- FIX: Apply Income only AFTER Turn 1 ---
                if (gameState.turnCount > 1) {
                    if (gameState.currentPlayer == 1) {
                        gameState.p1Funding += income;
                        currentTotal = gameState.p1Funding;
                    } else {
                        gameState.p2Funding += income;
                        currentTotal = gameState.p2Funding;
                    }

                    // --- NEW DEBUG LOG ---
                    Gdx.app.log("Economy", "Turn: " + gameState.turnCount
                            + " | Player: " + gameState.currentPlayer
                            + " | Income: +" + income
                            + " | Total Funds: " + currentTotal);
                } else {
                    Gdx.app.log("Economy", "Turn: " + gameState.turnCount
                            + " | Player: " + gameState.currentPlayer
                            + " | Game Start (No Income) | Total Funds: " + currentTotal);
                }

                resetUnitActions();

                // 3. Update HUD
                gameHUD.updateTurn(gameState.turnCount);
                int currentXP = (gameState.currentPlayer == 1) ? gameState.p1XP : gameState.p2XP;
                gameHUD.updateXP(currentXP);

                // Update label with current funds AND income rate (e.g., "5 (+2)")
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
