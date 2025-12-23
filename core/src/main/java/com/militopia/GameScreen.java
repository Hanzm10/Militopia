package com.militopia;

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
import com.badlogic.gdx.InputAdapter; // Import this!
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

public class GameScreen extends InputAdapter implements Screen { // Extend InputAdapter

    final MilitopiaGame game;
    MapGenerator.GameMap gameMap;
    OrthographicCamera camera;

    // UI (HUD)
    Stage hudStage;

    // Logic Vars
    long seed;
    String p1Name, p2Name, saveName;

    // Grid settings (Same as before)
    final int MAP_WIDTH = 32;
    final int MAP_HEIGHT = 32;
    final int TILE_WIDTH = 28;
    final int TILE_HEIGHT = 18;
    final float DRAW_WIDTH = 64f;
    final float DRAW_HEIGHT = 40f;

    // Input Vars
    float lastTouchX, lastTouchY;
    int selectedX = -1, selectedY = -1;

    public GameScreen(final MilitopiaGame game, long seed, String p1, String p2, String saveName) {
        this.game = game;
        this.seed = seed;
        this.p1Name = p1;
        this.p2Name = p2;
        // Create a filename based on P1 vs P2 (e.g., "P1_vs_P2.json")
        this.saveName = saveName;

        // 1. Setup Camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        camera.position.set(0, 0, 0);
        camera.update();

        // 2. Setup HUD (The Save Button)
        hudStage = new Stage(new ScreenViewport());
        setupHUD();

        // 3. Setup Input (Handle BOTH Map clicks and Button clicks)
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(hudStage); // UI gets clicks first
        multiplexer.addProcessor(this);     // Map gets clicks second
        Gdx.input.setInputProcessor(multiplexer);

        // 4. Generate Map
        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(MAP_WIDTH, MAP_HEIGHT, seed);
    }

    private void setupHUD() {
        TextButton saveBtn = new TextButton("Save & Exit", game.skin);
        saveBtn.setPosition(20, Gdx.graphics.getHeight() - 50); // Top Left
        saveBtn.setSize(120, 40);

        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                saveGame();
                game.setScreen(new MenuScreen(game)); // Go back to menu
            }
        });

        hudStage.addActor(saveBtn);
    }

    private void saveGame() {
        // Create the Data Object
        GameState state = new GameState(seed, p1Name, p2Name, saveName);

        // Convert to JSON
        Json json = new Json();
        String text = json.toJson(state);

        // Write to File (In a "saves" folder)
        FileHandle file = Gdx.files.local("saves/" + saveName + ".json");
        file.writeString(text, false);

        System.out.println("Game Saved to: " + file.path());
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
        renderMapLoop(); 
        game.batch.end();

        // 2. Draw UI on top
        hudStage.act();
        hudStage.draw();
    }

    // Copy your existing loop logic here
    private void renderMapLoop() {
        game.batch.setColor(Color.WHITE);

        for (int x = MAP_WIDTH - 1; x >= 0; x--) {
            for (int y = MAP_HEIGHT - 1; y >= 0; y--) {

                float isoX = (x - y) * (TILE_WIDTH / 2.0f);
                float isoY = (x + y) * (TILE_HEIGHT / 2.0f);

                // --- DRAWING MATH (Uses the flexible DRAW size) ---
                // We calculate an offset to keep the image centered as we change its size
                float xOffset = (DRAW_WIDTH - TILE_WIDTH) / 2;
                float yOffset = (DRAW_HEIGHT - TILE_HEIGHT) / 2;

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
                    game.batch.draw(t, isoX - xOffset, isoY - yOffset, DRAW_WIDTH, DRAW_HEIGHT);
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
                    game.batch.draw(o, isoX, isoY + 12, TILE_WIDTH, TILE_HEIGHT);
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
        camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 3.0f);
    }

    // ... (Keep updateHoveredTile, touchDown, touchDragged, etc.) ...
    private void updateHoveredTile() {
        // 1. Get Mouse pixels
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();

        // 2. Unproject (Account for Camera Zoom and Position)
        Vector3 worldCoords = camera.unproject(new Vector3(mouseX, mouseY, 0));

        // 3. Isometric Math Inversion
        // We reverse the (x-y) and (x+y) logic
        float halfW = TILE_WIDTH / 2.0f;
        float halfH = TILE_HEIGHT / 2.0f;

        // Adjust worldY because textures draw from bottom-left
        float adjustedY = worldCoords.y;

        // Formula derived from solving the system of equations
        int gridX = MathUtils.floor((adjustedY / halfH + worldCoords.x / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - worldCoords.x / halfW) / 2);

        // 4. Bounds Check (Am I actually on the map?)
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
        lastTouchX = screenX;
        lastTouchY = screenY;
        if (selectedX != -1) {
            System.out.println("CLICKED TILE: " + selectedX + ", " + selectedY);
        }
        return true;
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
