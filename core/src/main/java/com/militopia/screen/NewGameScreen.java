package com.militopia.screen;

import com.militopia.screen.MenuScreen;
import com.militopia.screen.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.data.GameState;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AssetManager;
import com.militopia.utils.HoverListener;
import com.militopia.utils.RenderUtils;

public class NewGameScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    TextField nameField, seedField;
    Label errorLabel;

    // Mode Selection
    TextButton blitzBtn, marathonBtn;
    int selectedWidth = 16;
    int selectedHeight = 16;

    public NewGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        nameField = new TextField("", game.skin);
        nameField.setMessageText("Enter Game Name");

        seedField = new TextField("", game.skin);
        seedField.setMessageText("Enter Seed");

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);

        // --- GAME MODE BUTTONS ---
        blitzBtn = new TextButton("Blitz (16x16)", game.skin, "toggle");
        marathonBtn = new TextButton("Marathon (32x32)", game.skin, "toggle");

        // Default selection: Blitz
        blitzBtn.setChecked(true);
        marathonBtn.setChecked(false);

        blitzBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedWidth = 16;
                selectedHeight = 16;
                marathonBtn.setChecked(false); // Manual Toggle
            }
        });

        marathonBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedWidth = 32;
                selectedHeight = 32;
                blitzBtn.setChecked(false); // Manual Toggle
            }
        });

        TextButton startBtn = new TextButton("Start Game", game.skin);
        startBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin);
        backBtn.addListener(new HoverListener());

        addInputRow(table, "Map Name:", nameField);
        addInputRow(table, "Seed:", seedField);

        // Add Mode Selection Row
        table.row().pad(10);
        table.add(new Label("Game Mode:", game.skin)).right().pad(5);
        Table modeTable = new Table();
        modeTable.add(blitzBtn).width(150).padRight(5);
        modeTable.add(marathonBtn).width(150);
        table.add(modeTable).left();

        table.row();
        table.add(errorLabel).colspan(2).pad(10);

        table.row().padTop(20);
        table.add(backBtn).width(100).pad(10);
        table.add(startBtn).width(100).pad(10);

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isEmpty(nameField) || isEmpty(seedField)) {
                    errorLabel.setText("All fields are required!");
                } else {
                    long seed;
                    try {
                        seed = Long.parseLong(seedField.getText());
                    } catch (Exception e) {
                        seed = seedField.getText().hashCode();
                    }

                    // Create GameState with selected Dimensions
                    GameState newState = new GameState(seed, nameField.getText() + '_' + seed, selectedWidth, selectedHeight);
                    game.setScreen(new GameScreen(game, newState));
                }
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
    }

    private boolean isEmpty(TextField field) {
        return field.getText() == null || field.getText().trim().isEmpty();
    }

    private void addInputRow(Table t, String labelText, TextField field) {
        t.add(new Label(labelText, game.skin)).right().pad(5);
        t.add(field).width(200).pad(5);
        t.row();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        RenderUtils.drawProportionalBackground(game.batch, game.assets.get(AssetManager.BACKGROUND));
        stage.act();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
