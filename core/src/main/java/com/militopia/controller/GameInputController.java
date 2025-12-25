package com.militopia.controller;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.militopia.config.GameConfig;
import com.militopia.screen.GameScreen;
import com.militopia.map.MapGenerator;
import com.militopia.factories.UnitFactory;
import com.militopia.factories.EntityFactory;
import com.militopia.components.*;

import java.util.ArrayList;
import java.util.List;

public class GameInputController extends InputAdapter {

    // --- References to the Game World ---
    private final GameScreen screen; // To update state (selectedX, bouncingX, etc.)
    private final OrthographicCamera camera;
    private final PooledEngine engine;
    private final MapGenerator.GameMap gameMap;
    private final UnitFactory unitFactory;
    private final EntityFactory entityFactory;
    private final Table summonMenu; // To toggle visibility

    // --- Internal State ---
    private float lastTouchX, lastTouchY;
    private Entity selectedUnitEntity = null;
    private int lastClickedX = -1;
    private int lastClickedY = -1;

    public GameInputController(GameScreen screen, OrthographicCamera camera, PooledEngine engine,
            MapGenerator.GameMap gameMap, UnitFactory unitFactory,
            EntityFactory entityFactory, Table summonMenu) {
        this.screen = screen;
        this.camera = camera;
        this.engine = engine;
        this.gameMap = gameMap;
        this.unitFactory = unitFactory;
        this.entityFactory = entityFactory;
        this.summonMenu = summonMenu;
    }

    // ========================================================================
    //                         MAIN INPUT METHODS
    // ========================================================================
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        lastTouchX = screenX;
        lastTouchY = screenY;

        // 1. Convert Screen -> Grid
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;

        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        // 2. Update GameScreen Selection State (For rendering)
        if (gridX >= 0 && gridX < GameConfig.MAP_WIDTH && gridY >= 0 && gridY < GameConfig.MAP_HEIGHT) {
            screen.updateSelection(gridX, gridY); // Helper we will make in GameScreen
            screen.triggerBounce(gridX, gridY);   // Helper we will make in GameScreen

            // --- PRIORITY 1: CLICKED MARKER? (Move Unit) ---
            Entity clickedMarker = getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);
            if (clickedMarker != null && selectedUnitEntity != null) {
                moveUnit(selectedUnitEntity, gridX, gridY);
                return true;
            }

            // --- RESET STATE ---
            clearMarkers();
            selectedUnitEntity = null;
            summonMenu.setVisible(false);

            // --- PRIORITY 2: CLICKED UNIT? (Select Unit) ---
            Entity clickedUnit = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
            if (clickedUnit != null) {
                selectedUnitEntity = clickedUnit;
                showMovementMarkers(gridX, gridY);
                return true;
            }

            // --- PRIORITY 3: CLICKED BASE? (Summon Menu) ---
            MapGenerator.ObjectType obj = gameMap.objects[gridX][gridY];

            // Allow clicking BOTH P1 and P2 bases
            if (obj == MapGenerator.ObjectType.BASE_P1 || obj == MapGenerator.ObjectType.BASE_P2) {
                lastClickedX = gridX;
                lastClickedY = gridY;
                summonMenu.setVisible(true); // Open the menu
                return true;
            }

        } else {
            // Clicked Outside
            screen.updateSelection(-1, -1);
            clearMarkers();
            selectedUnitEntity = null;
            summonMenu.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // 1. Copy the same Math logic from touchDown
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;

        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;

        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        // 2. Update the Screen Selection (This triggers the whitening)
        if (gridX >= 0 && gridX < GameConfig.MAP_WIDTH && gridY >= 0 && gridY < GameConfig.MAP_HEIGHT) {
            screen.updateSelection(gridX, gridY);
        } else {
            // Mouse is outside the map, deselect
            screen.updateSelection(-1, -1);
        }

        return false; // Return false to let other listeners handle it if needed
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
        camera.zoom += amountY * GameConfig.ZOOM_SPEED;
        camera.zoom = MathUtils.clamp(camera.zoom, GameConfig.ZOOM_MIN, GameConfig.ZOOM_MAX);
        return true;
    }

    public void updateHoveredTile(int screenX, int screenY) {
        // You can keep this logic here if you want mouse-over highlights,
        // or just let touchDown handle selection. 
        // If used, call screen.updateSelection(gridX, gridY).
    }

    // ========================================================================
    //                         HELPER LOGIC
    // ========================================================================
    private void moveUnit(Entity unit, int targetX, int targetY) {
        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);

        // Add Movement Component (System handles the animation)
        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));

        // Update logical position
        pos.x = targetX;
        pos.y = targetY;

        clearMarkers();
        selectedUnitEntity = null;
    }

    private void showMovementMarkers(int cx, int cy) {
        if (selectedUnitEntity == null) {
            return;
        }
        StatsComponent stats = selectedUnitEntity.getComponent(StatsComponent.class);
        if (stats == null) {
            return;
        }

        int range = stats.moveRange;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                if (x == 0 && y == 0) {
                    continue;
                }

                int targetX = cx + x;
                int targetY = cy + y;

                if (targetX >= 0 && targetX < GameConfig.MAP_WIDTH
                        && targetY >= 0 && targetY < GameConfig.MAP_HEIGHT) {

                    if (isValidMove(targetX, targetY, stats.moveType)) {
                        if (getEntityAt(targetX, targetY, TypeComponent.Type.UNIT) == null) {
                            entityFactory.createMovementMarker(targetX, targetY);
                        }
                    }
                }
            }
        }
    }

    private boolean isValidMove(int x, int y, StatsComponent.MoveType moveType) {
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];

        if (moveType == StatsComponent.MoveType.LAND) {
            return terrain != MapGenerator.TerrainType.WATER
                    && terrain != MapGenerator.TerrainType.DEEP_WATER;
        }
        if (moveType == StatsComponent.MoveType.SEA) {
            return terrain == MapGenerator.TerrainType.WATER
                    || terrain == MapGenerator.TerrainType.DEEP_WATER;
        }
        return true;
    }

    private void clearMarkers() {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(TypeComponent.class).get());
        List<Entity> toRemove = new ArrayList<>();
        for (Entity e : entities) {
            TypeComponent type = e.getComponent(TypeComponent.class);
            if (type.type == TypeComponent.Type.MARKER) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove) {
            engine.removeEntity(e);
        }
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

    // Getter for the summon logic (GameScreen needs this for the button listener)
    public int getLastClickedX() {
        return lastClickedX;
    }

    public int getLastClickedY() {
        return lastClickedY;
    }

    public void resetLastClicked() {
        lastClickedX = -1;
        lastClickedY = -1;
    }
}
