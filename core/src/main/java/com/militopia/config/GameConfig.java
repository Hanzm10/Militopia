package com.militopia.config;

public class GameConfig {
    // Grid Settings
    public static final int MAP_WIDTH = 32;
    public static final int MAP_HEIGHT = 32;
    public static final int TILE_WIDTH = 25;
    public static final int TILE_HEIGHT = 15;
    
    // Drawing Settings
    public static final float DRAW_WIDTH = 30f;
    public static final float DRAW_HEIGHT = 30f;
    
    // Animation Settings
    public static final float BOUNCE_DURATION = 0.25f;
    public static final float BOUNCE_HEIGHT = 5f;
    
    // mouse/touch x & y adjustment Settings
    public static final float INPUT_OFFSET_Y = -12f;
    public static final float INPUT_OFFSET_X = -12f;
    
    // Territory Settings
    public static final int BORDER_RADIUS = 2; // "2 tiles wide"
    
    // Camera Settings
    public static final float ZOOM_MIN = 0.2f; // Closest (Zoomed In)
    public static final float ZOOM_MAX = 2.0f; // Furthest (Zoomed Out)
    public static final float ZOOM_SPEED = 0.1f; // How fast scrolling zooms
}