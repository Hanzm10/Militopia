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
import com.militopia.components.AbilitiesComponent;
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
import com.militopia.systems.CombatSystem;
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
    private final CombatSystem combatSystem;

    private int lastTouchX, lastTouchY;
    private int lastClickedX = -1, lastClickedY = -1;
    private Entity selectedUnitEntity = null;
    private int bouncingX = -1, bouncingY = -1;
    private float bounceTimer = 0;
    private int hoveredX = -1, hoveredY = -1;
    private boolean inputEnabled = true;

    private int selectionIndex = 0;
    private boolean isTargetingAbility = false;
    private String targetingAbilityKey = null;
    private Entity targetingUnit = null;

    public GameInputController(GameScreen screen, OrthographicCamera camera, PooledEngine engine,
            MapGenerator.GameMap gameMap, UnitFactory unitFactory,
            EntityFactory entityFactory, GameHUD gameHUD, CombatSystem combatSystem) {
        this.screen = screen;
        this.camera = camera;
        this.engine = engine;
        this.gameMap = gameMap;
        this.unitFactory = unitFactory;
        this.entityFactory = entityFactory;
        this.gameHUD = gameHUD;
        this.combatSystem = combatSystem;
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
        if (!inputEnabled)
            return false;
        camera.zoom += amountY * GameConfig.ZOOM_SPEED;
        camera.zoom = MathUtils.clamp(camera.zoom, GameConfig.ZOOM_MIN, GameConfig.ZOOM_MAX);
        camera.update();
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!inputEnabled)
            return false;

        lastTouchX = screenX;
        lastTouchY = screenY;
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
            // --- ABILITY TARGETING ---
            if (isTargetingAbility) {
                executeTargetingAbility(gridX, gridY);
                return true;
            }

            boolean isVisible = gameMap.visibleTiles[gridX][gridY];
            if (screen.isFogEnabled() && !isVisible) {
                deselect();
                return true;
            }

            // --- MOVEMENT: click on a blue move-marker ---
            Entity clickedMoveMarker = getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);
            if (clickedMoveMarker != null && selectedUnitEntity != null) {
                moveUnit(selectedUnitEntity, gridX, gridY);
                return true;
            }

            // --- ATTACK: click on a red attack-marker tile (must have an enemy unit) ---
            Entity clickedAttackMarker = getEntityAt(gridX, gridY, TypeComponent.Type.ATTACK_MARKER);
            if (clickedAttackMarker != null && selectedUnitEntity != null) {
                Entity targetUnit = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (targetUnit != null) {
                    StatsComponent tStats = targetUnit.getComponent(StatsComponent.class);
                    if (tStats != null && tStats.owner != screen.getCurrentPlayer()) {
                        performAttack(selectedUnitEntity, targetUnit);
                        return true;
                    }
                }
                // Clicked an attack marker on an empty tile — do nothing extra
                return true;
            }

            // --- ATTACK: directly click enemy unit tile within range (no marker needed)
            // ---
            if (selectedUnitEntity != null) {
                Entity directTarget = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (directTarget != null) {
                    StatsComponent tStats = directTarget.getComponent(StatsComponent.class);
                    StatsComponent aStats = selectedUnitEntity.getComponent(StatsComponent.class);
                    GridPositionComponent aPos = selectedUnitEntity.getComponent(GridPositionComponent.class);
                    if (tStats != null && aStats != null && aPos != null
                            && tStats.owner != screen.getCurrentPlayer()) {
                        int dist = chebyshev(aPos.x, aPos.y, gridX, gridY);
                        if (dist <= aStats.attackRange) {
                            performAttack(selectedUnitEntity, directTarget);
                            return true;
                        }
                    }
                }
            }

            // --- Normal click cycling ---
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
            if (foundUnit != null)
                targets.add("UNIT");
            if (foundAnimal != null)
                targets.add("ANIMAL");
            if (foundStructure != null)
                targets.add("STRUCTURE");
            targets.add("TERRAIN");

            String currentTarget = targets.get(selectionIndex % targets.size());

            clearMarkers();
            selectedUnitEntity = null;
            gameHUD.hideSummonMenu();
            triggerBounce(gridX, gridY);

            if (currentTarget.equals("UNIT"))
                handleUnitTarget(foundUnit, foundAnimal, foundStructure, gridX, gridY);
            else if (currentTarget.equals("ANIMAL"))
                handleAnimalTarget(foundAnimal);
            else if (currentTarget.equals("STRUCTURE"))
                handleStructureTarget(foundStructure, gridX, gridY);
            else
                handleTerrainSelection(gridX, gridY);

        } else {
            deselect();
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Attack helpers
    // -------------------------------------------------------------------------

    /** Delegates to CombatSystem, then cleans up selection state. */
    private void performAttack(Entity attacker, Entity defender) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        combatSystem.resolveAttack(attacker, defender);
        // Snap HP in the HUD if the attacker survived (still selectable next turn)
        if (aStats != null && aStats.currentHP > 0) {
            gameHUD.snapHP(aStats.currentHP, aStats.maxHP);
        }
        clearMarkers();
        selectedUnitEntity = null;
        gameHUD.hideTileInfo();
    }

    /** Chebyshev distance for range checks. */
    private int chebyshev(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    // -------------------------------------------------------------------------
    // Unit / target handlers
    // -------------------------------------------------------------------------

    private void handleUnitTarget(Entity foundUnit, Entity foundAnimal, Entity foundStructure, int gridX, int gridY) {
        StatsComponent unitStats = foundUnit.getComponent(StatsComponent.class);

        if (unitStats.owner != screen.getCurrentPlayer()) {
            UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.name.toUpperCase());
            gameHUD.showUnitInfo(foundUnit, unitStats.name + " (Enemy)", info.region, unitStats.currentHP,
                    unitStats.maxHP);
            return;
        }

        if (!GameConfig.TESTING_MODE && unitStats.hasActed) {
            gameHUD.showTileInfo("Unit Exhausted", unitFactory.getUnitUi("RECRUIT").region);
            return;
        }

        selectedUnitEntity = foundUnit;
        showRangeMarkers(gridX, gridY);
        UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.name.toUpperCase());
        gameHUD.showUnitInfo(foundUnit, info.name, info.region, unitStats.currentHP, unitStats.maxHP);

        if (foundAnimal != null) {
            String animName = foundAnimal.getComponent(StatsComponent.class).name;
            MapGenerator.ObjectType animType = MapGenerator.ObjectType.HORSE;
            if (animName.contains("DEER"))
                animType = MapGenerator.ObjectType.DEER;
            else if (animName.contains("FISH"))
                animType = MapGenerator.ObjectType.FISH;
            else if (animName.contains("ZEBRA"))
                animType = MapGenerator.ObjectType.ZEBRA;
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
        if (rawName.contains("DEER"))
            type = MapGenerator.ObjectType.DEER;
        else if (rawName.contains("FISH"))
            type = MapGenerator.ObjectType.FISH;
        else if (rawName.contains("ZEBRA"))
            type = MapGenerator.ObjectType.ZEBRA;
        else if (rawName.contains("HORSE"))
            type = MapGenerator.ObjectType.HORSE;
        UnitFactory.UiInfo info = unitFactory.getObjectUi(type);
        gameHUD.showTileInfo(info.name, unitFactory.getHudIcon(type));
    }

    private void handleStructureTarget(Entity foundStructure, int gridX, int gridY) {
        MapGenerator.ObjectType objType = gameMap.objects[gridX][gridY];
        StatsComponent structStats = foundStructure.getComponent(StatsComponent.class);

        // --- Handle Bases ---
        if (objType == MapGenerator.ObjectType.BASE_P1 || objType == MapGenerator.ObjectType.BASE_P2) {
            int owner = (objType == MapGenerator.ObjectType.BASE_P2) ? 2 : 1;
            if (owner == screen.getCurrentPlayer()) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    int level = structStats.level;
                    gameHUD.openSummonMenu(owner, screen.getGameState(), level, "BASE");
                    return;
                }
            }
        }

        // --- Handle Specialized Structures (like PORTS) ---
        if (structStats != null && structStats.owner == screen.getCurrentPlayer()) {
            // Check for Port specifically
            if (structStats.name.equalsIgnoreCase("Port")) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    // Ports use parent base level for unlocks (or their own level if we decide
                    // later)
                    // For now, let's use a default high level or track unlocks globally.
                    // Actually, let's use the player's highest base level in that territory.
                    int level = structStats.level;
                    gameHUD.openSummonMenu(structStats.owner, screen.getGameState(), level, "PORT");
                    return;
                }
            }
        }

        UnitFactory.UiInfo info = unitFactory.getObjectUi(objType);
        gameHUD.showTileInfo(info.name, info.region);
    }

    // -------------------------------------------------------------------------
    // Hunt
    // -------------------------------------------------------------------------

    public void performHunt(Entity animal, Entity hunter) {
        StatsComponent hunterStats = hunter.getComponent(StatsComponent.class);
        GameState state = screen.getGameState();
        if (hunterStats.owner == 1)
            state.p1Funding += 1;
        else
            state.p2Funding += 1;
        engine.removeEntity(animal);
        hunterStats.hasActed = true;
        hunterStats.hasMoved = true;
        int income = screen.calculateIncome(hunterStats.owner);
        gameHUD.updateFunding((hunterStats.owner == 1) ? state.p1Funding : state.p2Funding, income);
        gameHUD.hideSummonMenu();
        deselect();
    }

    // -------------------------------------------------------------------------
    // Abilities
    // -------------------------------------------------------------------------

    public void performAbility(Entity unit, String abilityKey) {
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (stats == null || abilities == null)
            return;

        if (abilityKey.equals("DIG_IN")) {
            abilities.isDiggingIn = true;
            abilities.hasUsedDigIn = true;
            stats.hasActed = true;
            stats.hasMoved = true;
            // Visual feedback could be added here (e.g., spawn floating text "DUG IN")
            gameHUD.snapHP(stats.currentHP, stats.maxHP); // Refresh UI
            deselect();
        } else if (abilityKey.equals("LAUNCH_NUKE")) {
            isTargetingAbility = true;
            targetingAbilityKey = "LAUNCH_NUKE";
            targetingUnit = unit;
            // Highlight area or show range markers if needed
            gameHUD.hideTileInfo();
        }
    }

    private void executeTargetingAbility(int tx, int ty) {
        if (targetingAbilityKey.equals("LAUNCH_NUKE")) {
            combatSystem.launchNuke(targetingUnit, tx, ty);
        }
        isTargetingAbility = false;
        targetingAbilityKey = null;
        targetingUnit = null;
        deselect();
    }

    // -------------------------------------------------------------------------
    // Terrain
    // -------------------------------------------------------------------------

    private void handleTerrainSelection(int x, int y) {
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];

        if (gameMap.objects[x][y] != MapGenerator.ObjectType.NONE) {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
            return;
        }
        if (getEntityAt(x, y, TypeComponent.Type.OBJECT) != null) {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
            return;
        }

        int owner = screen.getCurrentPlayer();
        int maxLevel = 0;
        boolean isTerritory = false;
        int parentX = -1, parentY = -1;

        ImmutableArray<Entity> entities = engine
                .getEntitiesFor(Family.all(StatsComponent.class, GridPositionComponent.class).get());
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (stats.owner == owner && stats.income >= 2 && stats.name.contains("Base")) {
                int radius = stats.vision;
                if (Math.abs(pos.x - x) <= radius && Math.abs(pos.y - y) <= radius) {
                    isTerritory = true;
                    if (stats.level > maxLevel) {
                        maxLevel = stats.level;
                        parentX = pos.x;
                        parentY = pos.y;
                    }
                }
            }
        }

        if (isTerritory) {
            boolean isWater = (terrain == MapGenerator.TerrainType.WATER
                    || terrain == MapGenerator.TerrainType.DEEP_WATER);
            boolean isCoastal = isWater && hasAdjacentLand(x, y);
            gameHUD.openBuildMenu(x, y, owner, maxLevel, isWater, isCoastal, screen.getGameState(), parentX, parentY);
        } else {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
        }
    }

    private boolean hasAdjacentLand(int x, int y) {
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                if (t != MapGenerator.TerrainType.WATER && t != MapGenerator.TerrainType.DEEP_WATER)
                    return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Camera / mouse
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (!inputEnabled)
            return false;
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);
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
        if (!inputEnabled)
            return false;
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float x = Gdx.input.getDeltaX();
            float y = Gdx.input.getDeltaY();
            camera.translate(-x * camera.zoom * GameConfig.DRAG_SPEED, y * camera.zoom * GameConfig.DRAG_SPEED);
            camera.update();
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Markers
    // -------------------------------------------------------------------------

    private void triggerBounce(int x, int y) {
        this.bouncingX = x;
        this.bouncingY = y;
        this.bounceTimer = GameConfig.BOUNCE_DURATION;
    }

    private void moveUnit(Entity unit, int targetX, int targetY) {
        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        if (pos == null)
            return;
        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));
        int oldX = pos.x;
        int oldY = pos.y;
        pos.x = targetX;
        pos.y = targetY;

        // RANGER: Overwatch (Check if move triggers an enemy attack)
        combatSystem.checkOverwatch(unit, targetX, targetY);
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (stats != null) {
            stats.hasActed = true;
            stats.hasMoved = true;
        }

        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (abilities != null) {
            abilities.isDiggingIn = false;
        }
        gameHUD.hideSummonMenu();
        clearMarkers();
        selectedUnitEntity = null;
    }

    /**
     * Shows both blue movement markers AND red attack-range markers for the
     * selected unit simultaneously.
     */
    private void showRangeMarkers(int startX, int startY) {
        StatsComponent stats = selectedUnitEntity.getComponent(StatsComponent.class);
        int moveRange = (stats != null) ? stats.move : 3;
        int atkRange = (stats != null) ? stats.attackRange : 1;
        StatsComponent.MoveType moveType = (stats != null) ? stats.moveType : StatsComponent.MoveType.LAND;

        // --- Blue move markers ---
        if (!stats.hasMoved) {
            int[][] visitedMoves = new int[gameMap.width][gameMap.height];
            for (int[] row : visitedMoves)
                java.util.Arrays.fill(row, -1);
            floodFill(startX, startY, moveRange, visitedMoves, startX, startY, moveType);
        }

        // --- Red attack markers ---
        // Enumerate all tiles within Chebyshev distance == attackRange.
        // Skip tiles occupied by own units or by the attacker itself.
        for (int dx = -atkRange; dx <= atkRange; dx++) {
            for (int dy = -atkRange; dy <= atkRange; dy++) {
                if (dx == 0 && dy == 0)
                    continue; // skip self
                int tx = startX + dx;
                int ty = startY + dy;
                if (tx < 0 || tx >= gameMap.width || ty < 0 || ty >= gameMap.height)
                    continue;
                if (Math.max(Math.abs(dx), Math.abs(dy)) > atkRange)
                    continue;

                // Don't double-up on a tile that is already a blue move marker
                if (getEntityAt(tx, ty, TypeComponent.Type.MARKER) != null)
                    continue;
                // Only show attack markers on tiles occupied by enemies OR empty
                // enemy-reachable tiles
                Entity tileUnit = getEntityAt(tx, ty, TypeComponent.Type.UNIT);
                if (tileUnit != null) {
                    StatsComponent ts = tileUnit.getComponent(StatsComponent.class);
                    if (ts != null && ts.owner == screen.getCurrentPlayer())
                        continue; // skip own units
                }
                // For clean UX: only show red marker where there is an actual enemy
                if (tileUnit == null)
                    continue;
                entityFactory.createAttackMarker(tx, ty);
            }
        }
    }

    /** Flood-fill BFS for movement range (unchanged logic). */
    private void floodFill(int x, int y, int remainingMoves, int[][] visitedMoves,
            int startX, int startY, StatsComponent.MoveType moveType) {
        if (remainingMoves < 0)
            return;
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height)
            return;
        if (visitedMoves[x][y] >= remainingMoves)
            return;

        boolean isStart = (x == startX && y == startY);
        if (!isStart && !isWalkable(x, y, moveType))
            return;

        visitedMoves[x][y] = remainingMoves;
        if (!isStart && getEntityAt(x, y, TypeComponent.Type.MARKER) == null) {
            entityFactory.createMovementMarker(x, y);
        }

        int next = remainingMoves - 1;
        floodFill(x + 1, y, next, visitedMoves, startX, startY, moveType);
        floodFill(x - 1, y, next, visitedMoves, startX, startY, moveType);
        floodFill(x, y + 1, next, visitedMoves, startX, startY, moveType);
        floodFill(x, y - 1, next, visitedMoves, startX, startY, moveType);
        floodFill(x + 1, y + 1, next, visitedMoves, startX, startY, moveType);
        floodFill(x - 1, y + 1, next, visitedMoves, startX, startY, moveType);
        floodFill(x + 1, y - 1, next, visitedMoves, startX, startY, moveType);
        floodFill(x - 1, y - 1, next, visitedMoves, startX, startY, moveType);
    }

    private boolean isWalkable(int x, int y, StatsComponent.MoveType moveType) {
        if (x < 0 || x >= gameMap.width || y < 0 || y >= gameMap.height)
            return false;
        MapGenerator.TerrainType terrain = gameMap.terrain[x][y];
        if (moveType == StatsComponent.MoveType.LAND) {
            if (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER)
                return false;
        } else if (moveType == StatsComponent.MoveType.SEA) {
            if (terrain != MapGenerator.TerrainType.WATER && terrain != MapGenerator.TerrainType.DEEP_WATER)
                return false;
        }
        if (getEntityAt(x, y, TypeComponent.Type.UNIT) != null)
            return false;
        return true;
    }

    /**
     * Removes all MARKER and ATTACK_MARKER entities from the engine.
     */
    private void clearMarkers() {
        ImmutableArray<Entity> all = engine.getEntitiesFor(Family.all(TypeComponent.class).get());
        Array<Entity> toRemove = new Array<>();
        for (Entity e : all) {
            TypeComponent.Type t = e.getComponent(TypeComponent.class).type;
            if (t == TypeComponent.Type.MARKER || t == TypeComponent.Type.ATTACK_MARKER) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove)
            engine.removeEntity(e);
    }

    /** Public alias for undo — clears markers and resets selection. */
    public void clearMarkersPublic() {
        clearMarkers();
        selectedUnitEntity = null;
    }

    private Entity getEntityAt(int x, int y, TypeComponent.Type type) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class, TypeComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            TypeComponent t = e.getComponent(TypeComponent.class);
            if (pos.x == x && pos.y == y && t.type == type)
                return e;
        }
        return null;
    }
}
