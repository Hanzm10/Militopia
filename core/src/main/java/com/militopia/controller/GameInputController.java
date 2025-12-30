package com.militopia.controller;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.MovementComponent;
import com.militopia.components.StatsComponent;
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

    private int lastTouchX, lastTouchY;
    private int lastClickedX = -1, lastClickedY = -1;
    private Entity selectedUnitEntity = null;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;
    private int hoveredX = -1, hoveredY = -1;
    private int selectionIndex = 0;

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

    public int getHoveredX() {
        return hoveredX;
    }

    public int getHoveredY() {
        return hoveredY;
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

    public int getLastClickedX() {
        return lastClickedX;
    }

    public int getLastClickedY() {
        return lastClickedY;
    }

    public void resetLastClicked() {
        this.lastClickedX = -1;
        this.lastClickedY = -1;
    }

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

        if (gridX >= 0 && gridX < GameConfig.MAP_WIDTH && gridY >= 0 && gridY < GameConfig.MAP_HEIGHT) {

            // Check for marker first
            Entity clickedMarker = getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);
            boolean isVisible = gameMap.visibleTiles[gridX][gridY];

            // --- FOG OF WAR CHECK ---
            // If Fog is ON and the tile is NOT visible...
            if (screen.isFogEnabled() && !isVisible) {
                // FIX: If there is a marker here (move target), ALLOW THE CLICK.
                if (clickedMarker != null) {
                    // Do nothing here, let it fall through to the movement logic below
                } else {
                    // Otherwise, block interaction with hidden tiles
                    clearMarkers();
                    selectedUnitEntity = null;
                    gameHUD.hideSummonMenu();
                    gameHUD.hideTileInfo();
                    lastClickedX = -1;
                    lastClickedY = -1;
                    return true;
                }
            }
            // ------------------------

            if (clickedMarker != null && selectedUnitEntity != null) {
                moveUnit(selectedUnitEntity, gridX, gridY);
                return true;
            }

            if (gridX == lastClickedX && gridY == lastClickedY) {
                selectionIndex++;
            } else {
                selectionIndex = 0;
            }
            lastClickedX = gridX;
            lastClickedY = gridY;

            java.util.List<String> selectionStack = new java.util.ArrayList<>();
            Entity foundUnit = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
            MapGenerator.ObjectType foundObject = gameMap.objects[gridX][gridY];
            boolean hasObject = (foundObject != MapGenerator.ObjectType.NONE);

            if (foundUnit != null) {
                selectionStack.add("UNIT");
            }
            if (hasObject) {
                selectionStack.add("OBJECT");
            }
            selectionStack.add("TERRAIN");

            String targetType = selectionStack.get(selectionIndex % selectionStack.size());

            clearMarkers();
            selectedUnitEntity = null;
            gameHUD.hideSummonMenu();
            triggerBounce(gridX, gridY);

            if (targetType.equals("UNIT")) {
                handleUnitSelection(foundUnit, gridX, gridY);
            } else if (targetType.equals("OBJECT")) {
                handleObjectSelection(foundObject, gridX, gridY);
            } else if (targetType.equals("TERRAIN")) {
                handleTerrainSelection(gridX, gridY);
            }

        } else {
            clearMarkers();
            selectedUnitEntity = null;
            gameHUD.hideTileInfo();
            gameHUD.hideSummonMenu();
            lastClickedX = -1;
            lastClickedY = -1;
            selectionIndex = 0;
        }
        return true;
    }

    private void handleUnitSelection(Entity unit, int x, int y) {
        selectedUnitEntity = unit;
        showMovementMarkers(x, y);
        UnitFactory.UiInfo info = unitFactory.getUnitUi("RECRUIT");
        gameHUD.showTileInfo(info.name, info.region);
    }

    private void handleObjectSelection(MapGenerator.ObjectType obj, int x, int y) {
        // 1. CHECK FOR BASE (Summon Menu)
        if (obj == MapGenerator.ObjectType.BASE_P1 || obj == MapGenerator.ObjectType.BASE_P2) {
            int owner = (obj == MapGenerator.ObjectType.BASE_P2) ? 2 : 1;
            gameHUD.openSummonMenu(owner);
            System.out.println("Selected: Base (Opening Summon Menu)");
            return; // <--- CRITICAL FIX: Stop here! Don't show Tile Info.
        }

        // 2. STANDARD OBJECT INFO
        UnitFactory.UiInfo info = unitFactory.getObjectUi(obj);
        gameHUD.showTileInfo(info.name, info.region);
        System.out.println("Selected: " + info.name);
    }

    private void handleTerrainSelection(int x, int y) {
        MapGenerator.TerrainType type = gameMap.terrain[x][y];
        UnitFactory.UiInfo info = unitFactory.getTerrainUi(type);
        gameHUD.showTileInfo(info.name, info.region);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        if (gridX >= 0 && gridX < GameConfig.MAP_WIDTH && gridY >= 0 && gridY < GameConfig.MAP_HEIGHT) {
            this.hoveredX = gridX;
            this.hoveredY = gridY;
        } else {
            this.hoveredX = -1;
            this.hoveredY = -1;
        }
        return false;
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
        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));
        pos.x = targetX;
        pos.y = targetY;
        clearMarkers();
        selectedUnitEntity = null;
    }

    private boolean isWalkable(int x, int y) {
        if (x < 0 || x >= GameConfig.MAP_WIDTH || y < 0 || y >= GameConfig.MAP_HEIGHT) {
            return false;
        }
        if (gameMap.terrain[x][y] == MapGenerator.TerrainType.WATER || gameMap.terrain[x][y] == MapGenerator.TerrainType.DEEP_WATER) {
            return false;
        }
        if (getEntityAt(x, y, TypeComponent.Type.UNIT) != null) {
            return false;
        }
        return true;
    }

    private void showMovementMarkers(int startX, int startY) {
        StatsComponent stats = selectedUnitEntity.getComponent(StatsComponent.class);
        int moveRange = (stats != null) ? stats.moveRange : 3;

        // FIX: Use an int array to track "Max Fuel Left" at each tile.
        // Initialize with -1 because 0 is a valid "moves left" state.
        int[][] visitedMoves = new int[GameConfig.MAP_WIDTH][GameConfig.MAP_HEIGHT];
        for (int i = 0; i < GameConfig.MAP_WIDTH; i++) {
            for (int j = 0; j < GameConfig.MAP_HEIGHT; j++) {
                visitedMoves[i][j] = -1;
            }
        }

        floodFill(startX, startY, moveRange, visitedMoves, startX, startY);
    }

    private void floodFill(int x, int y, int remainingMoves, int[][] visitedMoves, int startX, int startY) {
        // 1. Basic Validation
        if (remainingMoves < 0) {
            return;
        }
        if (x < 0 || x >= GameConfig.MAP_WIDTH || y < 0 || y >= GameConfig.MAP_HEIGHT) {
            return;
        }

        // 2. OPTIMIZATION CHECK (The Fix)
        // If we have already reached this tile with MORE (or equal) energy, 
        // this current path is worse/redundant. Stop.
        if (visitedMoves[x][y] >= remainingMoves) {
            return;
        }

        // 3. Walkability Check
        boolean isStart = (x == startX && y == startY);
        if (!isStart && !isWalkable(x, y)) {
            return;
        }

        // 4. Record State
        // We found a better (or first) path to this tile. Record how much fuel we have.
        visitedMoves[x][y] = remainingMoves;

        // 5. Spawn Marker
        // Only spawn if it's not the start node AND we haven't already put a marker here 
        // (Check if entity exists to avoid duplicates, or just rely on clearMarkers clearing them later)
        // Note: Since we might visit a tile multiple times with better paths, avoid spawning duplicate entities.
        if (!isStart) {
            // Optimization: Only spawn if marker doesn't exist yet
            if (getEntityAt(x, y, TypeComponent.Type.MARKER) == null) {
                entityFactory.createMovementMarker(x, y);
            }
        }

        // 6. Recurse (Cost is 1 per tile)
        int nextMove = remainingMoves - 1;

        // Cardinal
        floodFill(x + 1, y, nextMove, visitedMoves, startX, startY);
        floodFill(x - 1, y, nextMove, visitedMoves, startX, startY);
        floodFill(x, y + 1, nextMove, visitedMoves, startX, startY);
        floodFill(x, y - 1, nextMove, visitedMoves, startX, startY);

        // Diagonal
        floodFill(x + 1, y + 1, nextMove, visitedMoves, startX, startY);
        floodFill(x - 1, y + 1, nextMove, visitedMoves, startX, startY);
        floodFill(x + 1, y - 1, nextMove, visitedMoves, startX, startY);
        floodFill(x - 1, y - 1, nextMove, visitedMoves, startX, startY);
    }

    private void clearMarkers() {
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
