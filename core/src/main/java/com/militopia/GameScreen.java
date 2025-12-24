package com.militopia;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import static com.militopia.MapGenerator.ObjectType.BASE_NEUTRAL;
import static com.militopia.MapGenerator.ObjectType.BASE_P1;
import static com.militopia.MapGenerator.ObjectType.BASE_P2;
import static com.militopia.MapGenerator.ObjectType.CACTUS;
import static com.militopia.MapGenerator.ObjectType.OIL;
import static com.militopia.MapGenerator.ObjectType.RUINS;
import static com.militopia.MapGenerator.ObjectType.TREE;
import static com.militopia.MapGenerator.TerrainType.DEEP_WATER;
import static com.militopia.MapGenerator.TerrainType.FOREST;
import static com.militopia.MapGenerator.TerrainType.GRASS;
import static com.militopia.MapGenerator.TerrainType.SAND;
import static com.militopia.MapGenerator.TerrainType.WATER;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.MovementComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TextureComponent;
import com.militopia.components.TypeComponent;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Texture;

public class GameScreen extends InputAdapter implements Screen { // Extend InputAdapter

    final MilitopiaGame game;
    MapGenerator.GameMap gameMap;
    OrthographicCamera camera;

    PooledEngine engine;
    EntityFactory entityFactory;
    UnitFactory unitFactory;

    // UI (HUD)
    Stage hudStage;
    Table summonMenu;
    BitmapFont font;

    // Logic Vars
    long seed;
    String p1Name, p2Name, saveName;

    // Grid settings (Same as before)
    final int MAP_WIDTH = 32;
    final int MAP_HEIGHT = 32;
    final int TILE_WIDTH = 25;
    final int TILE_HEIGHT = 15;
    final float DRAW_WIDTH = 30f;
    final float DRAW_HEIGHT = 30f;

    // Input Vars
    float lastTouchX, lastTouchY;
    int selectedX = -1, selectedY = -1;

    // BOUNCE ANIMATION VARIABLES
    int bouncingX = -1;
    int bouncingY = -1;
    float bounceTimer = 0;

    final float BOUNCE_DURATION = 0.25f; // Duration in seconds (fast bounce)
    final float BOUNCE_HEIGHT = 5f;     // How high it jumps in pixels

    int lastClickedX = -1;
    int lastClickedY = -1;

    Entity selectedUnitEntity = null;

    public GameScreen(final MilitopiaGame game, GameState loadedState) {
        this.game = game;
        this.seed = loadedState.seed;
        this.p1Name = loadedState.p1Name;
        this.p2Name = loadedState.p2Name;
        // Create a filename based on P1 vs P2 (e.g., "P1_vs_P2.json")
        this.saveName = loadedState.saveName;

        // 1. Setup Camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        camera.position.set(0, 0, 0);
        camera.update();

        // 1. Setup ECS
        engine = new PooledEngine();
        entityFactory = new EntityFactory(engine);
        unitFactory = new UnitFactory(engine);

        setupHUD();

        font = game.skin.getFont("default");
        font.getData().setScale(0.8f); // Make text slightly smaller to fit

        // 3. Setup Input (Handle BOTH Map clicks and Button clicks)
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hudStage); // UI gets clicks first
        multiplexer.addProcessor(this);     // Map gets clicks second
        Gdx.input.setInputProcessor(multiplexer);

        // 4. Generate Map
        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(MAP_WIDTH, MAP_HEIGHT, seed);

        // --- NEW: RESTORE UNITS ---
        if (loadedState.units != null) {
            for (UnitData u : loadedState.units) {
                // Re-create the entity at the saved spot
                if (u.type.equals("RECRUIT")) {
                    unitFactory.createRecruit(u.x, u.y);
                }
            }
            System.out.println("Restored " + loadedState.units.size() + " units.");
        }
    }

    private void setupHUD() {
        // 1. Initialize the Stage (if not already done in constructor)
        if (hudStage == null) {
            hudStage = new Stage(new ScreenViewport());
        }

        // --- PART A: THE SAVE BUTTON (Top Left) ---
        TextButton saveBtn = new TextButton("Save & Exit", game.skin);
        saveBtn.setPosition(20, Gdx.graphics.getHeight() - 50);
        saveBtn.setSize(120, 40);

        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Call your save function
                saveGame(); // Ensure you still have the saveGame() method in your class!
                game.setScreen(new MenuScreen(game));
            }
        });

        hudStage.addActor(saveBtn);

        // --- PART B: THE SUMMON MENU (Bottom Center) ---
        summonMenu = new Table();
        summonMenu.bottom();
        summonMenu.setFillParent(true);

        TextButton summonBtn = new TextButton("Summon Recruit", game.skin);
        summonBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Use locked coordinates
                if (lastClickedX != -1 && lastClickedY != -1) {
                    System.out.println("Spawning at: " + lastClickedX + "," + lastClickedY);
                    unitFactory.createRecruit(lastClickedX, lastClickedY);

                    summonMenu.setVisible(false);
                    lastClickedX = -1;
                    lastClickedY = -1;
                }
            }
        });

        summonMenu.add(summonBtn).padBottom(20);
        summonMenu.setVisible(false); // Start hidden

        hudStage.addActor(summonMenu);
    }

    private void drawBaseHUD(float x, float y, String name, Color baseColor) {
        // 1. GET THE WHITE TEXTURE CORRECTLY
        // We use get(Texture.class) because we added it as a raw Texture, not a Region
        Texture whiteTex = game.skin.get("white", Texture.class);

        // 2. SETUP DIMENSIONS
        float barWidth = 40f;
        float barHeight = 6f;

        // Center the bar relative to the Tile Width (DRAW_WIDTH is 64)
        float xOffset = (DRAW_WIDTH - barWidth) / 2f;

        // Move the HUD UP so it sits on the base, not under the dirt
        // +10 puts it near the "feet" of the base on top of the block
        float yOffset = 15f;

        // 3. DRAW XP BAR BACKGROUND (Black Border)
        game.batch.setColor(Color.BLACK);
        game.batch.draw(whiteTex, x + xOffset, y + yOffset, barWidth, barHeight);

        // 4. DRAW XP BAR FILL (Colored)
        float fillPercent = 0.5f;
        game.batch.setColor(baseColor);
        game.batch.draw(whiteTex, x + xOffset + 1, y + yOffset + 1, (barWidth - 2) * fillPercent, barHeight - 2);

        // 5. DRAW PLAYER NAME (Perfectly Centered)
        font.setColor(Color.WHITE);

        // Use GlyphLayout to measure the exact width of this specific name
        GlyphLayout layout = new GlyphLayout(font, name);

        // Math: (TileCenter) - (TextHalfWidth)
        float textX = x + (DRAW_WIDTH / 2f) - (layout.width / 2f);
        float textY = y + yOffset + 40; // 40px above the bar

        font.draw(game.batch, layout, textX, textY);

        // 6. RESET COLOR
        game.batch.setColor(Color.WHITE);
    }

    private void saveGame() {
        GameState state = new GameState(seed, p1Name, p2Name, saveName);

        // --- NEW: SAVE UNITS ---
        // 1. Get all Units from the Engine
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());

        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);

            // Only save UNITs (Ignore markers)
            if (type.type == TypeComponent.Type.UNIT) {
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

                // Add to the list
                state.units.add(new UnitData(pos.x, pos.y, "RECRUIT"));
            }
        }
        // -----------------------

        Json json = new Json();
        String text = json.toJson(state);

        FileHandle file = Gdx.files.local("saves/" + saveName + ".json");
        file.writeString(text, false);
        System.out.println("Saved " + state.units.size() + " units.");
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.53f, 0.81f, 0.92f, 1); // Sky Blue

        // 1. Draw Map
        handleInput(delta);
        updateHoveredTile();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        renderMapLoop(delta);
        game.batch.end();

        // 2. Draw UI on top
        hudStage.act();
        hudStage.draw();
    }

    private void renderMapLoop(float delta) {
        // --- 1. UPDATE BOUNCE TIMER ---
        if (bouncingX != -1) {
            bounceTimer += delta;
            if (bounceTimer >= BOUNCE_DURATION) {
                bouncingX = -1;
                bouncingY = -1;
            }
        }

        // Common offsets for centering images
        float xOffset = (DRAW_WIDTH - TILE_WIDTH) / 2f;
        float yOffset = (DRAW_HEIGHT - TILE_HEIGHT) / 2f;

        // --- 2. PASS 1: DRAW WORLD (Terrain, Objects) ---
        for (int x = MAP_WIDTH - 1; x >= 0; x--) {
            for (int y = MAP_HEIGHT - 1; y >= 0; y--) {
                float isoX = (x - y) * (TILE_WIDTH / 2.0f);
                float isoY = (x + y) * (TILE_HEIGHT / 2.0f);

                float animY = 0;
                if (x == bouncingX && y == bouncingY) {
                    float progress = bounceTimer / BOUNCE_DURATION;
                    animY = (float) Math.sin(progress * Math.PI) * BOUNCE_HEIGHT;
                }

                // A. Draw Terrain
                Texture t = null;
                switch (gameMap.terrain[x][y]) {
                    case GRASS:      t = game.texGrass; break;
                    case WATER:      t = game.texWater; break;
                    case DEEP_WATER: t = game.texDeepWater; break;
                    case SAND:       t = game.texSand; break;
                    case FOREST:     t = game.texForest; break;
                }
                if (t != null) game.batch.draw(t, isoX - xOffset, isoY - yOffset + animY, DRAW_WIDTH, DRAW_HEIGHT);

                // B. Draw Objects
                Texture o = null;
                switch (gameMap.objects[x][y]) {
                    case BASE_P1:      o = game.texBaseP1; break;
                    case BASE_P2:      o = game.texBaseP2; break;
                    case BASE_NEUTRAL: o = game.texBaseNeutral; break;
                    case TREE:         o = game.texTree; break;
                    case RUINS:        o = game.texRuins; break;
                    case OIL:          o = game.texOil; break;
                    case CACTUS:       o = game.texCactus; break;
                }
                
                if (o != null) {
                    // Use calculated alignment fixes we discussed
                    float objOffsetX = (DRAW_WIDTH - TILE_WIDTH) / 2f; 
                    float surfaceLift = 15f; 
                    
                    game.batch.draw(o, 
                       isoX - objOffsetX, 
                       isoY - yOffset + surfaceLift + animY, 
                       DRAW_WIDTH, DRAW_HEIGHT);
                }

                // C. Selection Highlight
                if (x == selectedX && y == selectedY) {
                    Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                    game.batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);
                    game.batch.setColor(0.4f, 0.4f, 0.4f, 1f);
                    if (t != null) game.batch.draw(t, isoX - xOffset, isoY - yOffset + animY, DRAW_WIDTH, DRAW_HEIGHT);
                    game.batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
                    game.batch.setColor(Color.WHITE);
                }
            }
        }

        // --- 3. PASS 2: DRAW UNITS (With Animation Restored!) ---
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TextureComponent.class).get());
        
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TextureComponent tex = e.getComponent(TextureComponent.class);
            TypeComponent typeC = e.getComponent(TypeComponent.class);
            MovementComponent move = e.getComponent(MovementComponent.class);

            float isoX, isoY;

            // --- RESTORED ANIMATION LOGIC ---
            if (move != null) {
                move.time += delta;
                float alpha = Math.min(move.time / move.duration, 1.0f);
                
                // Calculate Start and End pixels
                float startIsoX = (move.startX - move.startY) * (TILE_WIDTH / 2.0f);
                float startIsoY = (move.startX + move.startY) * (TILE_HEIGHT / 2.0f);
                float endIsoX = (move.targetX - move.targetY) * (TILE_WIDTH / 2.0f);
                float endIsoY = (move.targetX + move.targetY) * (TILE_HEIGHT / 2.0f);

                // Slide between them
                isoX = MathUtils.lerp(startIsoX, endIsoX, alpha);
                isoY = MathUtils.lerp(startIsoY, endIsoY, alpha);

                if (alpha >= 1.0f) {
                    e.remove(MovementComponent.class); // Stop moving
                }
            } else {
                // Static Position
                isoX = (pos.x - pos.y) * (TILE_WIDTH / 2.0f);
                isoY = (pos.x + pos.y) * (TILE_HEIGHT / 2.0f);
            }
            // --------------------------------

            boolean isMarker = (typeC.type == TypeComponent.Type.MARKER);
            float verticalOffset = isMarker ? 7.5f : 15f;

            // Unit Bounce (only if NOT moving)
            float unitAnimY = 0;
            if (move == null && pos.x == bouncingX && pos.y == bouncingY) {
                float progress = bounceTimer / BOUNCE_DURATION;
                unitAnimY = (float) Math.sin(progress * Math.PI) * BOUNCE_HEIGHT;
            }

            game.batch.draw(tex.region, 
                            isoX - xOffset, 
                            isoY - yOffset + verticalOffset + unitAnimY, 
                            DRAW_WIDTH, DRAW_HEIGHT);
        }

        // --- 4. PASS 3: DRAW UI OVERLAYS (XP Bars) ---
        for (int x = MAP_WIDTH - 1; x >= 0; x--) {
            for (int y = MAP_HEIGHT - 1; y >= 0; y--) {
                MapGenerator.ObjectType obj = gameMap.objects[x][y];
                
                if (obj == MapGenerator.ObjectType.BASE_P1 || obj == MapGenerator.ObjectType.BASE_P2) {
                    float isoX = (x - y) * (TILE_WIDTH / 2.0f);
                    float isoY = (x + y) * (TILE_HEIGHT / 2.0f);
                    
                    float animY = 0;
                    if (x == bouncingX && y == bouncingY) {
                         float progress = bounceTimer / BOUNCE_DURATION;
                         animY = (float) Math.sin(progress * Math.PI) * BOUNCE_HEIGHT;
                    }

                    String baseName = (obj == MapGenerator.ObjectType.BASE_P1) ? p1Name : p2Name;
                    Color baseColor = (obj == MapGenerator.ObjectType.BASE_P1) ? Color.BLUE : Color.RED;

                    drawBaseHUD(isoX - xOffset, isoY - yOffset + animY, baseName, baseColor);
                }
            }
        }
    }
    
    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom += 0.02f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            camera.zoom -= 0.02f;
        }
        camera.zoom = MathUtils.clamp(camera.zoom, 0.1f, 3.0f);
    }

    private void updateHoveredTile() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();

        Vector3 worldCoords = camera.unproject(new Vector3(mouseX, mouseY, 0));

        float heightOffset = 10f;

        float adjustedY = worldCoords.y - heightOffset;
        // -----------------------

        float halfW = TILE_WIDTH / 2.0f;
        float halfH = TILE_HEIGHT / 2.0f;

        // Use 'adjustedY' instead of 'worldCoords.y'
        int gridX = MathUtils.floor((adjustedY / halfH + worldCoords.x / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - worldCoords.x / halfW) / 2);

        if (gridX >= 0 && gridX < MAP_WIDTH && gridY >= 0 && gridY < MAP_HEIGHT) {
            selectedX = gridX;
            selectedY = gridY;
        } else {
            selectedX = -1;
            selectedY = -1;
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // 1. UPDATE LAST TOUCH
        lastTouchX = screenX;
        lastTouchY = screenY;

        // 2. CONVERT MOUSE CLICKS
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));

        float heightOffset = 10f;
        float adjustedY = worldCoords.y - heightOffset;
        float halfW = TILE_WIDTH / 2.0f;
        float halfH = TILE_HEIGHT / 2.0f;

        int gridX = MathUtils.floor((adjustedY / halfH + worldCoords.x / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - worldCoords.x / halfW) / 2);

        // 3. BOUNDS CHECK
        if (gridX >= 0 && gridX < MAP_WIDTH && gridY >= 0 && gridY < MAP_HEIGHT) {
            selectedX = gridX;
            selectedY = gridY;

            // --- RE-ADDED BOUNCE TRIGGER (THE FIX) ---
            bouncingX = gridX;
            bouncingY = gridY;
            bounceTimer = 0; // Reset animation to start immediately
            // -----------------------------------------

            // --- PRIORITY 1: CLICKED MARKER? (Move Unit) ---
            Entity clickedMarker = getEntityAt(selectedX, selectedY, TypeComponent.Type.MARKER);

            if (clickedMarker != null && selectedUnitEntity != null) {
                System.out.println("Moving Unit to " + selectedX + "," + selectedY);
                moveUnit(selectedUnitEntity, selectedX, selectedY);
                return true;
            }

            // --- RESET STATE ---
            clearMarkers();
            selectedUnitEntity = null;
            summonMenu.setVisible(false);

            // --- PRIORITY 2: CLICKED UNIT? (Select Unit) ---
            Entity clickedUnit = getEntityAt(selectedX, selectedY, TypeComponent.Type.UNIT);

            if (clickedUnit != null) {
                System.out.println("Unit Selected!");
                selectedUnitEntity = clickedUnit;
                showMovementMarkers(selectedX, selectedY);
                return true;
            }

            // --- PRIORITY 3: CLICKED BASE? (Summon Menu) ---
            MapGenerator.ObjectType obj = gameMap.objects[selectedX][selectedY];
            if (obj == MapGenerator.ObjectType.BASE_P1) {
                lastClickedX = selectedX; // Lock target
                lastClickedY = selectedY;
                summonMenu.setVisible(true);
                return true;
            }

        } else {
            // Clicked outside map
            clearMarkers();
            selectedUnitEntity = null;
            summonMenu.setVisible(false);
        }
        return true;
    }

    // --- HELPER LOGIC ---
    private void moveUnit(Entity unit, int targetX, int targetY) {
        // 1. Get the Position Component of the unit
        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);

        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));
        // 2. Update coordinates
        pos.x = targetX;
        pos.y = targetY;

        // 3. Cleanup
        clearMarkers();
        selectedUnitEntity = null; // Deselect after moving

        System.out.println("Unit moving to " + targetX + "," + targetY);
    }

    private void showMovementMarkers(int cx, int cy) {
        // 1. Get the Unit's Stats (so we know its Range and MoveType)
        if (selectedUnitEntity == null) {
            return;
        }
        StatsComponent stats = selectedUnitEntity.getComponent(StatsComponent.class);

        if (stats == null) {
            return; // Safety check
        }
        int range = stats.moveRange; // Use the unit's actual range!

        // 2. Loop through the range
        // This simple loop creates a square box of valid moves. 
        // For a diamond shape, add logic: Math.abs(x) + Math.abs(y) <= range
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                if (x == 0 && y == 0) {
                    continue; // Skip standing still
                }
                int targetX = cx + x;
                int targetY = cy + y;

                // 3. Check Bounds (Inside Map?)
                if (targetX >= 0 && targetX < MAP_WIDTH && targetY >= 0 && targetY < MAP_HEIGHT) {

                    // --- 4. TERRAIN CHECK (The Logic You Asked For) ---
                    if (isValidMove(targetX, targetY, stats.moveType)) {

                        // Check if another unit is blocking the way
                        if (getEntityAt(targetX, targetY, TypeComponent.Type.UNIT) == null) {
                            // Only spawn marker if valid!
                            // Note: You still use the generic EntityFactory for markers
                            entityFactory.createMovementMarker(targetX, targetY);
                        }
                    }
                }
            }
        }
    }

    private boolean isValidMove(int x, int y, StatsComponent.MoveType moveType) {
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];

        // RULE: LAND units cannot walk on WATER
        if (moveType == StatsComponent.MoveType.LAND) {
            if (terrain == MapGenerator.TerrainType.WATER
                    || terrain == MapGenerator.TerrainType.DEEP_WATER) {
                return false;
            }
        }

        // RULE: SEA units cannot walk on LAND (Future proofing)
        if (moveType == StatsComponent.MoveType.SEA) {
            if (terrain != MapGenerator.TerrainType.WATER
                    && terrain != MapGenerator.TerrainType.DEEP_WATER) {
                return false;
            }
        }

        return true;
    }

    private void clearMarkers() {
        // 1. Get all entities that have a TypeComponent
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(TypeComponent.class).get());

        // 2. Create a temporary list to hold the ones we want to kill
        List<Entity> toRemove = new ArrayList<>();

        // 3. Find the Markers
        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (type.type == TypeComponent.Type.MARKER) {
                toRemove.add(e); // Don't delete yet! Just remember it.
            }
        }

        // 4. NOW delete them safely
        for (Entity e : toRemove) {
            engine.removeEntity(e);
        }

        System.out.println("Markers cleared: " + toRemove.size());
    }

    private Entity getEntityAt(int x, int y, TypeComponent.Type targetType) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());

        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent type = e.getComponent(TypeComponent.class);

            if (pos.x == x && pos.y == y && type.type == targetType) {
                return e;
            }
        }
        return null;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float deltaX = lastTouchX - screenX;
        float deltaY = screenY - lastTouchY;
        camera.translate(deltaX * camera.zoom, deltaY * camera.zoom);
        lastTouchX = screenX;
        lastTouchY = screenY;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom += amountY * 0.1f;
        return true;
    }

    @Override
    public void show() {
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        hudStage.getViewport().update(width, height, true);
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
    }
}
