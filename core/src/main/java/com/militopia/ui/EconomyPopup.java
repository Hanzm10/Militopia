package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.controller.GameInputController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public void show(int turnCount, int income, int xpGain, int currentFunds,
                     LinkedHashMap<String, Integer> incomeBreakdown,
                     LinkedHashMap<String, Integer> xpBreakdown) {
        final Table popupTable = new Table();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.85f)));

        Table modal = new Table();
        modal.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        modal.pad(30);

        Label title = new Label("TURN " + turnCount + " REPORT", game.skin, "default-font", Color.GOLD);
        title.setFontScale(1.5f);
        modal.add(title).padBottom(20).row();

        // Income section
        Label incomeLbl = new Label("Income Generated: +$" + income, game.skin, "default-font", Color.GREEN);
        incomeLbl.setFontScale(1.1f);
        modal.add(incomeLbl).left().row();

        if (incomeBreakdown != null && !incomeBreakdown.isEmpty()) {
            Table incomeTable = new Table();
            for (Map.Entry<String, Integer> entry : incomeBreakdown.entrySet()) {
                Label nameLbl = new Label(entry.getKey(), game.skin, "default-font", Color.LIGHT_GRAY);
                nameLbl.setFontScale(0.75f);
                nameLbl.setAlignment(Align.left);
                Label amtLbl = new Label("+$" + entry.getValue(), game.skin, "default-font", Color.LIGHT_GRAY);
                amtLbl.setFontScale(0.75f);
                amtLbl.setAlignment(Align.right);
                incomeTable.add(nameLbl).width(140).left().padBottom(2);
                incomeTable.add(amtLbl).width(140).right().padBottom(2).row();
            }
            modal.add(incomeTable).padLeft(16).padBottom(10).left().row();
        } else {
            modal.add().padBottom(5).row();
        }

        // XP section
        Label xpLbl = new Label("XP Gained: +" + xpGain, game.skin, "default-font", Color.CYAN);
        xpLbl.setFontScale(1.1f);
        modal.add(xpLbl).left().row();

        if (xpBreakdown != null && !xpBreakdown.isEmpty()) {
            Table xpTable = new Table();
            for (Map.Entry<String, Integer> entry : xpBreakdown.entrySet()) {
                Label nameLbl = new Label(entry.getKey(), game.skin, "default-font", Color.LIGHT_GRAY);
                nameLbl.setFontScale(0.75f);
                nameLbl.setAlignment(Align.left);
                Label amtLbl = new Label("+" + entry.getValue(), game.skin, "default-font", Color.LIGHT_GRAY);
                amtLbl.setFontScale(0.75f);
                amtLbl.setAlignment(Align.right);
                xpTable.add(nameLbl).width(140).left().padBottom(2);
                xpTable.add(amtLbl).width(140).right().padBottom(2).row();
            }
            modal.add(xpTable).padLeft(16).padBottom(10).left().row();
        } else {
            modal.add().padBottom(10).row();
        }

        Label fundsLbl = new Label("Current Funds: $" + currentFunds, game.skin, "default-font", Color.WHITE);
        modal.add(fundsLbl).left().row();

        TextButton closeBtn = new TextButton("Awesome", game.skin, "militopia-btn");
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

        modal.add(closeBtn).fillX().width(200).padTop(25);

        popupTable.add(modal);
        popups.add(popupTable);

        // Lock map interactions while looking at the report
        inputController.setInputEnabled(false);
        bottomBar.setBlocked(true);

        stage.addActor(popupTable);
        popupTable.toFront();
    }
}
