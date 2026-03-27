package com.militopia.utils;

import com.badlogic.gdx.Gdx;

/**
 * Central action logger for Militopia.
 *
 * All player-facing game events are routed through this class so that
 * logic errors can be traced from the initial tile click to the end of the
 * game. Filter the Android/desktop console on the tag "ActionLog" to see
 * only these structured messages.
 *
 * Usage:
 * GameLogger.setContext(turn, player);
 * GameLogger.log(GameLogger.MOVE, "P1 moved Recruit (2,3) → (4,3)");
 */
public class GameLogger {

    // Log tag — filter on this in logcat / desktop console
    public static final String TAG = "ActionLog";

    // -----------------------------------------------------------------------
    // Categories
    // -----------------------------------------------------------------------
    public static final String SCREEN = "SCREEN";
    public static final String INPUT = "INPUT";
    public static final String MOVE = "MOVE";
    public static final String ATTACK = "ATTACK";
    public static final String ABILITY = "ABILITY";
    public static final String SUMMON = "SUMMON";
    public static final String BUILD = "BUILD";
    public static final String CAPTURE = "CAPTURE";
    public static final String SCAVENGE = "SCAVENGE";
    public static final String ECONOMY = "ECONOMY";
    public static final String UI = "UI";
    public static final String SFX = "SFX";
    public static final String CAMERA = "CAMERA";
    public static final String GAME_OVER = "GAME_OVER";

    // -----------------------------------------------------------------------
    // State (updated by GameScreen at each turn transition)
    // -----------------------------------------------------------------------
    private static int currentTurn = 0;
    private static int currentPlayer = 0;

    /** Called by GameScreen at the start of every new turn. */
    public static void setContext(int turn, int player) {
        currentTurn = turn;
        currentPlayer = player;
    }

    // -----------------------------------------------------------------------
    // Core log entry-points
    // -----------------------------------------------------------------------

    /**
     * Emits a log line with the current turn/player context.
     *
     * Format: [T{turn}|P{player}] [{category}] {message}
     */
    public static void log(String category, String message) {
        String entry = String.format("[T%d|P%d] [%s] %s",
                currentTurn, currentPlayer, category, message);
        if (Gdx.app != null) {
            Gdx.app.log(TAG, entry);
        } else {
            System.out.println(TAG + ": " + entry);
        }
    }

    /**
     * Emits a log line with an explicit player override (e.g. for counters
     * or events that belong to the non-active player).
     */
    public static void log(String category, int player, String message) {
        String entry = String.format("[T%d|P%d] [%s] %s",
                currentTurn, player, category, message);
        if (Gdx.app != null) {
            Gdx.app.log(TAG, entry);
        } else {
            System.out.println(TAG + ": " + entry);
        }
    }

    /**
     * Emits a context-free SCREEN log (no turn/player context).
     * Used for menu navigation before a game is running.
     */
    public static void logScreen(String message) {
        if (Gdx.app != null) {
            Gdx.app.log(TAG, "[SCREEN] " + message);
        } else {
            System.out.println(TAG + ": [SCREEN] " + message);
        }
    }

    // -----------------------------------------------------------------------
    // Convenience helpers
    // -----------------------------------------------------------------------

    /** Formats a grid coordinate as "(x,y)". */
    public static String pos(int x, int y) {
        return "(" + x + "," + y + ")";
    }

    /** Formats a move arrow: "(ox,oy) → (tx,ty)". */
    public static String move(int ox, int oy, int tx, int ty) {
        return pos(ox, oy) + " → " + pos(tx, ty);
    }
}
