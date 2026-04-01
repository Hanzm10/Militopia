package com.militopia.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.militopia.MilitopiaGame;
import com.militopia.managers.TutorialManager;

public class TutorialOverlay {

    private final MilitopiaGame game;
    private final Stage stage;
    private Table rootTable;
    private Label instructionLabel;
    private TextButton nextButton;

    public TutorialOverlay(MilitopiaGame game, Stage stage) {
        this.game = game;
        this.stage = stage;
        build();
    }

    private void build() {
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top().padTop(100);

        instructionLabel = new Label("", game.skin, "default-font", Color.YELLOW);
        instructionLabel.setFontScale(1.2f);
        instructionLabel.setAlignment(Align.center);
        instructionLabel.setWrap(true);

        nextButton = new TextButton("Got it!", game.skin, "militopia-btn");
        nextButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (TutorialManager.getInstance().isActive()) {
                    if (TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.COMPLETED) {
                        TutorialManager.getInstance().endTutorial();
                    } else if (TutorialManager.getInstance().getCurrentStep() == TutorialManager.Step.WELCOME) {
                        TutorialManager.getInstance().nextStep();
                    }
                }
            }
        });

        Table labelBg = new Table();
        labelBg.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.6f)));
        labelBg.add(instructionLabel).width(600).pad(20).row();
        labelBg.add(nextButton).padBottom(20);

        rootTable.add(labelBg).width(640);
        stage.addActor(rootTable);
    }

    public void update() {
        if (TutorialManager.getInstance().isActive()) {
            rootTable.setVisible(true);
            TutorialManager.Step current = TutorialManager.getInstance().getCurrentStep();
            instructionLabel.setText(current.instruction);
            
            // Only show the next button for Welcome and Completed steps
            boolean showButton = (current == TutorialManager.Step.WELCOME || current == TutorialManager.Step.COMPLETED);
            nextButton.setVisible(showButton);
            if (showButton) {
                nextButton.setText(current == TutorialManager.Step.COMPLETED ? "Finish" : "Got it!");
            }
        } else {
            rootTable.setVisible(false);
        }
    }

    public void setVisible(boolean visible) {
        rootTable.setVisible(visible);
    }
}
