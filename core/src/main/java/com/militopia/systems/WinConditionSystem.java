package com.militopia.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.militopia.MilitopiaGame;
import com.militopia.data.GameState;
import com.militopia.utils.GameLogger;

/**
 * WinConditionSystem — Ashley ECS system responsible for monitoring the game
 * state
 * and triggering the Game Over screen when a victory condition is met.
 *
 * This decouples the win/loss check from GameScreen.render().
 */
public class WinConditionSystem extends EntitySystem {

    /**
     * Functional interface for triggering the game over state.
     * This improves testability by allowing mocks in headless environments.
     */
    public interface GameOverTrigger {
        void trigger(int winnerID);
    }

    private final GameState gameState;
    private final GameOverTrigger trigger;
    private boolean isPlaying = true;

    public WinConditionSystem(GameState gameState, GameOverTrigger trigger) {
        // Priority 10 — runs after most other systems
        super(10);
        this.gameState = gameState;
        this.trigger = trigger;
    }

    /**
     * Enables or disables the win condition check.
     */
    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    @Override
    public void update(float deltaTime) {
        if (!isPlaying)
            return;

        // Check win conditions
        if (gameState.p1BaseCount == 0) {
            triggerGameOver(2); // Player 2 wins
        } else if (gameState.p2BaseCount == 0) {
            triggerGameOver(1); // Player 1 wins
        }
    }

    private void triggerGameOver(int winnerID) {
        GameLogger.log(GameLogger.GAME_OVER,
                "=== P" + winnerID + " WINS on Turn " + gameState.turnCount + " (Opponent lost all bases) ===");

        // Ensure we don't trigger multiple times
        isPlaying = false;
        gameState.isGameOver = true;
        gameState.winnerID = winnerID;

        trigger.trigger(winnerID);
    }
}
