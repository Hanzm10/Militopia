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
import com.badlogic.gdx.utils.Array;
import com.militopia.components.GridPositionComponent;
import com.militopia.components.MovementComponent;
import com.militopia.components.StatsComponent;
import com.militopia.components.TypeComponent;
import com.militopia.config.GameConfig;
import com.militopia.data.GameState;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.ui.GameHUD;
import java.util.ArrayList;
import java.util.List;

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
    private boolean inputEnabled = true;

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

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
        if (!enabled) {
            deselect();
        }
    }

    public void deselect() {
        clearMarkers();
        selectedUnitEntity = null;
        gameHUD.hideSummonMenu();
        gameHUD.hideTileInfo();
        lastClickedX = -1;
        lastClickedY = -1;
        selectionIndex = 0;
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
        if (!inputEnabled) {
            return false;
        }
        camera.zoom += amountY * GameConfig.ZOOM_SPEED;
        camera.zoom = MathUtils.clamp(camera.zoom, GameConfig.ZOOM_MIN, GameConfig.ZOOM_MAX);
        camera.update();
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!inputEnabled) {
            return false;
        }

        lastTouchX = screenX;
        lastTouchY = screenY;
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        // --- UPDATED: Use dynamic gameMap dimensions ---
        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
            boolean isVisible = gameMap.visibleTiles[gridX][gridY];
            if (screen.isFogEnabled() && !isVisible) {
                deselect();
                return true;
            }

            Entity clickedMarker = getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);
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

            Entity foundUnit = null;
            Entity foundAnimal = null;
            Entity foundStructure = null;

            ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(GridPositionComponent.class).get());
            for (Entity e : entities) {
                GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
                if (pos.x == gridX && pos.y == gridY) {
                    TypeComponent type = e.getComponent(TypeComponent.class);
                    if (type.type == TypeComponent.Type.UNIT) {
                        foundUnit = e;
                    } else if (type.type == TypeComponent.Type.OBJECT) {
                        if (pos.zIndex == 2) {
                            foundAnimal = e;
                        } else {
                            foundStructure = e;
                        }
                    }
                }
            }

            List<String> targets = new ArrayList<>();
            if (foundUnit != null) {
                targets.add("UNIT");
            }
            if (foundAnimal != null) {
                targets.add("ANIMAL");
            }
            if (foundStructure != null) {
                targets.add("STRUCTURE");
            }
            targets.add("TERRAIN");

            String currentTarget = targets.get(selectionIndex % targets.size());

            clearMarkers();
            selectedUnitEntity = null;
            gameHUD.hideSummonMenu();
            triggerBounce(gridX, gridY);

            if (currentTarget.equals("UNIT")) {
                handleUnitTarget(foundUnit, foundAnimal, foundStructure, gridX, gridY);
            } else if (currentTarget.equals("ANIMAL")) {
                handleAnimalTarget(foundAnimal);
            } else if (currentTarget.equals("STRUCTURE")) {
                handleStructureTarget(foundStructure, gridX, gridY);
            } else {
                handleTerrainSelection(gridX, gridY);
            }

        } else {
            deselect();
        }
        return true;
    }

    private void handleUnitTarget(Entity foundUnit, Entity foundAnimal, Entity foundStructure, int gridX, int gridY) {
        StatsComponent unitStats = foundUnit.getComponent(StatsComponent.class);

        if (unitStats.owner != screen.getCurrentPlayer()) {
            UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.name.toUpperCase());
            gameHUD.showTileInfo(unitStats.name + " (Enemy)", info.region);
            return;
        }

        if (!GameConfig.TESTING_MODE && unitStats.hasActed) {
            gameHUD.showTileInfo("Unit Exhausted", unitFactory.getUnitUi("RECRUIT").region);
            return;
        }

        selectedUnitEntity = foundUnit;
        showMovementMarkers(gridX, gridY);
        UnitFactory.UiInfo info = unitFactory.getUnitUi("RECRUIT");
        gameHUD.showTileInfo(info.name, info.region);

        if (foundAnimal != null) {
            String animName = foundAnimal.getComponent(StatsComponent.class).name;
            MapGenerator.ObjectType animType = MapGenerator.ObjectType.HORSE;
            if (animName.contains("DEER")) {
                animType = MapGenerator.ObjectType.DEER;
            } else if (animName.contains("FISH")) {
                animType = MapGenerator.ObjectType.FISH;
            } else if (animName.contains("ZEBRA")) {
                animType = MapGenerator.ObjectType.ZEBRA;
            }

            gameHUD.openHuntMenu(foundAnimal, foundUnit, animType, unitFactory, this);
        }

        if (foundStructure != null) {
            StatsComponent structStats = foundStructure.getComponent(StatsComponent.class);

            MapGenerator.ObjectType type = gameMap.objects[gridX][gridY];
            boolean isCapturable = (type == MapGenerator.ObjectType.BASE_P1
                    || type == MapGenerator.ObjectType.BASE_P2
                    || type == MapGenerator.ObjectType.TOWN);

            if (isCapturable && structStats.owner != unitStats.owner) {
                gameHUD.openCaptureMenu(foundStructure, foundUnit, unitFactory, this, gameMap, screen.getGameState());
            }
        }
    }

    private void handleAnimalTarget(Entity foundAnimal) {
        StatsComponent stats = foundAnimal.getComponent(StatsComponent.class);
        String rawName = (stats != null) ? stats.name : "";
        MapGenerator.ObjectType type = MapGenerator.ObjectType.HORSE;
        if (rawName.contains("DEER")) {
            type = MapGenerator.ObjectType.DEER;
        } else if (rawName.contains("FISH")) {
            type = MapGenerator.ObjectType.FISH;
        } else if (rawName.contains("ZEBRA")) {
            type = MapGenerator.ObjectType.ZEBRA;
        } else if (rawName.contains("HORSE")) {
            type = MapGenerator.ObjectType.HORSE;
        }

        UnitFactory.UiInfo info = unitFactory.getObjectUi(type);
        gameHUD.showTileInfo(info.name, unitFactory.getHudIcon(type));
    }

    private void handleStructureTarget(Entity foundStructure, int gridX, int gridY) {
        MapGenerator.ObjectType objType = gameMap.objects[gridX][gridY];
        if (objType == MapGenerator.ObjectType.BASE_P1 || objType == MapGenerator.ObjectType.BASE_P2) {
            int owner = (objType == MapGenerator.ObjectType.BASE_P2) ? 2 : 1;
            if (owner == screen.getCurrentPlayer()) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    // --- Pass current base level to menu ---
                    int level = foundStructure.getComponent(StatsComponent.class).level;
                    gameHUD.openSummonMenu(owner, screen.getGameState(), level);
                    return;
                }
            }
        }
        UnitFactory.UiInfo info = unitFactory.getObjectUi(objType);
        gameHUD.showTileInfo(info.name, info.region);
    }

    public void performHunt(Entity animal, Entity hunter) {
        StatsComponent hunterStats = hunter.getComponent(StatsComponent.class);
        GameState state = screen.getGameState();
        if (hunterStats.owner == 1) {
            state.p1Funding += 1;
        } else {
            state.p2Funding += 1;
        }
        engine.removeEntity(animal);
        hunterStats.hasActed = true;
        int income = screen.calculateIncome(hunterStats.owner);
        gameHUD.updateFunding((hunterStats.owner == 1) ? state.p1Funding : state.p2Funding, income);
        System.out.println("Hunt Successful! +1 Funding.");
        gameHUD.hideSummonMenu();
        deselect();
    }

    private void handleTerrainSelection(int x, int y) {
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];

        if (gameMap.objects[x][y] != MapGenerator.ObjectType.NONE) {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name, unitFactory.getTextureForTerrain(terrain.ordinal()));
            return;
        }

        int owner = screen.getCurrentPlayer();
        int maxLevel = 0;
        boolean isTerritory = false;

        // --- NEW: Track Parent Base Coordinates ---
        int parentX = -1;
        int parentY = -1;

        ImmutableArray<Entity> entities = engine.getEntitiesFor(Family.all(StatsComponent.class, GridPositionComponent.class).get());

        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);

            // Check if it's a Base owned by current player
            if (stats.owner == owner && stats.income >= 2 && stats.name.contains("Base")) {
                int radius = stats.vision;
                // Check distance
                if (Math.abs(pos.x - x) <= radius && Math.abs(pos.y - y) <= radius) {
                    isTerritory = true;
                    // We use the level of the highest base covering this tile
                    if (stats.level > maxLevel) {
                        maxLevel = stats.level;
                        // Link to this base (highest level one takes priority if overlapping)
                        parentX = pos.x;
                        parentY = pos.y;
                    }
                }
            }
        }

        if (isTerritory) {
            boolean isWater = (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER);
            boolean isCoastal = isWater && hasAdjacentLand(x, y);

            // Pass Parent Coords to HUD
            gameHUD.openBuildMenu(x, y, owner, maxLevel, isWater, isCoastal, screen.getGameState(), parentX, parentY);
        } else {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name, unitFactory.getTextureForTerrain(terrain.ordinal()));
        }
    }

    private boolean hasAdjacentLand(int x, int y) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                if (t != MapGenerator.TerrainType.WATER && t != MapGenerator.TerrainType.DEEP_WATER) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (!inputEnabled) {
            return false;
        }
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);
        // --- UPDATED: Dynamic dimensions ---
        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
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
        if (!inputEnabled) {
            return false;
        }
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
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (stats != null) {
            stats.hasActed = true;
        }
        gameHUD.hideSummonMenu();
        clearMarkers();
        selectedUnitEntity = null;
    }

    private boolean isWalkable(int x, int y) {
        // --- UPDATED: Dynamic dimensions ---
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) {
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
        int moveRange = (stats != null) ? stats.attackRange : 3;
        // --- UPDATED: Dynamic dimensions ---
        int[][] visitedMoves = new int[gameMap.width][gameMap.height];
        for (int i = 0; i < gameMap.width; i++) {
            for (int j = 0; j < gameMap.height; j++) {
                visitedMoves[i][j] = -1;
            }
        }
        floodFill(startX, startY, moveRange, visitedMoves, startX, startY);
    }

    private void floodFill(int x, int y, int remainingMoves, int[][] visitedMoves, int startX, int startY) {
        if (remainingMoves < 0) {
            return;
        }
        // --- UPDATED: Dynamic dimensions ---
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height) {
            return;
        }
        if (visitedMoves[x][y] >= remainingMoves) {
            return;
        }
        boolean isStart = (x == startX && y == startY);
        if (!isStart && !isWalkable(x, y)) {
            return;
        }
        visitedMoves[x][y] = remainingMoves;
        if (!isStart) {
            if (getEntityAt(x, y, TypeComponent.Type.MARKER) == null) {
                entityFactory.createMovementMarker(x, y);
            }
        }
        int nextMove = remainingMoves - 1;
        floodFill(x + 1, y, nextMove, visitedMoves, startX, startY);
        floodFill(x - 1, y, nextMove, visitedMoves, startX, startY);
        floodFill(x, y + 1, nextMove, visitedMoves, startX, startY);
        floodFill(x, y - 1, nextMove, visitedMoves, startX, startY);
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
