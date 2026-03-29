package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.managers.AudioManager;
import com.militopia.managers.SFXKeys;

public class GameStatsPopup {
    private final Stage stage;
    private final MilitopiaGame game;
    private final GameInputController inputController;
    private final HudBottomBar bottomBar;

    public GameStatsPopup(MilitopiaGame game, Stage stage, GameInputController inputController, HudBottomBar bottomBar) {
        this.game = game;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    public void show(GameState state) {
        final Table popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));

        Table modal = new Table();
        modal.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        modal.pad(30);

        Label title = new Label("GAME STATS (TURN " + state.turnCount + ")", game.skin, "default-font", Color.WHITE);
        title.setFontScale(1.5f);
        modal.add(title).padBottom(20).colspan(2).row();

        // Player 1
        Table p1Table = new Table();
        Label p1Title = new Label(state.p1Name.toUpperCase(), game.skin, "default-font", Color.CYAN);
        p1Table.add(p1Title).padBottom(10).row();
        p1Table.add(new Label("Funds: $" + state.p1Funding, game.skin, "default-font", Color.WHITE)).row();
        p1Table.add(new Label("Global XP: " + state.p1XP, game.skin, "default-font", Color.WHITE)).row();
        p1Table.add(new Label("Bases: " + state.p1BaseCount, game.skin, "default-font", Color.WHITE)).row();
        
        // Player 2
        Table p2Table = new Table();
        Label p2Title = new Label(state.p2Name.toUpperCase(), game.skin, "default-font", Color.RED);
        p2Table.add(p2Title).padBottom(10).row();
        p2Table.add(new Label("Funds: $" + state.p2Funding, game.skin, "default-font", Color.WHITE)).row();
        p2Table.add(new Label("Global XP: " + state.p2XP, game.skin, "default-font", Color.WHITE)).row();
        p2Table.add(new Label("Bases: " + state.p2BaseCount, game.skin, "default-font", Color.WHITE)).row();

        modal.add(p1Table).padRight(40);
        modal.add(p2Table).row();

        TextButton closeBtn = new TextButton("Close", game.skin, "militopia-btn");
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.getInstance().playSFX(SFXKeys.UI_CLICK_CONFIRM);
                popupTable.remove();
                // We rely on GameScreen to manage UI locks, but as a fallback trigger unlock
                inputController.setInputEnabled(true);
                bottomBar.setBlocked(false);
            }
        });

        modal.add(closeBtn).colspan(2).fillX().width(200).padTop(30);

        popupTable.add(modal);
        
        // Ensure input is disabled when this opens
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);

        stage.addActor(popupTable);
        popupTable.toFront();
    }
}
