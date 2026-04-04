package com.militopia.map;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapGenerator {

    public enum TerrainType {
        DEEP_WATER, WATER, SAND, GRASS, MOUNTAIN
    }

    public enum ObjectType {
        NONE,
        BASE_P1, BASE_P2,
        BASE_P1_LVL2, BASE_P2_LVL2,
        BASE_P1_LVL3, BASE_P2_LVL3,
        BASE_P1_LVL4, BASE_P2_LVL4,
        BASE_P1_LVL5, BASE_P2_LVL5,
        BASE_P1_LVL6, BASE_P2_LVL6,
        BASE_P1_LVL7, BASE_P2_LVL7,
        BASE_P1_LVL8, BASE_P2_LVL8,
        BASE_P1_LVL9, BASE_P2_LVL9,
        BASE_P1_LVL10, BASE_P2_LVL10,
        TOWN,
        OIL, RUINS, CACTUS, TREE, MOUNTAIN_OBJ,
        HORSE, FISH, DEER, ZEBRA
    }

    public static class GameMap {
        public TerrainType[][] terrain;
        public ObjectType[][] objects;
        public boolean[][] visibleTiles;
        public boolean[][] detectedTiles; // NEW: For stealth detection
        public boolean[][] rails;
        public int width, height;

        public GameMap(int w, int h) {
            this.width = w;
            this.height = h;
            terrain = new TerrainType[w][h];
            objects = new ObjectType[w][h];
            visibleTiles = new boolean[w][h];
            detectedTiles = new boolean[w][h];
            rails = new boolean[w][h];
        }
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // --- CONFIGURATION ---
    private static final int MAP_MARGIN = 3; // Rule 4: At least 3 tiles from edges

    public GameMap generateMap(int width, int height, long seed) {
        GameMap map = new GameMap(width, height);
        SimpleNoise noise = new SimpleNoise(seed);
        MathUtils.random.setSeed(seed);

        List<Point> grassTiles = new ArrayList<>();

        // 1. TERRAIN PASS
        generateTerrain(map, width, height, noise, grassTiles);

        // 2. PLAYER BASES PASS (Far apart + On Land + Margins)
        List<Point> bases = placePlayerBases(map, width, height);

        // 3. FLORA PASS (Trees, Cacti - No Random Oil anymore)
        placeFlora(map, width, height);

        // 4. STRUCTURES PASS (Towns spaced apart + Margins)
        List<Point> towns = placeStructures(map, width, height, seed, grassTiles);

        // 5. RESOURCE PASS (Mandatory Oil near Settlements)
        List<Point> allSettlements = new ArrayList<>(bases);
        allSettlements.addAll(towns);

        placeResourcesAround(map, allSettlements);

        return map;
    }

    // -------------------------------------------------------------------------
    // GENERATION STEPS
    // -------------------------------------------------------------------------

    private void generateTerrain(GameMap map, int width, int height, SimpleNoise noise, List<Point> grassTiles) {
        // scale controls feature size: lower = larger landmasses, higher = noisier/smaller islands
        float scale = 0.15f;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // SimpleNoise (Simplex) returns values in roughly [-1, 1]
                double value = noise.eval(x * scale, y * scale);
                TerrainType type;

                // Threshold bands map noise range to terrain types:
                //   value < -0.40  →  deep ocean
                //   -0.40 to -0.15 →  shallow water
                //   -0.15 to  0.00 →  coastal sand
                //    0.00 to  0.50 →  grass (most common land)
                //    0.50+         →  mountain ridges
                if (value < -0.4)
                    type = TerrainType.DEEP_WATER;
                else if (value < -0.15)
                    type = TerrainType.WATER;
                else if (value < 0.001)
                    type = TerrainType.SAND;
                else if (value < 0.5)
                    type = TerrainType.GRASS;
                else
                    type = TerrainType.MOUNTAIN;

                map.terrain[x][y] = type;
                map.objects[x][y] = ObjectType.NONE;

                if (type == TerrainType.GRASS)
                    grassTiles.add(new Point(x, y));
                if (type == TerrainType.MOUNTAIN)
                    map.objects[x][y] = ObjectType.MOUNTAIN_OBJ;
            }
        }
    }

    private List<Point> placePlayerBases(GameMap map, int width, int height) {
        List<Point> baseLocations = new ArrayList<>();

        // --- 1. Place Player 1 Base ---
        // Constraint: Within top-left quadrant, but respecting margins
        int p1x = MathUtils.random(MAP_MARGIN, width / 3);
        int p1y = MathUtils.random(MAP_MARGIN, height - MAP_MARGIN);

        Point p1Point = findNearestLand(map, p1x, p1y);
        setBase(map, p1Point.x, p1Point.y, ObjectType.BASE_P1);
        baseLocations.add(p1Point);

        // --- 2. Place Player 2 Base ---
        Point p2Point = null;
        int attempts = 0;
        float minDistance = (width + height) / 2.5f;

        while (p2Point == null && attempts < 20) {
            int rx = MathUtils.random(width / 2, width - MAP_MARGIN);
            int ry = MathUtils.random(MAP_MARGIN, height - MAP_MARGIN);

            if (Vector2.dst(p1Point.x, p1Point.y, rx, ry) > minDistance) {
                p2Point = findNearestLand(map, rx, ry);
            }
            attempts++;
        }

        // Fallback (Respecting margins)
        if (p2Point == null)
            p2Point = new Point(width - (MAP_MARGIN + 1), height - (MAP_MARGIN + 1));

        setBase(map, p2Point.x, p2Point.y, ObjectType.BASE_P2);
        baseLocations.add(p2Point);

        return baseLocations;
    }

    private List<Point> placeStructures(GameMap map, int width, int height, long seed, List<Point> grassTiles) {
        Collections.shuffle(grassTiles, new java.util.Random(seed));
        List<Point> towns = new ArrayList<>();
        int targetTowns = 8;

        for (Point p : grassTiles) {
            if (towns.size() >= targetTowns)
                break;

            // RULE 4 Check: Margin from edges
            if (p.x < MAP_MARGIN || p.x >= width - MAP_MARGIN ||
                    p.y < MAP_MARGIN || p.y >= height - MAP_MARGIN) {
                continue;
            }

            if (map.objects[p.x][p.y] != ObjectType.NONE && map.objects[p.x][p.y] != ObjectType.TREE)
                continue;

            boolean tooClose = false;
            for (Point t : towns) {
                if (Vector2.dst(p.x, p.y, t.x, t.y) < 2.5f) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                if (isNearBase(map, p.x, p.y))
                    tooClose = true;
            }

            if (!tooClose) {
                map.objects[p.x][p.y] = ObjectType.TOWN;
                towns.add(p);
            }
        }
        return towns;
    }

    private void placeResourcesAround(GameMap map, List<Point> centers) {
        for (Point center : centers) {

            // --- RULE 2: MANDATORY OIL (1-2 count) ---
            int oilCount = MathUtils.random(1, 2);
            spawnSpecificResource(map, center, oilCount, ObjectType.OIL, 1, 3); // Rule 1: 1-3 tiles

            // --- OPTIONAL: RUINS (0-1 count) ---
            // Kept for flavor, but less priority
            int ruinCount = MathUtils.random(0, 1);
            spawnSpecificResource(map, center, ruinCount, ObjectType.RUINS, 2, 4);
        }
    }

    private void spawnSpecificResource(GameMap map, Point center, int count, ObjectType type, int minDist,
            int maxDist) {
        int placed = 0;
        int attempts = 0;

        while (placed < count && attempts < 20) {
            attempts++;

            int dist = MathUtils.random(minDist, maxDist);
            int angle = MathUtils.random(0, 360);

            int tx = center.x + (int) (MathUtils.cosDeg(angle) * dist);
            int ty = center.y + (int) (MathUtils.sinDeg(angle) * dist);

            // Rule 3 Check: Allow generation (even on water) as long as it's empty
            if (isValid(map, tx, ty) && map.objects[tx][ty] == ObjectType.NONE) {
                map.objects[tx][ty] = type;
                placed++;
            }
        }
    }

    private void placeFlora(GameMap map, int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (map.objects[x][y] != ObjectType.NONE)
                    continue;

                TerrainType t = map.terrain[x][y];
                float chance = MathUtils.random();

                if (t == TerrainType.GRASS && chance < 0.20f) {
                    map.objects[x][y] = ObjectType.TREE;
                } else if (t == TerrainType.SAND) {
                    if (chance < 0.05f)
                        map.objects[x][y] = ObjectType.CACTUS;
                    // Rule 1 Enforcement: REMOVED random Oil generation here.
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // UTILITIES
    // -------------------------------------------------------------------------

    private Point findNearestLand(GameMap map, int startX, int startY) {
        if (isLand(map, startX, startY))
            return new Point(startX, startY);

        int maxRadius = Math.max(map.width, map.height);
        for (int r = 1; r < maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                if (checkLand(map, startX + dx, startY - r))
                    return new Point(startX + dx, startY - r);
                if (checkLand(map, startX + dx, startY + r))
                    return new Point(startX + dx, startY + r);
            }
            for (int dy = -r + 1; dy < r; dy++) {
                if (checkLand(map, startX - r, startY + dy))
                    return new Point(startX - r, startY + dy);
                if (checkLand(map, startX + r, startY + dy))
                    return new Point(startX + r, startY + dy);
            }
        }
        return new Point(startX, startY);
    }

    private boolean checkLand(GameMap map, int x, int y) {
        if (!isValid(map, x, y))
            return false;
        return isLand(map, x, y);
    }

    private boolean isLand(GameMap map, int x, int y) {
        TerrainType t = map.terrain[x][y];
        return t == TerrainType.GRASS || t == TerrainType.SAND || t == TerrainType.MOUNTAIN;
    }

    private boolean isValid(GameMap map, int x, int y) {
        return x >= 0 && x < map.width && y >= 0 && y < map.height;
    }

    private boolean isNearBase(GameMap map, int x, int y) {
        for (int i = 0; i < map.width; i++) {
            for (int j = 0; j < map.height; j++) {
                if (map.objects[i][j] == ObjectType.BASE_P1 || map.objects[i][j] == ObjectType.BASE_P2) {
                    if (Vector2.dst(x, y, i, j) < 5)
                        return true;
                }
            }
        }
        return false;
    }

    private void setBase(GameMap map, int x, int y, ObjectType type) {
        map.terrain[x][y] = TerrainType.GRASS;
        map.objects[x][y] = type;
        clearObj(map, x + 1, y);
        clearObj(map, x - 1, y);
        clearObj(map, x, y + 1);
        clearObj(map, x, y - 1);
    }

    private void clearObj(GameMap map, int x, int y) {
        if (isValid(map, x, y))
            map.objects[x][y] = ObjectType.NONE;
    }
}