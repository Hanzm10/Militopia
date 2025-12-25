package com.militopia.screen;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.factories.EntityFactory;
import com.militopia.config.GameConfig;
import com.militopia.data.GameState;
import com.militopia.map.MapGenerator;
import com.militopia.MilitopiaGame;
import com.militopia.data.UnitData;
import com.militopia.factories.UnitFactory;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.TypeComponent;
import com.militopia.controller.GameInputController;
import com.militopia.systems.MapRenderSystem;
import com.militopia.systems.MovementSystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.managers.SaveManager;

public class GameScreen implements Screen {

    final MilitopiaGame game;
    OrthographicCamera camera;

    // --- 1. DATA & SYSTEMS ---
    MapGenerator.GameMap gameMap;
    PooledEngine engine;

    // Systems
    MapRenderSystem mapRenderSystem;
    UnitRenderSystem unitRenderSystem;

    // Factories
    EntityFactory entityFactory;
    UnitFactory unitFactory;

    // --- 2. INPUT & UI ---
    GameInputController inputController;
    Stage hudStage;
    Table summonMenu;
    BitmapFont font;

    SaveManager saveManager; // <--- Add this

    // --- 3. STATE (Shared with Controller) ---
    // Public so InputController can update them
    public int selectedX = -1, selectedY = -1;
    public int bouncingX = -1, bouncingY = -1;
    public float bounceTimer = 0;

    // Game Session Data
    long seed;
    String p1Name, p2Name, saveName;

    public GameScreen(final MilitopiaGame game, GameState loadedState) {
        this.game = game;

        // Unpack GameState
        this.seed = loadedState.seed;
        this.p1Name = loadedState.p1Name;
        this.p2Name = loadedState.p2Name;
        this.saveName = loadedState.saveName;

        // 1. Setup Camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        camera.zoom = 1.0f;

        // 2. Setup ECS Engine & Factories
        engine = new PooledEngine();
        entityFactory = new EntityFactory(engine);
        unitFactory = new UnitFactory(engine);

        // 3. Generate Map
        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, seed);

        // Center Map on Start-up
        float midX = GameConfig.MAP_WIDTH / 2f;
        float midY = GameConfig.MAP_HEIGHT / 2f;

        // 2. Convert Grid(16, 16) to Isometric Coordinates
        // (This uses the exact same math as your Render System)
        float isoCenterX = (midX - midY) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoCenterY = (midX + midY) * (GameConfig.TILE_HEIGHT / 2.0f);

        // 3. Move Camera
        camera.position.set(isoCenterX, isoCenterY, 0);
        camera.update();

        // 4. Add Systems
        // A. Movement Logic (Updates timers)
        engine.addSystem(new MovementSystem());

        // B. Map Rendering (Priority 0 - Bottom)
        mapRenderSystem = new MapRenderSystem(game.batch, game, gameMap, p1Name, p2Name);
        engine.addSystem(mapRenderSystem);

        unitRenderSystem = new UnitRenderSystem(game.batch); // Store it in the variable!
        engine.addSystem(unitRenderSystem);

        // 5. Restore Units from Save File
        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                if (u.type.equals("RECRUIT")) {
                    unitFactory.createRecruit(u.x, u.y);
                }
            }
        }

        // 6. Setup Fonts & UI
        font = game.skin.getFont("default-font"); // Use the name you added in MilitopiaGame
        font.getData().setScale(0.5f); // Scale down if 24px is too big for the map names
        setupHUD(); // Creates 'hudStage' and 'summonMenu'

        // 7. Setup Input Controller (Connects everything)
        inputController = new GameInputController(
                this, camera, engine, gameMap, unitFactory, entityFactory, summonMenu
        );

        // 8. Setup Input Multiplexer
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hudStage);      // UI First
        multiplexer.addProcessor(inputController); // Logic Second
        Gdx.input.setInputProcessor(multiplexer);

        // Inside GameScreen Constructor
        saveManager = new SaveManager();
    }

    // ========================================================================
    //                            MAIN LOOP
    // ========================================================================
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.53f, 0.81f, 0.92f, 1); // Sky Blue

        // 1. Update Camera
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        // 2. Update Bounce Timer (Visuals only)
        if (bouncingX != -1) {
            bounceTimer += delta;
            if (bounceTimer >= GameConfig.BOUNCE_DURATION) {
                bouncingX = -1;
                bouncingY = -1;
            }
        }

        game.batch.begin();

        // 3. Sync State to Map & Unit System (Pass dynamic variables)
        mapRenderSystem.updateState(selectedX, selectedY, bouncingX, bouncingY, bounceTimer);
        
        unitRenderSystem.updateState(selectedX, selectedY);

        // 4. Run ECS Engine (Draws Map -> Then Units)
        engine.update(delta);

        // 5. Draw UI Overlays (XP Bars / Names) - "Pass 4"
        renderUI();

        game.batch.end();

        // 6. Draw HUD (Buttons)
        hudStage.act();
        hudStage.draw();
    }

    // ========================================================================
    //                            UI & HUD
    // ========================================================================
    private void renderUI() {
        // Loop purely to find bases and draw their names/bars on top of everything
        float xOffset = (GameConfig.DRAW_WIDTH - GameConfig.TILE_WIDTH) / 2f;
        float yOffset = (GameConfig.DRAW_HEIGHT - GameConfig.TILE_HEIGHT) / 2f;

        for (int x = GameConfig.MAP_WIDTH - 1; x >= 0; x--) {
            for (int y = GameConfig.MAP_HEIGHT - 1; y >= 0; y--) {
                MapGenerator.ObjectType obj = gameMap.objects[x][y];

                if (obj == MapGenerator.ObjectType.BASE_P1 || obj == MapGenerator.ObjectType.BASE_P2) {
                    float isoX = (x - y) * (GameConfig.TILE_WIDTH / 2.0f);
                    float isoY = (x + y) * (GameConfig.TILE_HEIGHT / 2.0f);

                    float animY = 0;
                    if (x == bouncingX && y == bouncingY) {
                        float progress = bounceTimer / GameConfig.BOUNCE_DURATION;
                        animY = (float) Math.sin(progress * Math.PI) * GameConfig.BOUNCE_HEIGHT;
                    }

                    String baseName = (obj == MapGenerator.ObjectType.BASE_P1) ? p1Name : p2Name;
                    Color baseColor = (obj == MapGenerator.ObjectType.BASE_P1) ? Color.BLUE : Color.RED;

                    drawBaseHUD(isoX - xOffset, isoY - yOffset + animY, baseName, baseColor);
                }
            }
        }
    }

    private void drawBaseHUD(float x, float y, String name, Color baseColor) {
        Texture whiteTex = game.skin.get("white", Texture.class);
        float barWidth = 40f;
        float barHeight = 6f;
        float xOffset = (GameConfig.DRAW_WIDTH - barWidth) / 2f;
        float yOffset = 15f; // Lift above dirt

        // Draw Bar Background
        game.batch.setColor(Color.BLACK);
        game.batch.draw(whiteTex, x + xOffset, y + yOffset, barWidth, barHeight);

        // Draw Bar Fill (50% placeholder)
        float fillPercent = 0.5f;
        game.batch.setColor(baseColor);
        game.batch.draw(whiteTex, x + xOffset + 1, y + yOffset + 1, (barWidth - 2) * fillPercent, barHeight - 2);

        // Draw Name
        font.setColor(Color.WHITE);
        GlyphLayout layout = new GlyphLayout(font, name);
        float textX = x + (GameConfig.DRAW_WIDTH / 2f) - (layout.width / 2f);
        float textY = y + yOffset + 40;

        font.draw(game.batch, layout, textX, textY);
        game.batch.setColor(Color.WHITE);
    }

    private void setupHUD() {
        hudStage = new Stage(new ScreenViewport());

        // A. Save Button
        TextButton saveBtn = new TextButton("Save & Exit", game.skin);
        saveBtn.setPosition(20, Gdx.graphics.getHeight() - 50);
        saveBtn.setSize(120, 40);
        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                saveGame();
                game.setScreen(new MenuScreen(game));
            }
        });
        hudStage.addActor(saveBtn);

        // B. Summon Menu
        summonMenu = new Table();
        summonMenu.bottom();
        summonMenu.setFillParent(true);

        TextButton summonBtn = new TextButton("Summon Recruit", game.skin);
        summonBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Ask controller where the last base click was
                int tx = inputController.getLastClickedX();
                int ty = inputController.getLastClickedY();

                if (tx != -1 && ty != -1) {
                    unitFactory.createRecruit(tx, ty);
                    summonMenu.setVisible(false);
                    inputController.resetLastClicked();
                }
            }
        });

        summonMenu.add(summonBtn).padBottom(20);
        summonMenu.setVisible(false);
        hudStage.addActor(summonMenu);
    }

    // ========================================================================
    //                      HELPER METHODS (Called by Controller)
    // ========================================================================
    public void updateSelection(int x, int y) {
        this.selectedX = x;
        this.selectedY = y;
    }

    public void triggerBounce(int x, int y) {
        this.bouncingX = x;
        this.bouncingY = y;
        this.bounceTimer = 0;
    }

    private void saveGame() {
        // Pass the raw data to the manager
        saveManager.saveGame(seed, p1Name, p2Name, saveName, engine);
    }

    @Override
    public void resize(int width, int height) {
        hudStage.getViewport().update(width, height, true);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
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

    @Override
    public void dispose() {
        hudStage.dispose();
        // Engine typically doesn't need explicit dispose unless you have custom resources
    }
}
