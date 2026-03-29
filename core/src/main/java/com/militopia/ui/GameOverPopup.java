package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.screen.GameScreen;
import com.militopia.screen.MenuScreen;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

/**
 * Modal "Game Over" popup.
 *
 * Displays the winner and providing a button to return to the main menu.
 * Blocks all map and HUD input while visible.
 */
public class GameOverPopup {

    private final Stage stage;
    private final MilitopiaGame game;
    private final GameScreen gameScreen;
    private final GameInputController inputController;
    private final HudBottomBar bottomBar;

    private Table popupTable;

    public GameOverPopup(MilitopiaGame game, GameScreen gameScreen, Stage stage,
            GameInputController inputController, HudBottomBar bottomBar) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    public void show(int winnerID, GameState state) {
        buildPopup(winnerID, state);

        // Explicitly set bounds to ensure input blocking on first frame
        popupTable.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        stage.addActor(popupTable);
        popupTable.toFront();

        // Lock inputs
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);
    }

    private void buildPopup(int winnerID, GameState state) {
        popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));

        // Block all input
        popupTable.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent e, float x, float y) {
                return true;
            }

            @Override
            public boolean keyDown(InputEvent e, int keycode) {
                return true;
            }
        });

        Table modal = new Table();

        Label title = new Label("GAME OVER", game.skin, "default-font", Color.WHITE);
        title.setFontScale(2.5f);
        modal.add(title).padBottom(20).row();

        String winnerName = (winnerID == 1) ? state.p1Name : state.p2Name;
        Color winnerColor = (winnerID == 1) ? Color.CYAN : Color.RED;
        Label winnerLabel = new Label(winnerName + " VICTORIOUS!", game.skin, "default-font", winnerColor);
        winnerLabel.setFontScale(1.2f);
        modal.add(winnerLabel).padBottom(40).row();

        TextButton menuBtn = new TextButton("Return to Main Menu", game.skin, "militopia-btn");
        menuBtn.addListener(new HoverListener());
        menuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                GameLogger.logScreen("Navigating → Main Menu from Game Over Popup (Saving Game)");
                gameScreen.saveAndExit();
            }
        });
        modal.add(menuBtn).fillX().width(280);

        popupTable.add(modal);
    }
}
