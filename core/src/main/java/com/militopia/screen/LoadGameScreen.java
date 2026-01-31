package com.militopia.screen;

import com.militopia.screen.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.data.GameState;
import com.militopia.MilitopiaGame;
import com.militopia.managers.AssetManager;
import com.militopia.utils.HoverListener;
import com.militopia.utils.RenderUtils;

public class LoadGameScreen implements Screen {

    final MilitopiaGame game;
    Stage stage;

    public LoadGameScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        mainTable.add(new Label("Saved Games", game.skin)).pad(20).row();

        Table listTable = new Table();

        // 1. GET ALL FILES IN "saves/" DIRECTORY
        FileHandle dir = Gdx.files.local("saves/");
        if (!dir.exists()) {
            dir.mkdirs(); // Create folder if it doesn't exist
        }

        FileHandle[] files = dir.list(".json"); // Only get JSON files

        if (files.length == 0) {
            listTable.add(new Label("No saves found.", game.skin));
        } else {
            Json json = new Json();

            // 2. LOOP THROUGH FILES
            for (FileHandle file : files) {
                try {
                    // Read the file to get stats (names, seed)
                    final GameState state = json.fromJson(GameState.class, file.readString());

                    String buttonText = state.saveName + " (" + state.timestamp + ")";
                    TextButton btn = new TextButton(buttonText, game.skin);
                    btn.addListener(new HoverListener());

                    btn.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            // LOAD THE GAME with the saved seed
                            game.setScreen(new GameScreen(game, state));
                        }
                    });

                    listTable.add(btn).fillX().pad(5).width(400).row();

                } catch (Exception e) {
                    System.out.println("Corrupt save file: " + file.name());
                }
            }
        }

        ScrollPane scroll = new ScrollPane(listTable, game.skin);
        mainTable.add(scroll).size(500, 300).row();

        TextButton backBtn = new TextButton("Back", game.skin);
        backBtn.addListener(new HoverListener());
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        mainTable.add(backBtn).pad(20);
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
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
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
