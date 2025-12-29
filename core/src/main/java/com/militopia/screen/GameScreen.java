package com.militopia.screen;

import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.ExtendViewport; // <--- Use this oneimport com.badlogic.gdx.utils.viewport.Viewport;
import com.militopia.MilitopiaGame;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.data.UnitData;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.SaveManager;
import com.militopia.map.MapGenerator; // Only import the outer class
import com.militopia.systems.MapRenderSystem;
import com.militopia.systems.MovementSystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.ui.GameHUD;
import com.militopia.systems.FogSystem; // Import

public class GameScreen implements Screen {

    private final MilitopiaGame game;
    private final GameState gameState;

    // --- CORE GAME COMPONENTS ---
    private OrthographicCamera camera;
    private ExtendViewport viewport;
    private PooledEngine engine;

    // CHANGED: Use the inner class type
    private MapGenerator.GameMap gameMap;

    // --- FACTORIES & MANAGERS ---
    private UnitFactory unitFactory;
    private EntityFactory entityFactory;
    private SaveManager saveManager;

    // --- SYSTEMS ---
    private MapRenderSystem mapRenderSystem;
    private UnitRenderSystem unitRenderSystem;
    private GameInputController inputController;

    // --- UI MANAGER ---
    public GameHUD gameHUD;

    // FOG System
    private FogSystem fogSystem;
    private boolean isFogEnabled = true; // State

    private BitmapFont font;

    public GameScreen(final MilitopiaGame game, GameState loadedState) {
        this.game = game;
        this.gameState = loadedState;

        // ... camera code ...
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        // ... (keep camera pos logic) ...
        float midGridX = GameConfig.MAP_WIDTH / 2f;
        float midGridY = GameConfig.MAP_HEIGHT / 2f;
        float isoCenterX = (midGridX - midGridY) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoCenterY = (midGridX + midGridY) * (GameConfig.TILE_HEIGHT / 2.0f);
        camera.position.set(isoCenterX, isoCenterY, 0);
        camera.zoom = GameConfig.STARTUP_ZOOM;
        camera.update();
        viewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);

        // 2. SETUP ENGINE & FACTORIES
        engine = new PooledEngine();

        // FIX: Pass game.assets to factories
        entityFactory = new EntityFactory(engine, game.assets);
        unitFactory = new UnitFactory(engine, game.assets);

        saveManager = new SaveManager();

        // 3. GENERATE MAP
        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, loadedState.seed);

        for (int x = 0; x < GameConfig.MAP_WIDTH; x++) {
            for (int y = 0; y < GameConfig.MAP_HEIGHT; y++) {
                MapGenerator.ObjectType type = gameMap.objects[x][y];
                if (type != MapGenerator.ObjectType.NONE) {
                    unitFactory.createObjectEntity(x, y, type);
                }
            }
        }

        // 4. ADD SYSTEMS
        engine.addSystem(new MovementSystem());

        // Assume Player 1 is the human player
        fogSystem = new FogSystem(gameMap, 1);
        engine.addSystem(fogSystem);

        // FIX: Pass UnitFactory (not Game)
        mapRenderSystem = new MapRenderSystem(game.batch, unitFactory, gameMap);
        engine.addSystem(mapRenderSystem);

        unitRenderSystem = new UnitRenderSystem(game.batch, gameMap);
        engine.addSystem(unitRenderSystem);

        // ... (Rest of code remains the same) ...
        // ... restore units ...
        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                if ("RECRUIT".equals(u.type)) {
                    unitFactory.createRecruit(u.x, u.y, u.owner);
                }
            }
        }

        gameHUD = new GameHUD(game);
        font = game.skin.getFont("default-font"); // This now works because we fixed MilitopiaGame
        font.getData().setScale(0.5f);

        inputController = new GameInputController(
                this, camera, engine, gameMap, unitFactory, entityFactory, gameHUD
        );

        gameHUD.build(this, inputController, unitFactory);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(gameHUD.stage);
        multiplexer.addProcessor(inputController);
        Gdx.input.setInputProcessor(multiplexer);
    }

    /**
     * Called by GameHUD when "Save & Exit" is clicked.
     */
    public void saveAndExit() {
        saveManager.saveGame(gameState, engine);
        game.setScreen(new com.militopia.screen.MenuScreen(game));
    }
    
    public boolean toggleFog() {
        isFogEnabled = !isFogEnabled;
        
        // Notify Renderers
        mapRenderSystem.setFogEnabled(isFogEnabled);
        unitRenderSystem.setFogEnabled(isFogEnabled);
        
        // Note: FogSystem keeps running to calculate visible tiles, 
        // renderers just choose whether to respect it or not.
        return isFogEnabled;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        inputController.update(delta);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // Update selection states for the renderers
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
    }

    @Override
    public void resize(int width, int height) {
        // 1. Update World Viewport
        // 'false' is crucial here. It tells the camera:
        // "Update the size of the screen, but DON'T reset my position to 0,0"
        viewport.update(width, height, false);

        // 2. Update HUD (UI)
        gameHUD.resize(width, height);
    }

    @Override
    public void dispose() {
        engine.clearPools();
        gameHUD.dispose();
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
