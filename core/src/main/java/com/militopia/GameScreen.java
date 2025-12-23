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
import com.militopia.components.TextureComponent;
import com.militopia.components.TypeComponent;
import java.util.ArrayList;
import java.util.List;

public class GameScreen extends InputAdapter implements Screen { // Extend InputAdapter

    final MilitopiaGame game;
    MapGenerator.GameMap gameMap;
    OrthographicCamera camera;

    PooledEngine engine;
    EntityFactory factory;

    // UI (HUD)
    Stage hudStage;
    Table summonMenu;

    // Logic Vars
    long seed;
    String p1Name, p2Name, saveName;

    // Grid settings (Same as before)
    final int MAP_WIDTH = 32;
    final int MAP_HEIGHT = 32;
    final int TILE_WIDTH = 27;
    final int TILE_HEIGHT = 17;
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
        factory = new EntityFactory(engine, game);

        setupHUD();

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
                    factory.createRecruit(u.x, u.y);
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
                    factory.createRecruit(lastClickedX, lastClickedY);

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

    // Copy your existing loop logic here
    private void renderMapLoop(float delta) {
        //  UPDATE BOUNCE TIMER
        if (bouncingX != -1) {
            bounceTimer += delta;
            if (bounceTimer >= BOUNCE_DURATION) {
                bouncingX = -1; // Animation finished
                bouncingY = -1;
            }
        }
        game.batch.setColor(Color.WHITE);

        for (int x = MAP_WIDTH - 1; x >= 0; x--) {
            for (int y = MAP_HEIGHT - 1; y >= 0; y--) {

                float isoX = (x - y) * (TILE_WIDTH / 2.0f);
                float isoY = (x + y) * (TILE_HEIGHT / 2.0f);

                // --- DRAWING MATH (Uses the flexible DRAW size) ---
                // We calculate an offset to keep the image centered as we change its size
                float xOffset = (DRAW_WIDTH - TILE_WIDTH) / 2;
                float yOffset = (DRAW_HEIGHT - TILE_HEIGHT) / 2;

                // --- CALCULATE BOUNCE OFFSET ---
                float animY = 0;
                if (x == bouncingX && y == bouncingY) {
                    // Normalize time from 0.0 to 1.0
                    float progress = bounceTimer / BOUNCE_DURATION;
                    // Math.sin(PI * progress) creates a curve: 0 -> 1 -> 0
                    animY = (float) Math.sin(progress * Math.PI) * BOUNCE_HEIGHT;
                }

                // 1. PICK THE TERRAIN TEXTURE
                Texture t = null;
                switch (gameMap.terrain[x][y]) {
                    case GRASS:
                        t = game.texGrass;
                        break;
                    case WATER:
                        t = game.texWater;
                        break;
                    case DEEP_WATER:
                        t = game.texDeepWater;
                        break;
                    case SAND:
                        t = game.texSand;
                        break;
                    case FOREST:
                        t = game.texForest;
                        break;
                }

                // 2. DRAW THE TILE
                // Note: We might need to adjust y slightly if the image is tall (like a block)
                if (t != null) {
                    // Draw using the DRAW variables, shifted by the offset
                    game.batch.draw(t, isoX - xOffset, isoY - yOffset + animY, DRAW_WIDTH, DRAW_HEIGHT);
                }

                // 3. DRAW OBJECTS (Layered on top)
                Texture o = null;
                switch (gameMap.objects[x][y]) {
                    case BASE_P1:
                        o = game.texBaseP1;
                        break;
                    case BASE_P2:
                        o = game.texBaseP2;
                        break;
                    case BASE_NEUTRAL:
                        o = game.texBaseNeutral;
                        break;
                    case TREE:
                        o = game.texTree;
                        break;
                    case RUINS:
                        o = game.texRuins;
                        break;
                    case OIL:
                        o = game.texOil;
                        break;
                    case CACTUS:
                        o = game.texCactus;
                        break;

                }

                if (o != null) {
                    // Draw object centered on the tile
                    // Adjust offsets based on your image size! 
                    // Usually objects need to be drawn slightly higher (y + 8) to look like they sit ON the tile.
                    game.batch.draw(o, isoX - 2, isoY + 7 + animY, DRAW_WIDTH, DRAW_HEIGHT);
                }

                // --- DRAW "WHITEN" EFFECT (HOVER) ---
                if (x == selectedX && y == selectedY) {
                    // 1. Change Blend Mode to "ADDITIVE" (This adds brightness)
                    // GL_ONE means: "Add the pixel color to what's already on screen"
                    Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
                    game.batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE);

                    // 2. Set the "Brightness" power
                    // Dark Gray = Low brightness added. White = Maximum brightness added.
                    game.batch.setColor(0.4f, 0.4f, 0.4f, 1f);

                    // 3. Draw the SAME texture we just drew for the terrain (t)
                    // Since 't' is the grass/sand texture, the highlight fits perfectly!
                    if (t != null) {
                        game.batch.draw(t, isoX - xOffset, isoY - yOffset + animY, DRAW_WIDTH, DRAW_HEIGHT);
                    }

                    // 4. RESET Blend Mode to Normal (Crucial!)
                    game.batch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
                    game.batch.setColor(Color.WHITE);
                }
            }
        }

        // DRAW ECS ENTITIES (Units & Markers) ON TOP
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TextureComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TextureComponent tex = e.getComponent(TextureComponent.class);

            // Calculate ISO position for this entity
            float isoX = (pos.x - pos.y) * (TILE_WIDTH / 2.0f);
            float isoY = (pos.x + pos.y) * (TILE_HEIGHT / 2.0f);

            // Adjust offsets
            float xOffset = (DRAW_WIDTH - TILE_WIDTH) / 2f;
            float yOffset = (DRAW_HEIGHT - TILE_HEIGHT) / 2f;

            // DRAW
            game.batch.draw(tex.region, isoX - xOffset, isoY - yOffset + 12, DRAW_WIDTH, DRAW_HEIGHT);
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
        // 1. UPDATE LAST TOUCH (For camera dragging)
        lastTouchX = screenX;
        lastTouchY = screenY;

        // 2. CONVERT MOUSE CLICKS TO GRID COORDINATES (The "Math")
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));

        // Height correction: Subtract ~12 pixels from Y to account for the "dirt" block height
        // This fixes the issue where you click the roof but select the tile behind it
        float heightOffset = 12f;
        float adjustedY = worldCoords.y - heightOffset;

        float halfW = TILE_WIDTH / 2.0f;
        float halfH = TILE_HEIGHT / 2.0f;

        // Solve for Isometric X and Y
        int gridX = MathUtils.floor((adjustedY / halfH + worldCoords.x / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - worldCoords.x / halfW) / 2);

        // 3. BOUNDS CHECK (Are we inside the map?)
        if (gridX >= 0 && gridX < MAP_WIDTH && gridY >= 0 && gridY < MAP_HEIGHT) {
            selectedX = gridX;
            selectedY = gridY;

            // --- PRIORITY 1: DID WE CLICK A MOVEMENT MARKER? ---
            // (This means we are moving the unit we selected previously)
            Entity clickedMarker = getEntityAt(selectedX, selectedY, TypeComponent.Type.MARKER);

            if (clickedMarker != null && selectedUnitEntity != null) {
                System.out.println("Moving Unit to " + selectedX + "," + selectedY);
                moveUnit(selectedUnitEntity, selectedX, selectedY);
                return true;
            }

            // --- RESET STATE ---
            // If we didn't click a marker, we are starting a new action.
            clearMarkers();
            selectedUnitEntity = null; // Deselect old unit
            summonMenu.setVisible(false);

            // --- PRIORITY 2: DID WE CLICK A UNIT? ---
            Entity clickedUnit = getEntityAt(selectedX, selectedY, TypeComponent.Type.UNIT);

            if (clickedUnit != null) {
                System.out.println("Unit Selected!");
                selectedUnitEntity = clickedUnit; // REMEMBER THIS UNIT!
                showMovementMarkers(selectedX, selectedY);
                return true;
            }

            // --- PRIORITY 3: DID WE CLICK A BASE? ---
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

        // 2. Update coordinates
        pos.x = targetX;
        pos.y = targetY;

        // 3. Cleanup
        clearMarkers();
        selectedUnitEntity = null; // Deselect after moving

        System.out.println("Unit moved successfully.");
    }

    private void showMovementMarkers(int cx, int cy) {
        // Loop through all 8 neighbors
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0) {
                    continue; // Skip self
                }
                int targetX = cx + x;
                int targetY = cy + y;

                // Bounds check
                if (targetX >= 0 && targetX < MAP_WIDTH && targetY >= 0 && targetY < MAP_HEIGHT) {
                    factory.createMovementMarker(targetX, targetY);
                }
            }
        }
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
