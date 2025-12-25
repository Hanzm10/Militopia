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

public class NewGameScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    TextField nameField, seedField, p1Field, p2Field;
    Label errorLabel; // The new error message text

    public NewGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // 1. Create Fields (Empty by default to test validation)
        nameField = new TextField("", game.skin);
        nameField.setMessageText("Enter Game Name"); // Hint text

        seedField = new TextField("", game.skin);
        seedField.setMessageText("Enter Seed");

        p1Field = new TextField("", game.skin);
        p1Field.setMessageText("Enter Player 1 Name");

        p2Field = new TextField("", game.skin);
        p2Field.setMessageText("Enter Player 2 Name");

        // 2. Create Error Label (Initially hidden/empty)
        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle); // Start empty

        TextButton startBtn = new TextButton("Start Game", game.skin);
        TextButton backBtn = new TextButton("Back", game.skin);

        // 3. Layout
        addInputRow(table, "Map Name:", nameField);
        addInputRow(table, "Seed:", seedField);
        addInputRow(table, "Player 1:", p1Field);
        addInputRow(table, "Player 2:", p2Field);

        // Add the error label row (It will appear here if text is set)
        table.row();
        table.add(errorLabel).colspan(2).pad(10);

        table.row().padTop(20);
        table.add(backBtn).width(100).pad(10);
        table.add(startBtn).width(100).pad(10);

        // 4. Validation Logic
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Check if ANY field is empty
                if (isEmpty(nameField) || isEmpty(seedField) || isEmpty(p1Field) || isEmpty(p2Field)) {
                    // SHOW ERROR
                    errorLabel.setText("All fields are required!");
                    // (Optional) Shake animation could go here
                } else {
                    // SUCCESS - Proceed
                    long seed;
                    try {
                        seed = Long.parseLong(seedField.getText());
                    } catch (Exception e) {
                        seed = seedField.getText().hashCode();
                    }

                    GameState newState = new GameState(seed, p1Field.getText(), p2Field.getText(), nameField.getText() + '_' + seed);
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

    // Helper to check for empty strings
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
