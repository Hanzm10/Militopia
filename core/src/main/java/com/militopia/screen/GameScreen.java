package com.militopia.screen;

import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.factories.EntityFactory;
import com.militopia.config.GameConfig;
import com.militopia.data.GameState;
import com.militopia.map.MapGenerator;
import com.militopia.MilitopiaGame;
import com.militopia.data.UnitData;
import com.militopia.factories.UnitFactory;
import com.militopia.controller.GameInputController;
import com.militopia.systems.MapRenderSystem;
import com.militopia.systems.MovementSystem;
import com.militopia.systems.UnitRenderSystem;
import com.militopia.managers.SaveManager;
import com.militopia.utils.HoverListener;

public class GameScreen implements Screen {

    final MilitopiaGame game;
    OrthographicCamera camera;
    FitViewport viewport;

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

    SaveManager saveManager;
    private GameState gameState;

    // --- 3. STATE (Shared with Controller) ---
    // Public so InputController can update them
    public int selectedX = -1, selectedY = -1;
    public int bouncingX = -1, bouncingY = -1;
    public float bounceTimer = 0;

    // Game Session Data
    long seed;
    String p1Name, p2Name, saveName;

    // Top HUD Labels (So we can update them later)
    private Label xpLabel;
    private Label fundsLabel;
    private Label turnLabel;

    private Stage stage;

    // Settings Overlay
    private Table settingsOverlay;

    public GameScreen(final MilitopiaGame game, GameState loadedState) {
        this.game = game;
        this.gameState = loadedState;

        // Unpack GameState
        this.seed = loadedState.seed;
        this.p1Name = loadedState.p1Name;
        this.p2Name = loadedState.p2Name;
        this.saveName = loadedState.saveName;

        // 1. Setup Camera & Viewport
        camera = new OrthographicCamera();
        camera.setToOrtho(false, GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT);
        camera.zoom = 1.0f;
        viewport = new FitViewport(GameConfig.DRAW_WIDTH, GameConfig.DRAW_HEIGHT, camera);

        // 2. Setup ECS Engine & Factories
        engine = new PooledEngine();
        entityFactory = new EntityFactory(engine);
        unitFactory = new UnitFactory(engine);
        saveManager = new SaveManager();

        // 3. Generate Map
        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(GameConfig.MAP_WIDTH, GameConfig.MAP_HEIGHT, seed);

        // --- Center Camera Logic ---
        float midX = GameConfig.MAP_WIDTH / 2f;
        float midY = GameConfig.MAP_HEIGHT / 2f;
        float isoCenterX = (midX - midY) * (GameConfig.TILE_WIDTH / 2.0f);
        float isoCenterY = (midX + midY) * (GameConfig.TILE_HEIGHT / 2.0f);
        camera.position.set(isoCenterX, isoCenterY, 0);
        camera.update();

        // 4. Add Systems
        engine.addSystem(new MovementSystem());
        mapRenderSystem = new MapRenderSystem(game.batch, game, gameMap, p1Name, p2Name);
        engine.addSystem(mapRenderSystem);
        unitRenderSystem = new UnitRenderSystem(game.batch);
        engine.addSystem(unitRenderSystem);

        // 5. Restore Units
        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                if (u.type.equals("RECRUIT")) {
                    unitFactory.createRecruit(u.x, u.y, u.owner);
                }
            }
        }

        font = game.skin.getFont("default-font"); // Initialize the font!
        font.getData().setScale(0.5f); // Keep your scaling preference

        // 6. INITIALIZE UI VARIABLES (The Fix)
        // We create the Stage and Menu objects NOW so we can pass them to the controller.
        stage = new Stage(new ScreenViewport());
        summonMenu = new Table(); // Create the table reference now!

        // 7. Setup Input Controller
        // Now it's safe to pass 'summonMenu' because we just created it above.
        inputController = new GameInputController(
                this, camera, engine, gameMap, unitFactory, entityFactory, summonMenu
        );

        // 8. Setup HUD (Populate the UI)
        // We removed the 'new Stage' and 'setInputProcessor' lines from inside setupHUD()
        // so it purely builds the UI elements now.
        setupHUD();

        // 9. Final Input Setup
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);           // UI Clicks First
        multiplexer.addProcessor(inputController); // Map Clicks Second
        Gdx.input.setInputProcessor(multiplexer);
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
        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }
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
        // ============================================
        // 1. TOP HUD (XP, Funding, Turn)
        // ============================================
        Table topTable = new Table();
        topTable.top().padTop(20);
        topTable.setFillParent(true);

        topTable.add(createStatGroup("XP", "0")).expandX();
        topTable.add(createStatGroup("Funding", "1000")).expandX();
        topTable.add(createStatGroup("Turn", "1")).expandX();

        stage.addActor(topTable);

        // ============================================
        // 2. BOTTOM HUD (Settings, Stats, End Turn)
        // ============================================
        Table bottomTable = new Table();
        bottomTable.bottom().padBottom(20);
        bottomTable.setFillParent(true);

        ImageButton settingsBtn = createCircleButton("icon_settings");
        ImageButton statsBtn = createCircleButton("icon_stats");
        ImageButton endTurnBtn = createCircleButton("icon_end");

        bottomTable.add(createIconGroup(settingsBtn, "Settings")).expandX();
        bottomTable.add(createIconGroup(statsBtn, "Game Stats")).expandX();
        bottomTable.add(createIconGroup(endTurnBtn, "End Turn")).expandX();

        stage.addActor(bottomTable);

        // ============================================
        // 3. SUMMON MENU (The missing part!)
        // ============================================
        // Note: summonMenu was initialized in the Constructor, so we just config it here.
        summonMenu.setVisible(false); // Hidden by default
        summonMenu.setBackground(game.skin.newDrawable("white", Color.DARK_GRAY)); // Grey background
        summonMenu.setSize(200, 150);
        summonMenu.setPosition(GameConfig.DRAW_WIDTH / 2f - 100, GameConfig.DRAW_HEIGHT / 2f - 75); // Center it

        Label summonLabel = new Label("Summon Unit", game.skin);
        TextButton recruitBtn = new TextButton("Recruit", game.skin);
        TextButton cancelBtn = new TextButton("Cancel", game.skin);

        // --- Recruit Logic ---
        recruitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int tx = inputController.getLastClickedX();
                int ty = inputController.getLastClickedY();

                if (tx != -1 && ty != -1) {
                    // Check ownership of the base to spawn the correct unit color
                    int owner = 1;
                    if (gameMap.objects[tx][ty] == MapGenerator.ObjectType.BASE_P2) {
                        owner = 2;
                    }

                    unitFactory.createRecruit(tx, ty, owner);
                    summonMenu.setVisible(false);
                    inputController.resetLastClicked();
                }
            }
        });

        // --- Cancel Logic ---
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                summonMenu.setVisible(false);
                inputController.resetLastClicked();
            }
        });

        // Add hover effects
        recruitBtn.addListener(new HoverListener());
        cancelBtn.addListener(new HoverListener());

        // Layout the menu
        summonMenu.add(summonLabel).pad(10).row();
        summonMenu.add(recruitBtn).fillX().pad(5).row();
        summonMenu.add(cancelBtn).fillX().pad(5);

        stage.addActor(summonMenu);

        // ============================================
        // 4. SETTINGS OVERLAY
        // ============================================
        createSettingsOverlay();

        // ============================================
        // 5. BOTTOM BUTTON LISTENERS
        // ============================================
        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.setVisible(true);
            }
        });

        settingsBtn.addListener(new HoverListener());
        statsBtn.addListener(new HoverListener());
        endTurnBtn.addListener(new HoverListener());
    }

    // --- HELPER: Create Top Stat (Label over Value) ---
    private Table createStatGroup(String title, String placeholderValue) {
        Table t = new Table();
        Label titleLbl = new Label(title, game.skin, "default-font", Color.GRAY);
        titleLbl.setFontScale(0.8f); // Smaller title

        Label valLbl = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
        valLbl.setFontScale(1.2f); // Bigger value

        // Save references if needed (Logic for later)
        if (title.equals("XP")) {
            xpLabel = valLbl;
        }
        if (title.equals("Funding")) {
            fundsLabel = valLbl;
        }
        if (title.equals("Turn")) {
            turnLabel = valLbl;
        }

        t.add(titleLbl).row();
        t.add(valLbl);
        return t;
    }

    // --- HELPER: Create Circle Icon Button ---
    private ImageButton createCircleButton(String iconName) {
        // 1. Create a "Circle" background
        // Since we don't have a circle texture yet, we use a standard button style
        // Later, you can create a specific ImageButtonStyle in your skin
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = game.skin.newDrawable("white", Color.DARK_GRAY); // Placeholder Circle
        style.down = game.skin.newDrawable("white", Color.GRAY);

        // Try to load icon, fallback to nothing if missing
        try {
            style.imageUp = new TextureRegionDrawable(new TextureRegion(new Texture(iconName + ".png")));
        } catch (Exception e) {
            // Icon missing? Just leave it blank for now
        }

        ImageButton btn = new ImageButton(style);

        // Make it round-ish using simple scaling (or use a real circle PNG later)
        btn.setSize(60, 60);
        return btn;
    }

    // --- HELPER: Group Button + Label for Bottom HUD ---
    private Table createIconGroup(ImageButton btn, String labelText) {
        Table t = new Table();
        t.add(btn).size(60, 60).row(); // Force size

        Label lbl = new Label(labelText, game.skin, "default-font", Color.WHITE);
        lbl.setFontScale(0.7f);
        t.add(lbl).padTop(5);
        return t;
    }

    // --- HELPER: Create the Settings Popup ---
    private void createSettingsOverlay() {
        settingsOverlay = new Table();
        settingsOverlay.setFillParent(true);
        settingsOverlay.setVisible(false); // Hidden start

        // 1. Dark Background (The "Blur" effect)
        // We create a black pixel and make it 80% transparent
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0.85f); // Dark tint
        p.fill();
        settingsOverlay.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(p))));
        p.dispose();

        // 2. The "Menu Box" in the center
        Table menuBox = new Table();
        menuBox.setBackground(game.skin.newDrawable("white", Color.DARK_GRAY)); // Grey box

        Label title = new Label("PAUSED", game.skin);
        title.setFontScale(1.5f);

        TextButton saveExitBtn = new TextButton("Save & Exit", game.skin);
        saveExitBtn.addListener(new HoverListener());

        // 3. Save & Exit Logic
        saveExitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // A. Save the Game
                SaveManager saveManager = new SaveManager();
                saveManager.saveGame(gameState, engine);

                // B. Go to Menu
                game.setScreen(new com.militopia.screen.MenuScreen(game));
            }
        });

        TextButton resumeBtn = new TextButton("Resume", game.skin);
        resumeBtn.addListener(new HoverListener());
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.setVisible(false); // Hide overlay
            }
        });

        // Layout the box
        menuBox.add(title).pad(20).row();
        menuBox.add(saveExitBtn).size(200, 50).pad(10).row();
        menuBox.add(resumeBtn).size(200, 50).pad(10);

        settingsOverlay.add(menuBox).size(300, 250); // Add box to overlay

        stage.addActor(settingsOverlay); // Add to stage LAST so it's on top
    }

    public void updateSelection(int x, int y) {
        this.selectedX = x;
        this.selectedY = y;
    }

    public void triggerBounce(int x, int y) {
        this.bouncingX = x;
        this.bouncingY = y;
        this.bounceTimer = 0;
    }

    @Override
    public void resize(int width, int height) {
        // Update Viewport
        viewport.update(width, height);

        // Update UI Stage (CHANGE 'hudStage' TO 'stage')
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
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
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    @Override
    public void dispose() {
        hudStage.dispose();
        // Engine typically doesn't need explicit dispose unless you have custom resources
    }
}
