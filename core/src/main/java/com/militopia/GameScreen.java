package com.militopia;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameScreen extends InputAdapter implements Screen {

    final MilitopiaGame game;
    Texture tileTexture;
    Texture highlightTexture; // New: To show selection
    OrthographicCamera camera;

    // Grid Settings
    final int MAP_WIDTH = 30;
    final int MAP_HEIGHT = 30;
    final int TILE_WIDTH = 32;
    final int TILE_HEIGHT = 16;

    final float DRAW_WIDTH = 70f;
    final float DRAW_HEIGHT = 38f;

    // Input Variables
    float lastTouchX, lastTouchY;

    // Selection Variables
    int selectedX = -1;
    int selectedY = -1;

    //to store map data
    MapGenerator.GameMap gameMap; // Replace the old TerrainType[][] array

    //user input for new game
    long seed;
    String p1Name;
    String p2Name;

    public GameScreen(final MilitopiaGame game, long seed, String p1, String p2) {
        this.game = game;
        this.seed = seed;
        this.p1Name = p1;
        this.p2Name = p2;

        tileTexture = new Texture("square.png");
        highlightTexture = new Texture("square.png"); // Re-using square for now

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        // Center camera roughly on the map
        camera.position.set(0, 0, 0);
        camera.zoom = 1.0f;
        camera.update();

        Gdx.input.setInputProcessor(this);
        MapGenerator generator = new MapGenerator();
        gameMap = generator.generateMap(MAP_WIDTH, MAP_HEIGHT, System.currentTimeMillis());

        System.out.println("Started match: " + p1 + " vs " + p2);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.4f, 0.7f, 1.0f, 1);
        handleInput(delta);
        updateHoveredTile();

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.setColor(Color.WHITE);

        for (int x = MAP_WIDTH - 1; x >= 0; x--) {
            for (int y = MAP_HEIGHT - 1; y >= 0; y--) {

                float isoX = (x - y) * (TILE_WIDTH / 2.0f);
                float isoY = (x + y) * (TILE_HEIGHT / 2.0f);

                // --- DRAWING MATH (Uses the flexible DRAW size) ---
                // We calculate an offset to keep the image centered as we change its size
                float xOffset = (DRAW_WIDTH - TILE_WIDTH) / 2;
                float yOffset = (DRAW_HEIGHT - TILE_HEIGHT) / 2;

//                // --- 1. DRAW TERRAIN (The Missing Part) ---
//                MapGenerator.TerrainType type = gameMap.terrain[x][y];
//
//                switch (type) {
//                    case DEEP_WATER:
//                        game.batch.setColor(0, 0, 0.5f, 1);
//                        break; // Dark Blue
//                    case WATER:
//                        game.batch.setColor(0, 0, 1, 1);
//                        break;    // Blue
//                    case SAND:
//                        game.batch.setColor(1, 1, 0, 1);
//                        break;    // Yellow
//                    case GRASS:
//                        game.batch.setColor(0, 1, 0, 1);
//                        break;    // Green
//                    case FOREST:
//                        game.batch.setColor(0, 0.5f, 0, 1);
//                        break; // Dark Green
//                }
//
//                // Draw the base tile with the specific biome color
//                game.batch.draw(tileTexture, isoX, isoY, TILE_WIDTH, TILE_WIDTH);
//
//                // --- 2. DRAW SELECTION HIGHLIGHT ---
//                if (x == selectedX && y == selectedY) {
//                    game.batch.setColor(1, 1, 1, 0.5f);
//                    game.batch.draw(highlightTexture, isoX, isoY, TILE_WIDTH, TILE_WIDTH);
//                }
//
//                // --- 3. DRAW OBJECTS (On top of terrain) ---
//                MapGenerator.ObjectType obj = gameMap.objects[x][y];
//                if (obj != MapGenerator.ObjectType.NONE) {
//
//                    switch (obj) {
//                        case BASE_P1:
//                            game.batch.setColor(Color.SALMON);
//                            break;
//                        case BASE_P2:
//                            game.batch.setColor(Color.RED);
//                            break;
//                        case BASE_NEUTRAL:
//                            game.batch.setColor(Color.GRAY);
//                            break;
//                        case OIL:
//                            game.batch.setColor(Color.BLACK);
//                            break;
//                        case RUINS:
//                            game.batch.setColor(Color.PURPLE);
//                            break;
//                        case CACTUS:
//                            game.batch.setColor(Color.OLIVE);
//                            break;
//                        case TREE:
//                            game.batch.setColor(Color.BROWN);
//                            break;
//                    }
//
//                    // Draw the object slightly smaller than the tile
//                    float objSize = TILE_WIDTH * 1.2f;
//                    float objOffX = (TILE_WIDTH - objSize) / 2;
//                    float objOffY = (TILE_WIDTH - objSize) / 2;
//
//                    game.batch.draw(tileTexture, isoX + objOffX, isoY + objOffY, objSize, objSize);
//                }
//            }
//        }
//
//        game.batch.setColor(Color.WHITE); // Reset color at the end
//        game.batch.end();
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
                    // ... etc ...
                }

                if (o != null) {
                    // Draw object centered on the tile
                    // Adjust offsets based on your image size! 
                    // Usually objects need to be drawn slightly higher (y + 8) to look like they sit ON the tile.
                    game.batch.draw(o, isoX, isoY+13, TILE_WIDTH*2, TILE_HEIGHT*2);
                }
            }
        }
        game.batch.end();
    }

    /**
     * * THE MAGIC MATH: Converts Mouse Pixels -> Camera World -> Isometric
     * Grid
     */
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

    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom += 0.02f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            camera.zoom -= 0.02f;
        }
        camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 3.0f);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            MapGenerator generator = new MapGenerator();
            // Generate new map with random seed
            gameMap = generator.generateMap(MAP_WIDTH, MAP_HEIGHT, System.currentTimeMillis());
            System.out.println("New Map Generated!");
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
        tileTexture.dispose();
        highlightTexture.dispose();
    }
}
