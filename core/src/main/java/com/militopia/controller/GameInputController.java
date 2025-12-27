package com.militopia.controller;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array; // Use LibGDX Array
import com.militopia.components.GridPositionComponent;
import com.militopia.components.MovementComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TextureComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.GameConfig;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.ui.GameHUD;

public class GameInputController extends InputAdapter {

    private final GameScreen screen;
    private final OrthographicCamera camera;
    private final PooledEngine engine;
    private final MapGenerator.GameMap gameMap;
    private final UnitFactory unitFactory;
    private final EntityFactory entityFactory;
    private final GameHUD gameHUD;

    // State Variables
    private int lastTouchX, lastTouchY;
    private int lastClickedX = -1, lastClickedY = -1;
    private Entity selectedUnitEntity = null;

    // BOUNCE VARIABLES
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;

    //Track Hover Position
    private int hoveredX = -1, hoveredY = -1;

    // Selection Cycling State
    private int selectionIndex = 0;

    // Add getters for the renderer to use
    public int getHoveredX() {
        return hoveredX;
    }

    public int getHoveredY() {
        return hoveredY;
    }

    public GameInputController(GameScreen screen, OrthographicCamera camera, PooledEngine engine,
            MapGenerator.GameMap gameMap, UnitFactory unitFactory,
            EntityFactory entityFactory, GameHUD gameHUD) {
        this.screen = screen;
        this.camera = camera;
        this.engine = engine;
        this.gameMap = gameMap;
        this.unitFactory = unitFactory;
        this.entityFactory = entityFactory;
        this.gameHUD = gameHUD;
    }

    // --- GETTERS ---
    public int getLastClickedX() {
        return lastClickedX;
    }

    public int getLastClickedY() {
        return lastClickedY;
    }

    public int getBouncingX() {
        return bouncingX;
    }

    public int getBouncingY() {
        return bouncingY;
    }

    public float getBounceTimer() {
        return bounceTimer;
    }

    public void resetLastClicked() {
        this.lastClickedX = -1;
        this.lastClickedY = -1;
    }

    // --- CRITICAL: Call this from GameScreen.render() ---
    public void update(float deltaTime) {
        if (bounceTimer > 0) {
            bounceTimer -= deltaTime;
            if (bounceTimer <= 0) {
                bouncingX = -1;
                bouncingY = -1;
            }
        }
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        camera.zoom += amountY * GameConfig.ZOOM_SPEED;
        camera.zoom = MathUtils.clamp(camera.zoom, GameConfig.ZOOM_MIN, GameConfig.ZOOM_MAX);
        camera.update();
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        lastTouchX = screenX;
        lastTouchY = screenY;

        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;

        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        System.out.println("Clicked Grid: " + gridX + ", " + gridY);

        if (gridX >= 0 && gridX < GameConfig.MAP_WIDTH && gridY >= 0 && gridY < GameConfig.MAP_HEIGHT) {

            // --- PRIORITY 0: MOVEMENT (Always Check First) ---
            // If we have a unit selected and we click a valid Move Marker, 
            // we MOVE. We do NOT cycle selection here.
            Entity clickedMarker = getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);
            if (clickedMarker != null && selectedUnitEntity != null) {
                moveUnit(selectedUnitEntity, gridX, gridY);
                return true;
            }

            // --- SELECTION CYCLING LOGIC ---
            // 1. Check if we clicked the SAME tile as before
            if (gridX == lastClickedX && gridY == lastClickedY) {
                selectionIndex++; // Go to next item
            } else {
                selectionIndex = 0; // New tile, start at top
            }

            // Update Global Last Clicked
            lastClickedX = gridX;
            lastClickedY = gridY;

            // 2. Build the Stack of Selectable Things (Top to Bottom)
            // Use a simple List or ArrayList to hold the "types" of things found
            java.util.List<String> selectionStack = new java.util.ArrayList<>();

            Entity foundUnit = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
            MapGenerator.ObjectType foundObject = gameMap.objects[gridX][gridY];
            boolean hasObject = (foundObject != MapGenerator.ObjectType.NONE);

            // LAYER 1: UNIT (Z-Index High)
            if (foundUnit != null) {
                selectionStack.add("UNIT");
            }

            // LAYER 2: OBJECT/BASE (Z-Index Medium)
            if (hasObject) {
                selectionStack.add("OBJECT");
            }

            // LAYER 3: TERRAIN (Z-Index Low - Always there)
            selectionStack.add("TERRAIN");

            // 3. Pick the Target based on Index
            // The modulo (%) operator makes it loop safely (0, 1, 2, 0, 1...)
            String targetType = selectionStack.get(selectionIndex % selectionStack.size());

            // 4. RESET UI STATE (Before selecting new thing)
            clearMarkers();
            // Don't nullify selectedUnitEntity if we are just clicking the same unit again,
            // but for safety, let's re-select it explicitly below.
            selectedUnitEntity = null;
            gameHUD.summonMenu.setVisible(false);
            triggerBounce(gridX, gridY);

            // 5. EXECUTE SELECTION
            if (targetType.equals("UNIT")) {
                handleUnitSelection(foundUnit, gridX, gridY);
            } else if (targetType.equals("OBJECT")) {
                handleObjectSelection(foundObject, gridX, gridY);
            } else if (targetType.equals("TERRAIN")) {
                handleTerrainSelection(gridX, gridY);
            }

        } else {
            // Clicked Outside
            clearMarkers();
            selectedUnitEntity = null;
            gameHUD.summonMenu.setVisible(false);
            gameHUD.hideTileInfo();
            lastClickedX = -1;
            lastClickedY = -1;
            selectionIndex = 0; // Reset
        }
        return true;
    }

    // 1. HANDLE UNIT SELECTION
    private void handleUnitSelection(Entity unit, int x, int y) {
        selectedUnitEntity = unit;
        showMovementMarkers(x, y);

        // Use Factory to get consistent Name and Icon
        // (Later you can get specific type from StatsComponent)
        UnitFactory.UiInfo info = unitFactory.getUnitUi("RECRUIT");

        gameHUD.showTileInfo(info.name, info.region);
        System.out.println("Selected: " + info.name);
    }

    // 2. HANDLE OBJECT SELECTION
    private void handleObjectSelection(MapGenerator.ObjectType obj, int x, int y) {
        // Use Factory lookup
        UnitFactory.UiInfo info = unitFactory.getObjectUi(obj);

        gameHUD.showTileInfo(info.name, info.region);

        // Logic for opening menu remains the same
        if (obj == MapGenerator.ObjectType.BASE_P1 || obj == MapGenerator.ObjectType.BASE_P2) {
            int owner = (obj == MapGenerator.ObjectType.BASE_P2) ? 2 : 1;
            gameHUD.openSummonMenu(owner);
        }

        System.out.println("Selected: " + info.name);
    }

    // 3. HANDLE TERRAIN SELECTION
    private void handleTerrainSelection(int x, int y) {
        MapGenerator.TerrainType type = gameMap.terrain[x][y];

        // Use Factory lookup
        UnitFactory.UiInfo info = unitFactory.getTerrainUi(type);

        gameHUD.showTileInfo(info.name, info.region);
        System.out.println("Selected: " + info.name);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // 1. Convert Screen -> Grid (Same math as touchDown)
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;

        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        // 2. Update Hover State
        if (gridX >= 0 && gridX < GameConfig.MAP_WIDTH && gridY >= 0 && gridY < GameConfig.MAP_HEIGHT) {
            this.hoveredX = gridX;
            this.hoveredY = gridY;
        } else {
            this.hoveredX = -1;
            this.hoveredY = -1;
        }
        return false; // Let other systems handle it if needed
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float x = Gdx.input.getDeltaX();
            float y = Gdx.input.getDeltaY();
            camera.translate(-x * camera.zoom * GameConfig.DRAG_SPEED, y * camera.zoom * GameConfig.DRAG_SPEED);
            camera.update();
            return true;
        }
        return false;
    }

    private void triggerBounce(int x, int y) {
        this.bouncingX = x;
        this.bouncingY = y;
        this.bounceTimer = GameConfig.BOUNCE_DURATION;
    }

    private void moveUnit(Entity unit, int targetX, int targetY) {
        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);

        // Add movement component for smooth sliding
        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));

        // Update logical position immediately so other units can't move here
        pos.x = targetX;
        pos.y = targetY;

        // CRITICAL: Clear markers immediately to prevent "Ghost Markers"
        clearMarkers();
        selectedUnitEntity = null;
    }

    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= GameConfig.MAP_WIDTH || y < 0 || y >= GameConfig.MAP_HEIGHT) {
            return false;
        }

        // Check Water
        if (gameMap.terrain[x][y] == MapGenerator.TerrainType.WATER
                || gameMap.terrain[x][y] == MapGenerator.TerrainType.DEEP_WATER) {
            return false;
        }

        // Check for Units
        if (getEntityAt(x, y, TypeComponent.Type.UNIT) != null) {
            return false;
        }

        return true;
    }

    private void showMovementMarkers(int startX, int startY) {
        StatsComponent stats = selectedUnitEntity.getComponent(StatsComponent.class);
        int moveRange = (stats != null) ? stats.moveRange : 3; // Ensure your Component uses 'movement' or 'moveRange' correctly

        boolean[][] visited = new boolean[GameConfig.MAP_WIDTH][GameConfig.MAP_HEIGHT];

        // Start Flood Fill
        floodFill(startX, startY, moveRange, visited, startX, startY);
    }

    private void floodFill(int x, int y, int remainingMoves, boolean[][] visited, int startX, int startY) {
        if (remainingMoves < 0) {
            return;
        }
        if (x < 0 || x >= GameConfig.MAP_WIDTH || y < 0 || y >= GameConfig.MAP_HEIGHT) {
            return;
        }

        // If we already visited this tile with MORE or EQUAL moves, skip it.
        // (This simple boolean check is okay for uniform cost, but if diagonals cost more, you'd need a cost array)
        if (visited[x][y]) {
            return;
        }

        // Is this the unit's starting tile?
        boolean isStart = (x == startX && y == startY);

        // Check Walkability (Skip check for start tile so we can "expand" out of it)
        if (!isStart && !isWalkable(x, y)) {
            return;
        }

        // Mark as visited
        visited[x][y] = true;

        // Spawn Marker (Don't put a marker under the unit itself)
        if (!isStart) {
            entityFactory.createMovementMarker(x, y);
        }

        // --- RECURSE: CARDINAL DIRECTIONS (4) ---
        floodFill(x + 1, y, remainingMoves - 1, visited, startX, startY); // Right
        floodFill(x - 1, y, remainingMoves - 1, visited, startX, startY); // Left
        floodFill(x, y + 1, remainingMoves - 1, visited, startX, startY); // Up
        floodFill(x, y - 1, remainingMoves - 1, visited, startX, startY); // Down

        // --- RECURSE: DIAGONAL DIRECTIONS (4) - ADD THESE! ---
        floodFill(x + 1, y + 1, remainingMoves - 1, visited, startX, startY); // Top-Right
        floodFill(x - 1, y + 1, remainingMoves - 1, visited, startX, startY); // Top-Left
        floodFill(x + 1, y - 1, remainingMoves - 1, visited, startX, startY); // Bottom-Right
        floodFill(x - 1, y - 1, remainingMoves - 1, visited, startX, startY); // Bottom-Left
    }

    private void clearMarkers() {
        // SAFE REMOVAL: Copy entities to a list first
        ImmutableArray<Entity> markers = engine.getEntitiesFor(Family.all(TypeComponent.class).get());
        Array<Entity> toRemove = new Array<>();

        for (Entity e : markers) {
            if (e.getComponent(TypeComponent.class).type == TypeComponent.Type.MARKER) {
                toRemove.add(e);
            }
        }

        for (Entity e : toRemove) {
            engine.removeEntity(e);
        }
    }

    private Entity getEntityAt(int x, int y, TypeComponent.Type type) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && t.type == type) {
                return e;
            }
        }
        return null;
    }
}
