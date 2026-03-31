package com.militopia.controller;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.militopia.components.*;
import com.militopia.config.AnimalType;
import com.militopia.config.CombatConstants;
import com.militopia.config.GameConfig;
import com.militopia.config.UnitType;
import com.militopia.data.GameState;
import com.militopia.factories.EntityFactory;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.systems.CombatSystem;
import com.militopia.ui.GameHUD;
import com.militopia.utils.GameLogger;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import java.util.ArrayList;
import java.util.List;

public class GameInputController extends InputAdapter {

    private final GameScreen screen;
    private final OrthographicCamera camera;
    private final Viewport viewport;
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

    // Dev Mode
    public enum DevSpawnMode { NONE, SPAWNING_UNIT, BUILDING_STRUCTURE, KILLING_UNIT, REMOVING_OBJECT, BUILDING_MAP_OBJ, SETTING_BASE_LEVEL }
    private DevSpawnMode devSpawnMode = DevSpawnMode.NONE;
    private UnitType devSpawnUnitType;
    private String devSpawnStructureType;
    private String devSpawnMapObjType;
    public int devSpawnOwner = 1;

    public GameInputController(GameScreen screen, OrthographicCamera camera, Viewport viewport,
            PooledEngine engine, MapGenerator.GameMap gameMap, UnitFactory unitFactory,
            EntityFactory entityFactory, GameHUD gameHUD, CombatSystem combatSystem) {
        this.screen = screen;
        this.camera = camera;
        this.viewport = viewport;
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

    public void enterDevSpawn(DevSpawnMode mode, UnitType type, int owner) {
        devSpawnMode = mode; devSpawnUnitType = type; devSpawnOwner = owner;
    }
    public void enterDevBuild(String type, int owner) {
        devSpawnMode = DevSpawnMode.BUILDING_STRUCTURE; devSpawnStructureType = type; devSpawnOwner = owner;
    }
    public void enterDevBuildMapObj(String type, int owner) {
        devSpawnMode = DevSpawnMode.BUILDING_MAP_OBJ; devSpawnMapObjType = type; devSpawnOwner = owner;
    }
    public void enterDevKill()         { devSpawnMode = DevSpawnMode.KILLING_UNIT; }
    public void enterDevRemove()       { devSpawnMode = DevSpawnMode.REMOVING_OBJECT; }
    public void enterDevSetBaseLevel() { devSpawnMode = DevSpawnMode.SETTING_BASE_LEVEL; }
    public void exitDevMode()          { devSpawnMode = DevSpawnMode.NONE; }
    public DevSpawnMode getDevSpawnMode() { return devSpawnMode; }

    public void deselect() {
        if (selectedUnitEntity != null) {
            AudioManager.getInstance().playSFX(SFXKeys.UNIT_DESELECT);
        }
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
    public boolean keyDown(int keycode) {
        if (!inputEnabled) return false;

        // Undo Shortcut (Ctrl+Z)
        if (keycode == Input.Keys.Z && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
            screen.undoTurn();
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        GameLogger.log(GameLogger.INPUT, "Mouse scrolled: x=" + amountX + ", y=" + amountY);
        float oldZoom = camera.zoom;
        camera.zoom += amountY * GameConfig.ZOOM_SPEED;
        camera.zoom = MathUtils.clamp(camera.zoom, GameConfig.ZOOM_MIN, GameConfig.ZOOM_MAX);
        camera.update();
        if (oldZoom != camera.zoom) {
            GameLogger.log(GameLogger.CAMERA, String.format("Camera zoom: %.2f (delta: %.2f)", camera.zoom, amountY));
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!inputEnabled)
            return false;

        // --- LAN LOCKDOWN ---
        // Prevent all map interaction if it's not the local hardware's turn!
        if (screen.getGameState().currentPlayer != screen.getActiveLocalPlayer()) {
            return false;
        }

        lastTouchX = screenX;
        lastTouchY = screenY;
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0),
                viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        // INPUT_OFFSET shifts world coords to align with the rendered tile origin
        float adjustedY = worldCoords.y + GameConfig.INPUT_OFFSET_Y;
        float adjustedX = worldCoords.x + GameConfig.INPUT_OFFSET_X;
        float halfW = GameConfig.TILE_WIDTH / 2.0f;
        float halfH = GameConfig.TILE_HEIGHT / 2.0f;
        // Inverse isometric projection: convert 2D screen position back to grid (x, y).
        // Standard iso formula: screenX = (gridX - gridY) * halfW,
        //                       screenY = (gridX + gridY) * halfH
        // Solving for gridX and gridY gives the expressions below.
        int gridX = MathUtils.floor((adjustedY / halfH + adjustedX / halfW) / 2);
        int gridY = MathUtils.floor((adjustedY / halfH - adjustedX / halfW) / 2);

        if (gridX >= 0 && gridX < gameMap.width && gridY >= 0 && gridY < gameMap.height) {
            // Reset HUD stage scroll focus so map zoom (scroll wheel) works even
            // after the user has interacted with the dev panel ScrollPane.
            gameHUD.stage.setScrollFocus(null);

            // Auto-retract dev panel when the user clicks the map.
            if (screen.getGameState().isDevMode && screen.devPanel != null
                    && screen.devPanel.isVisible()) {
                screen.devPanel.retract();
            }

            // --- DEV MODE INTERCEPT ---
            if (screen.getGameState().isDevMode && devSpawnMode != DevSpawnMode.NONE) {
                handleDevTouch(gridX, gridY);
                return true;
            }

            // --- ABILITY TARGETING ---
            if (isTargetingAbility) {
                executeTargetingAbility(gridX, gridY);
                return true;
            }

            boolean isVisible = gameMap.visibleTiles[gridX][gridY];
            // --- NEW: Movement into Fog (Jammers) ---
            // Allow interaction even if fogged IF there's a movement marker there.
            Entity clickedMoveMarker = getEntityAt(gridX, gridY, TypeComponent.Type.MARKER);

            if (screen.isFogEnabled() && !isVisible && clickedMoveMarker == null) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                deselect();
                return true;
            }

            // --- MOVEMENT: click on a blue move-marker ---
            if (clickedMoveMarker != null && selectedUnitEntity != null) {
                moveUnit(selectedUnitEntity, gridX, gridY);
                return true;
            }

            // --- TRANSFORMATION: click on a transform-marker ---
            Entity clickedTransformMarker = getEntityAt(gridX, gridY, TypeComponent.Type.TRANSFORM_MARKER);
            if (clickedTransformMarker != null && selectedUnitEntity != null) {
                transformUnit(selectedUnitEntity, gridX, gridY);
                return true;
            }

            // --- ATTACK: click on a red attack-marker tile ---
            Entity clickedAttackMarker = getEntityAt(gridX, gridY, TypeComponent.Type.ATTACK_MARKER);
            if (clickedAttackMarker != null && selectedUnitEntity != null) {
                StatsComponent aStats = selectedUnitEntity.getComponent(StatsComponent.class);
                Entity targetUnit = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                Entity enemy = null;
                if (targetUnit != null) {
                    StatsComponent tStats = targetUnit.getComponent(StatsComponent.class);
                    if (tStats != null && tStats.owner != screen.getActiveLocalPlayer()) {
                        enemy = targetUnit;
                    }
                }
                if (aStats != null && aStats.unitType == UnitType.JUGGERNAUT) {
                    performJump(selectedUnitEntity, enemy, gridX, gridY);
                    return true;
                }
                if (enemy != null) {
                    performAttack(selectedUnitEntity, enemy);
                    return true;
                }
                return true;
            }

            // --- ATTACK: directly click enemy unit tile within range (no marker needed) ---
            if (selectedUnitEntity != null) {
                Entity directTarget = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (directTarget != null) {
                    StatsComponent tStats = directTarget.getComponent(StatsComponent.class);
                    StatsComponent aStats = selectedUnitEntity.getComponent(StatsComponent.class);
                    GridPositionComponent aPos = selectedUnitEntity.getComponent(GridPositionComponent.class);
                    if (tStats != null && aStats != null && aPos != null
                            && tStats.owner != screen.getActiveLocalPlayer()) {
                        int dist = chebyshev(aPos.x, aPos.y, gridX, gridY);
                        if (dist <= aStats.attackRange) {
                            if (aStats.unitType == UnitType.JUGGERNAUT) {
                                performJump(selectedUnitEntity, directTarget, gridX, gridY);
                            } else {
                                performAttack(selectedUnitEntity, directTarget);
                            }
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

            // LOG: raw tile click
            MapGenerator.TerrainType clickedTerrain = gameMap.terrain[gridX][gridY];
            GameLogger.log(GameLogger.INPUT, "Click " + GameLogger.pos(gridX, gridY)
                    + " | terrain=" + clickedTerrain.name());

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
                        if (pos.zIndex == 2 || e.getComponent(AnimalComponent.class) != null) {
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
                handleTerrainSelection(gridX, gridY, clickedTerrain);

        } else {
            AudioManager.getInstance().playSFX(SFXKeys.TILE_DESELECT);
            deselect();
            gameHUD.hideTileInfo(); // NEW: Auto-hide D-03
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

    /** Triggers a Juggernaut jump to the target tile. Target may be null for empty-tile jumps. */
    private void performJump(Entity attacker, Entity target, int tx, int ty) {
        StatsComponent aStats = attacker.getComponent(StatsComponent.class);
        // Block jump onto water terrain
        MapGenerator.TerrainType terrain = gameMap.terrain[tx][ty];
        if (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER) return;
        // Block jump onto a friendly unit tile
        Entity tileUnit = getEntityAt(tx, ty, TypeComponent.Type.UNIT);
        if (tileUnit != null) {
            StatsComponent ts = tileUnit.getComponent(StatsComponent.class);
            if (ts != null && ts.owner == screen.getActiveLocalPlayer()) return;
        }
        combatSystem.resolveJumperAttack(attacker, target, tx, ty);
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

        if (unitStats.owner != screen.getActiveLocalPlayer()) {
            UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.unitType);
            GameLogger.log(GameLogger.INPUT, unitStats.owner,
                    "Enemy unit inspected: " + unitStats.name + " at " + GameLogger.pos(gridX, gridY)
                            + " | HP: " + unitStats.currentHP + "/" + unitStats.maxHP);
            gameHUD.showUnitInfo(foundUnit, unitStats.name + " (Enemy)", info.region, unitStats.currentHP,
                    unitStats.maxHP);
            return;
        }

        if (!GameConfig.TESTING_MODE && unitStats.hasActed) {
            GameLogger.log(GameLogger.INPUT,
                    "Unit exhausted: " + unitStats.name + " at " + GameLogger.pos(gridX, gridY));
            AudioManager.getInstance().playSFX(SFXKeys.UNIT_CANT_MOVE);
            UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.unitType);
            gameHUD.showTileInfo("Unit Exhausted (" + unitStats.name + ")", info.region);
            return;
        }

        selectedUnitEntity = foundUnit;
        AudioManager.getInstance().playSFX(SFXKeys.UNIT_SELECT);
        GameLogger.log(GameLogger.INPUT, "Unit selected: " + unitStats.name
                + " at " + GameLogger.pos(gridX, gridY)
                + " | HP: " + unitStats.currentHP + "/" + unitStats.maxHP);
        showRangeMarkers(gridX, gridY);
        UnitFactory.UiInfo info = unitFactory.getUnitUi(unitStats.unitType);
        gameHUD.showUnitInfo(foundUnit, info.name, info.region, unitStats.currentHP, unitStats.maxHP);

        if (foundAnimal != null) {
            String animName = foundAnimal.getComponent(StatsComponent.class).name;
            AnimalType detectedAnimal = AnimalType.fromKey(animName);
            MapGenerator.ObjectType animType = MapGenerator.ObjectType.HORSE;
            if (detectedAnimal == AnimalType.DEER)
                animType = MapGenerator.ObjectType.DEER;
            else if (detectedAnimal == AnimalType.FISH)
                animType = MapGenerator.ObjectType.FISH;
            else if (detectedAnimal == AnimalType.ZEBRA)
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

            if (type == MapGenerator.ObjectType.RUINS) {
                gameHUD.openScavengeMenu(foundStructure, foundUnit, unitFactory, this);
            }
        }
    }

    private void handleAnimalTarget(Entity foundAnimal) {
        StatsComponent stats = foundAnimal.getComponent(StatsComponent.class);
        String rawName = (stats != null) ? stats.name : "";
        GameLogger.log(GameLogger.INPUT, "Animal inspected: " + rawName);
        AnimalType detectedAnimal = AnimalType.fromKey(rawName);
        MapGenerator.ObjectType type = MapGenerator.ObjectType.HORSE;
        if (detectedAnimal == AnimalType.DEER)
            type = MapGenerator.ObjectType.DEER;
        else if (detectedAnimal == AnimalType.FISH)
            type = MapGenerator.ObjectType.FISH;
        else if (detectedAnimal == AnimalType.ZEBRA)
            type = MapGenerator.ObjectType.ZEBRA;
        else if (detectedAnimal == AnimalType.HORSE)
            type = MapGenerator.ObjectType.HORSE;
        UnitFactory.UiInfo info = unitFactory.getObjectUi(type);
        gameHUD.showTileInfo(info.name, unitFactory.getHudIcon(type));
    }

    private void handleStructureTarget(Entity foundStructure, int gridX, int gridY) {
        MapGenerator.ObjectType objType = gameMap.objects[gridX][gridY];
        StatsComponent structStats = foundStructure.getComponent(StatsComponent.class);
        int sOwner = (structStats != null) ? structStats.owner : 0;
        String sOwnerStr = (sOwner == 0) ? "neutral" : "P" + sOwner;
        String structName = (structStats != null) ? structStats.name : objType.name();

        GameLogger.log(GameLogger.INPUT,
                "Structure selected: " + structName
                        + " at " + GameLogger.pos(gridX, gridY) + " | owner=" + sOwnerStr);

        // --- Handle Bases ---
        if (objType == MapGenerator.ObjectType.BASE_P1 || objType == MapGenerator.ObjectType.BASE_P2) {
            int owner = (objType == MapGenerator.ObjectType.BASE_P2) ? 2 : 1;

            // Show base info in the bottom panel (D-01)
            if (owner == screen.getActiveLocalPlayer()) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    int level = structStats.level;
                    // --- UNIQUE: Unified Base Menu (Stats + Summons) ---
                    gameHUD.showBaseInfoUnified(foundStructure, screen.getGameState(), level, "BASE");
                    return;
                }
            }
            // Enemy Base or unit on top: Show standard InfoPanel scouting
            // Use specialized name and icon
            gameHUD.showBaseInfo(foundStructure, structName, foundStructure.getComponent(com.militopia.components.TextureComponent.class).region, true);
            return;
        }

        // --- Handle Specialized Structures (like PORTS) ---
        if (structStats != null && structStats.owner == screen.getActiveLocalPlayer()) {
            if (structStats.name.equalsIgnoreCase("Port")) {
                Entity unitOnTop = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitOnTop == null) {
                    int portLevel = findMaxBaseLevelNear(gridX, gridY, structStats.owner);
                    gameHUD.openSummonMenu(structStats.owner, screen.getGameState(), portLevel, "PORT");
                    return;
                }
            }
        }

        // If it's a structure with stats (income/xp), use showBaseInfo to show stats
        if (structStats != null && (structStats.income > 0 || structStats.xpGain > 0 || structStats.owner > 0)) {
            gameHUD.showBaseInfo(foundStructure, structName, unitFactory.getTextureForPopup(structStats.unitTypeKey),
                    true);
        } else {
            UnitFactory.UiInfo info = unitFactory.getObjectUi(objType);
            gameHUD.showTileInfo(info.name, info.region);
        }
    }

    // -------------------------------------------------------------------------
    // Hunt
    // -------------------------------------------------------------------------

    public void performHunt(Entity animal, Entity hunter) {
        StatsComponent hunterStats = hunter.getComponent(StatsComponent.class);
        GameState state = screen.getGameState();
        GameLogger.log(GameLogger.CAPTURE, hunterStats.owner,
                "Hunt: " + hunterStats.name + " hunted animal at " + GameLogger.pos(
                        animal.getComponent(GridPositionComponent.class) != null
                                ? animal.getComponent(GridPositionComponent.class).x
                                : -1,
                        animal.getComponent(GridPositionComponent.class) != null
                                ? animal.getComponent(GridPositionComponent.class).y
                                : -1)
                        + " | +" + CombatConstants.ANIMAL_HUNT_FUNDING + " funding");
        if (hunterStats.owner == 1)
            state.p1Funding += CombatConstants.ANIMAL_HUNT_FUNDING;
        else
            state.p2Funding += CombatConstants.ANIMAL_HUNT_FUNDING;
        AudioManager.getInstance().playSFX(SFXKeys.ACTION_HUNT);
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

        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        String posStr = (pos != null) ? GameLogger.pos(pos.x, pos.y) : "(?,?)";

        if (abilityKey.equals("DIG_IN")) {
            GameLogger.log(GameLogger.ABILITY, stats.owner,
                    "DIG IN: " + stats.name + " digs in at " + posStr);
            abilities.isDiggingIn = true;
            abilities.hasUsedDigIn = true;
            stats.hasActed = true;
            stats.hasMoved = true;
            // Visual feedback could be added here (e.g., spawn floating text "DUG IN")
            gameHUD.snapHP(stats.currentHP, stats.maxHP); // Refresh UI
            deselect();
        } else if (abilityKey.equals("LAUNCH_NUKE")) {
            GameLogger.log(GameLogger.ABILITY, stats.owner,
                    "LAUNCH NUKE: " + stats.name + " at " + posStr + " — awaiting target tile");
            isTargetingAbility = true;
            targetingAbilityKey = "LAUNCH_NUKE";
            targetingUnit = unit;
            // Highlight area or show range markers if needed
            gameHUD.hideTileInfo();
        } else if (abilityKey.equals("OVERWATCH")) {
            GameLogger.log(GameLogger.ABILITY, stats.owner,
                    "OVERWATCH: " + stats.name + " goes into overwatch at " + posStr);
            abilities.isOverwatchActive = true;
            stats.hasActed = true;
            stats.hasMoved = true;
            gameHUD.snapHP(stats.currentHP, stats.maxHP); // Refresh UI
            deselect();
        }
    }

    private void executeTargetingAbility(int tx, int ty) {
        if (targetingAbilityKey.equals("LAUNCH_NUKE")) {
            StatsComponent tStats = targetingUnit != null ? targetingUnit.getComponent(StatsComponent.class) : null;
            String name = tStats != null ? tStats.name : "?";
            int owner = tStats != null ? tStats.owner : 0;
            GameLogger.log(GameLogger.ABILITY, owner,
                    "NUKE launched by " + name + " → target " + GameLogger.pos(tx, ty));
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

    private void handleTerrainSelection(int x, int y, MapGenerator.TerrainType terrain) {
        MapGenerator.ObjectType obj = gameMap.objects[x][y];

        // If there's a blocking object that isn't Oil, just show terrain info
        if (obj != MapGenerator.ObjectType.NONE && obj != MapGenerator.ObjectType.OIL) {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
            return;
        }

        int owner = screen.getActiveLocalPlayer();

        // --- 1. TERRITORY CHECK (Critical for Build Menu) ---
        // [0]=isTerritory(1/0), [1]=maxLevel, [2]=parentX, [3]=parentY
        int[] territory = findControllingBase(x, y, owner);

        // --- 3. BUILD MENU OR TERRAIN INFO ---
        if (territory[0] == 1) {
            boolean isWater = (terrain == MapGenerator.TerrainType.WATER
                    || terrain == MapGenerator.TerrainType.DEEP_WATER);
            boolean isCoastalWater = isWater && hasAdjacentLand(x, y);
            boolean isCoastalLand = !isWater && hasAdjacentWater(x, y);
            boolean hasBuildOptions = !isWater || isCoastalWater;

            if (hasBuildOptions) {
                gameHUD.openBuildMenu(x, y, owner, territory[1], isWater, isCoastalWater, isCoastalLand,
                        screen.getGameState(), territory[2], territory[3], terrain, unitFactory);
            } else {
                gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                        unitFactory.getTextureForTerrain(terrain.ordinal()));
            }
        } else {
            gameHUD.showTileInfo(unitFactory.getTerrainUi(terrain).name,
                    unitFactory.getTextureForTerrain(terrain.ordinal()));
        }
    }

    /**
     * Returns the highest-level friendly base whose vision radius covers (tx, ty),
     * defaulting to 1 if none found. Used to determine port summon tier.
     */
    private int findMaxBaseLevelNear(int tx, int ty, int owner) {
        int maxLevel = 1;
        ImmutableArray<Entity> allEnts = engine.getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class).get());
        for (Entity e : allEnts) {
            StatsComponent bs = e.getComponent(StatsComponent.class);
            GridPositionComponent bp = e.getComponent(GridPositionComponent.class);
            if (bs.owner == owner && bs.income >= 2 && bs.name.contains("Base")) {
                if (Math.abs(bp.x - tx) <= bs.vision && Math.abs(bp.y - ty) <= bs.vision) {
                    if (bs.level > maxLevel) maxLevel = bs.level;
                }
            }
        }
        return maxLevel;
    }

    /**
     * Finds the highest-level friendly base whose territory covers (tx, ty).
     * Returns [isTerritory(1/0), maxLevel, parentX, parentY].
     */
    private int[] findControllingBase(int tx, int ty, int owner) {
        int maxLevel = 0, parentX = -1, parentY = -1;
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(StatsComponent.class, GridPositionComponent.class).get());
        for (Entity e : entities) {
            StatsComponent stats = e.getComponent(StatsComponent.class);
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (stats.owner == owner && stats.income >= 2 && stats.name.contains("Base")) {
                if (Math.abs(pos.x - tx) <= stats.vision && Math.abs(pos.y - ty) <= stats.vision) {
                    if (stats.level > maxLevel) {
                        maxLevel = stats.level;
                        parentX = pos.x;
                        parentY = pos.y;
                    }
                }
            }
        }
        return new int[]{ maxLevel > 0 ? 1 : 0, maxLevel, parentX, parentY };
    }

    private boolean hasAdjacentLand(int x, int y) {
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
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

    private boolean hasAdjacentWater(int x, int y) {
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                if (t == MapGenerator.TerrainType.WATER || t == MapGenerator.TerrainType.DEEP_WATER)
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
        Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0),
                viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        // Inverse isometric projection — see touchDown for formula derivation
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
            Vector3 oldWorld = camera.unproject(new Vector3(lastTouchX, lastTouchY, 0),
                    viewport.getScreenX(), viewport.getScreenY(),
                    viewport.getScreenWidth(), viewport.getScreenHeight());
            Vector3 newWorld = camera.unproject(new Vector3(screenX, screenY, 0),
                    viewport.getScreenX(), viewport.getScreenY(),
                    viewport.getScreenWidth(), viewport.getScreenHeight());
            camera.translate(oldWorld.x - newWorld.x, oldWorld.y - newWorld.y);
            lastTouchX = screenX;
            lastTouchY = screenY;
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
        AudioManager.getInstance().playSFX(SFXKeys.TILE_CLICK);
    }

    private void moveUnit(Entity unit, int targetX, int targetY) {
        GridPositionComponent pos = unit.getComponent(GridPositionComponent.class);
        if (pos == null)
            return;
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        String unitName = (stats != null) ? stats.name : "?";
        int owner = (stats != null) ? stats.owner : 0;
        int oldX = pos.x, oldY = pos.y;
        GameLogger.log(GameLogger.MOVE, owner,
                unitName + " moves " + GameLogger.move(oldX, oldY, targetX, targetY));
        unit.add(new MovementComponent(pos.x, pos.y, targetX, targetY));
        pos.x = targetX;
        pos.y = targetY;

        // RANGER: Overwatch (Check if move triggers an enemy attack)
        combatSystem.checkOverwatch(unit, targetX, targetY);
        if (stats != null) {
            stats.hasActed = true;
            stats.hasMoved = true;

            // Play Movement SFX
            if (stats.moveType == StatsComponent.MoveType.AIR) {
                if (stats.unitType == UnitType.APACHE) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_AIR_HELICOPTER);
                } else if (stats.unitType == UnitType.RECON_DRONE
                        || stats.unitType == UnitType.SUICIDE_DRONE) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_AIR_DRONE);
                } else {
                    // B2 and any future air units
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_AIR_B2);
                }
            } else if (stats.moveType == StatsComponent.MoveType.SEA) {
                AudioManager.getInstance().playSFX(SFXKeys.MOVE_WATER);
            } else {
                // LAND — each weight class has its own sound
                if (stats.unitType == UnitType.JUGGERNAUT) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_HEAVY_JUGGERNAUT);
                } else if (stats.unitType == UnitType.TANK) {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_HEAVY_TANK);
                } else {
                    AudioManager.getInstance().playSFX(SFXKeys.MOVE_LAND_LIGHT);
                }
            }
        }

        AbilitiesComponent abilities = unit.getComponent(AbilitiesComponent.class);
        if (abilities != null) {
            abilities.isDiggingIn = false;
            abilities.pendingSkirmishMove = false;
        }
        clearMarkers();
        selectedUnitEntity = null;
    }

    private void transformUnit(Entity unit, int targetX, int targetY) {
        StatsComponent stats = unit.getComponent(StatsComponent.class);
        if (stats == null) return;

        UnitType targetType = null;
        if (stats.unitType == UnitType.GUNBOAT) targetType = UnitType.RANGER;
        else if (stats.unitType == UnitType.DESTROYER) targetType = UnitType.TANK;

        if (targetType == null) return;

        int owner = stats.owner;
        int currentHP = stats.currentHP;
        String oldName = stats.name;

        GameLogger.log(GameLogger.MOVE, owner,
                oldName + " transforms into " + targetType.name() + " at " + GameLogger.pos(targetX, targetY));

        // Remove old unit
        engine.removeEntity(unit);

        // Create new unit
        unitFactory.createUnit(targetType, targetX, targetY, owner, true);

        // Update HP to match old unit
        Entity newUnit = getEntityAt(targetX, targetY, TypeComponent.Type.UNIT);
        if (newUnit != null) {
            StatsComponent newStats = newUnit.getComponent(StatsComponent.class);
            if (newStats != null) {
                newStats.currentHP = currentHP;
            }
        }

        AudioManager.getInstance().playSFX(SFXKeys.UNIT_DEPLOY);
        gameHUD.hideSummonMenu();
        gameHUD.hideTileInfo();
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

        // GUNBOAT: Skirmish — cap move to 1 tile after attacking
        AbilitiesComponent selectedAbilities = selectedUnitEntity.getComponent(AbilitiesComponent.class);
        if (selectedAbilities != null && selectedAbilities.pendingSkirmishMove) {
            moveRange = 1;
        }

        // Move range SFX — play once per marker reveal
        AudioManager.getInstance().playSFX(SFXKeys.TILE_MOVE_RANGE);

        // --- Blue move markers ---
        // Juggernaut always jumps — skip flood fill so red attack markers aren't shadowed
        boolean isJuggernautSelected = stats.unitType == UnitType.JUGGERNAUT;
        if (!stats.hasMoved && !isJuggernautSelected) {
            int[][] visitedMoves = new int[gameMap.width][gameMap.height];
            for (int[] row : visitedMoves)
                java.util.Arrays.fill(row, -1);
            floodFill(startX, startY, moveRange, visitedMoves, startX, startY, moveType);
        }

        // --- Red attack markers ---
        // Enumerate all tiles within Chebyshev distance == attackRange.
        // Skip tiles occupied by own units or by the attacker itself.
        int attackMarkersAdded = 0;
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
                    if (ts != null && ts.owner == screen.getActiveLocalPlayer())
                        continue; // skip own units
                }
                // Juggernaut can jump to any tile (empty or enemy); others need an actual enemy
                boolean isJuggernaut = stats != null && stats.unitType == UnitType.JUGGERNAUT;
                if (tileUnit == null && !isJuggernaut)
                    continue;
                // Juggernaut cannot jump onto water
                if (isJuggernaut) {
                    MapGenerator.TerrainType t = gameMap.terrain[tx][ty];
                    if (t == MapGenerator.TerrainType.WATER || t == MapGenerator.TerrainType.DEEP_WATER)
                        continue;
                }
                entityFactory.createAttackMarker(tx, ty);
                attackMarkersAdded++;
            }
        }

        // Attack range SFX — only play if at least one attack marker was actually spawned
        if (attackMarkersAdded > 0) {
            AudioManager.getInstance().playSFX(SFXKeys.TILE_ATTACK_RANGE);
        }

        // --- Transformation markers: Gunboat/Destroyer near land ---
        if (!stats.hasMoved && (stats.unitType == UnitType.GUNBOAT || stats.unitType == UnitType.DESTROYER)) {
            int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 }, { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
            for (int[] d : dirs) {
                int nx = startX + d[0];
                int ny = startY + d[1];
                if (nx >= 0 && nx < gameMap.width && ny >= 0 && ny < gameMap.height) {
                    MapGenerator.TerrainType t = gameMap.terrain[nx][ny];
                    // If it's land and no unit is there
                    if (t != MapGenerator.TerrainType.WATER && t != MapGenerator.TerrainType.DEEP_WATER) {
                        if (getEntityAt(nx, ny, TypeComponent.Type.UNIT) == null) {
                            entityFactory.createTransformMarker(nx, ny);
                        }
                    }
                }
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

    private Entity getEntityAtLayer(int x, int y, int zIndex) {
        ImmutableArray<Entity> entities = engine.getEntitiesFor(
                Family.all(GridPositionComponent.class).get());
        for (Entity e : entities) {
            GridPositionComponent pos = e.getComponent(GridPositionComponent.class);
            if (pos.x == x && pos.y == y && pos.zIndex == zIndex)
                return e;
        }
        return null;
    }

    private void handleDevTouch(int gridX, int gridY) {
        switch (devSpawnMode) {
            case SPAWNING_UNIT: {
                Entity existingUnit = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (existingUnit != null) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                }

                Entity existingObj = getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT);
                if (existingObj != null) {
                    StatsComponent s = existingObj.getComponent(StatsComponent.class);
                    if (s != null && s.owner > 0 && s.owner != devSpawnOwner) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                }

                MapGenerator.TerrainType terrain = gameMap.terrain[gridX][gridY];
                StatsComponent.MoveType moveType = unitFactory.getUnitMoveType(devSpawnUnitType);
                boolean isWater = (terrain == MapGenerator.TerrainType.WATER || terrain == MapGenerator.TerrainType.DEEP_WATER);

                if (moveType == StatsComponent.MoveType.LAND && isWater) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                } else if (moveType == StatsComponent.MoveType.SEA && !isWater) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                }

                unitFactory.createUnit(devSpawnUnitType, gridX, gridY, devSpawnOwner, false);
                GameLogger.log(GameLogger.INPUT, "[DEV] Spawned " + devSpawnUnitType
                        + " at (" + gridX + "," + gridY + ") for P" + devSpawnOwner);
                break;
            }
            case BUILDING_STRUCTURE: {
                Entity existingStructure = getEntityAtLayer(gridX, gridY, 1);
                MapGenerator.ObjectType existingMapObj = gameMap.objects[gridX][gridY];
                boolean isOilTile = (existingMapObj == MapGenerator.ObjectType.OIL);

                if (existingStructure != null) {
                    StatsComponent stats = existingStructure.getComponent(StatsComponent.class);
                    boolean isOilRes = (stats != null && stats.name.equals("Oil Reservoir"));
                    if (!(devSpawnStructureType.equals("OIL_DERRICK") && isOilRes)) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else if (existingMapObj != null && existingMapObj != MapGenerator.ObjectType.NONE && !isOilTile) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    return;
                }

                MapGenerator.TerrainType buildTerrain = gameMap.terrain[gridX][gridY];
                boolean buildIsWater = (buildTerrain == MapGenerator.TerrainType.WATER || buildTerrain == MapGenerator.TerrainType.DEEP_WATER);
                boolean buildIsCoastalWater = buildIsWater && hasAdjacentLand(gridX, gridY);
                boolean buildIsCoastalLand = !buildIsWater && hasAdjacentWater(gridX, gridY);

                if (devSpawnStructureType.equals("PORT")) {
                    if (!buildIsWater || !buildIsCoastalWater) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else if (devSpawnStructureType.equals("NUCLEAR")) {
                    if (buildIsWater || !buildIsCoastalLand) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else if (devSpawnStructureType.equals("OIL_DERRICK")) {
                    if (!isOilTile) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                } else {
                    if (buildIsWater) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        return;
                    }
                }

                if (devSpawnStructureType.equals("OIL_DERRICK") && isOilTile) {
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.NONE;
                    if (existingStructure != null) {
                        engine.removeEntity(existingStructure);
                    }
                }

                if (devSpawnStructureType.equals("BASE")) {
                    MapGenerator.ObjectType baseType = (devSpawnOwner == 1) ? MapGenerator.ObjectType.BASE_P1 : MapGenerator.ObjectType.BASE_P2;
                    gameMap.objects[gridX][gridY] = baseType;
                    unitFactory.createObjectEntity(gridX, gridY, baseType, screen.getGameState());
                } else if (devSpawnStructureType.equals("TOWN")) {
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.TOWN;
                    unitFactory.createObjectEntity(gridX, gridY, MapGenerator.ObjectType.TOWN, screen.getGameState());
                } else {
                    unitFactory.createStructure(devSpawnStructureType, gridX, gridY, devSpawnOwner, -1, -1);
                }

                GameLogger.log(GameLogger.INPUT, "[DEV] Built " + devSpawnStructureType
                        + " at (" + gridX + "," + gridY + ") for P" + devSpawnOwner);
                break;
            }
            case KILLING_UNIT:
                Entity unitAtTile = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitAtTile != null) {
                    engine.removeEntity(unitAtTile);
                    GameLogger.log(GameLogger.INPUT, "[DEV] Killed unit at (" + gridX + "," + gridY + ")");
                }
                break;
            case REMOVING_OBJECT:
                boolean removedSomething = false;
                Entity unitToRemove = getEntityAt(gridX, gridY, TypeComponent.Type.UNIT);
                if (unitToRemove != null) {
                    engine.removeEntity(unitToRemove);
                    removedSomething = true;
                }
                Entity objToRemove = getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT);
                if (objToRemove != null) {
                    engine.removeEntity(objToRemove);
                    removedSomething = true;
                }
                // Always clear the tile's object layer in the map data, even if it's just a tree
                if (gameMap.objects[gridX][gridY] != MapGenerator.ObjectType.NONE) {
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.NONE;
                    removedSomething = true;
                }
                if (removedSomething) {
                    GameLogger.log(GameLogger.INPUT, "[DEV] Removed object/unit at (" + gridX + "," + gridY + ")");
                    AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                }
                break;
            case BUILDING_MAP_OBJ:
                // Check terrain validation
                boolean objIsWater = devSpawnMapObjType.equals("FISH") || devSpawnMapObjType.equals("OIL") || devSpawnMapObjType.equals("RUINS");
                boolean tileIsWater = (gameMap.terrain[gridX][gridY] == MapGenerator.TerrainType.WATER || gameMap.terrain[gridX][gridY] == MapGenerator.TerrainType.DEEP_WATER);

                if (objIsWater != tileIsWater) {
                    AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                    break;
                }

                boolean isAnimal = (com.militopia.config.AnimalType.fromKey(devSpawnMapObjType) != null);

                // Check stacking rules: non-animals cannot be placed if there's already any object (static or animal)
                if (!isAnimal) {
                    Entity animalAtTile = getEntityAtLayer(gridX, gridY, 2);
                    if (gameMap.objects[gridX][gridY] != MapGenerator.ObjectType.NONE || animalAtTile != null) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        break;
                    }
                }

                if (isAnimal) {
                    // Prevent animal stacking: cannot place an animal if one already exists
                    Entity existAnimal = getEntityAtLayer(gridX, gridY, 2);
                    if (existAnimal != null) {
                        AudioManager.getInstance().playSFX(SFXKeys.UI_ERROR);
                        break;
                    }
                }

                if (devSpawnMapObjType.equals("MOUNTAIN")) {
                    gameMap.terrain[gridX][gridY] = MapGenerator.TerrainType.MOUNTAIN;
                    gameMap.objects[gridX][gridY] = MapGenerator.ObjectType.MOUNTAIN_OBJ;
                    unitFactory.createObjectEntity(gridX, gridY, MapGenerator.ObjectType.MOUNTAIN_OBJ, screen.getGameState());
                } else {
                    MapGenerator.ObjectType objType = MapGenerator.ObjectType.valueOf(devSpawnMapObjType);
                    
                    if (!isAnimal) {
                        gameMap.objects[gridX][gridY] = objType;
                    }
                    
                    unitFactory.createObjectEntity(gridX, gridY, objType, screen.getGameState());
                }
                
                GameLogger.log(GameLogger.INPUT, "[DEV] Built Map Obj " + devSpawnMapObjType
                        + " at (" + gridX + "," + gridY + ")");
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                break;
            case SETTING_BASE_LEVEL:
                Entity baseAtTile = getEntityAt(gridX, gridY, TypeComponent.Type.OBJECT);
                if (baseAtTile != null) {
                    StatsComponent s = baseAtTile.getComponent(StatsComponent.class);
                    if (s != null && s.level > 0) {
                        screen.devOpenBaseLevelPicker(baseAtTile);
                    }
                }
                break;
            default:
                break;
        }
    }

}
