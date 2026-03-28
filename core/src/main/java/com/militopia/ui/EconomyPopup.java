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
import java.util.ArrayList;
import java.util.List;

public class EconomyPopup {
    private final Stage stage;
    private final MilitopiaGame game;
    private final GameInputController inputController;
    private final HudBottomBar bottomBar;
    private final List<Table> popups = new ArrayList<>();

    public EconomyPopup(MilitopiaGame game, Stage stage, GameInputController inputController, HudBottomBar bottomBar) {
        this.game = game;
        this.stage = stage;
        this.inputController = inputController;
        this.bottomBar = bottomBar;
    }

    public void show(int turnCount, int income, int xpGain, int currentFunds) {
        final Table popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));

        Table modal = new Table();
        modal.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        modal.pad(30);

        Label title = new Label("TURN " + turnCount + " REPORT", game.skin, "default-font", Color.GOLD);
        title.setFontScale(1.5f);
        modal.add(title).padBottom(20).row();

        Label incomeLbl = new Label("Income Generated: +$" + income, game.skin, "default-font", Color.GREEN);
        Label xpLbl = new Label("Total XP Gained: +" + xpGain, game.skin, "default-font", Color.CYAN);
        Label fundsLbl = new Label("Current Funds: $" + currentFunds, game.skin, "default-font", Color.WHITE);

        modal.add(incomeLbl).padBottom(5).row();
        modal.add(xpLbl).padBottom(15).row();
        modal.add(fundsLbl).row();

        TextButton closeBtn = new TextButton("Awesome", game.skin);
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                popups.remove(popupTable);
                popupTable.remove();
                
                // Only unlock UI if no other popups (like level ups) are visible
                if (popups.isEmpty()) {
                    inputController.setInputEnabled(true);
                    bottomBar.setBlocked(false);
                }
            }
        });

        modal.add(closeBtn).padTop(25).size(150, 40);

        popupTable.add(modal);
        popups.add(popupTable);

        // Lock map interactions while looking at the report
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);

        stage.addActor(popupTable);
        popupTable.toFront();
    }
}
