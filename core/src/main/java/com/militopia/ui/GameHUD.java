package com.militopia.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.screen.GameScreen;
import com.militopia.utils.HoverListener;

public class GameHUD {

    private MilitopiaGame game;
    public Stage stage;
    private Table rootTable;
    public Table summonMenu;
    private Table settingsOverlay;
    private AssetManager assets;
    private Table tileInfoTable;
    private Table bottomContainer;
    private com.badlogic.gdx.scenes.scene2d.ui.Image tileInfoImage;
    private Label tileInfoLabel;
    private int currentBaseOwner = 1;

    public GameHUD(MilitopiaGame game) {
        this.game = game;
        this.assets = game.assets;
        stage = new Stage(new ScreenViewport());
        summonMenu = new Table();
        rootTable = new Table();
    }

    public void build(final GameScreen screen, final GameInputController inputController, final UnitFactory unitFactory) {
        setupHUD(screen, inputController, unitFactory);
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (tileInfoTable != null) {
            tileInfoTable.setWidth(width);
            tileInfoTable.setX(0);
        }
        rootTable.invalidateHierarchy();
        if (summonMenu != null) {
            summonMenu.setSize(width, 140);
            summonMenu.setX(0);
        }
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }

    private void setupHUD(final GameScreen screen, final GameInputController inputController, final UnitFactory unitFactory) {
        rootTable.clear();
        rootTable.setFillParent(true);
        TextureRegionDrawable topBg = createGradientDrawable(80, true);
        TextureRegionDrawable bottomBg = createGradientDrawable(80, false);

        Table topContent = new Table();
        topContent.add(createStatGroup("XP", "0")).expandX();
        topContent.add(createStatGroup("Funding", "1000")).expandX();
        topContent.add(createStatGroup("Turn", "1")).expandX();
        Table topContainer = new Table();
        topContainer.setBackground(topBg);
        topContainer.add(topContent).width(GameConfig.UI_WIDTH).padTop(10).padBottom(20);
        rootTable.add(topContainer).growX().top().row();

        rootTable.add().expandY().row();

        Table bottomContent = new Table();
        ImageButton settingsBtn = createCircleButton("icon_settings");
        ImageButton statsBtn = createCircleButton("icon_stats");
        ImageButton endTurnBtn = createCircleButton("icon_end");

        // Add Listeners to Bottom Buttons (They need color setting too for the whiten effect to work)
        // Note: We removed the setColor(LIGHT_GRAY) logic previously to keep them white.
        
        bottomContent.add(createIconGroup(settingsBtn, "Settings")).expandX();
        bottomContent.add(createIconGroup(statsBtn, "Game Stats")).expandX();
        bottomContent.add(createIconGroup(endTurnBtn, "End Turn")).expandX();

        bottomContainer = new Table();
        bottomContainer.setBackground(bottomBg);
        bottomContainer.add(bottomContent).width(GameConfig.UI_WIDTH).padBottom(10).padTop(20);
        rootTable.add(bottomContainer).growX().bottom();

        stage.addActor(rootTable);

        createTileInfoPanel();
        configureSummonMenu(inputController, unitFactory);
        createSettingsOverlay(screen);

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.setVisible(true);
            }
        });
        settingsBtn.addListener(new HoverListener());
        statsBtn.addListener(new HoverListener());
        endTurnBtn.addListener(new HoverListener());
    }

    private void configureSummonMenu(final GameInputController inputController, final UnitFactory unitFactory) {
        summonMenu.clear();
        summonMenu.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.95f)));

        Table contentTable = new Table();

        // 1. ADD UNITS
        addSummonButton(contentTable, "RECRUIT", inputController, unitFactory);

        // --- CANCEL BUTTON REMOVED ---

        // Add content centered
        summonMenu.add(contentTable).expandX().center();

        float panelHeight = 140f;
        summonMenu.setSize(stage.getWidth(), panelHeight);
        summonMenu.setPosition(0, -panelHeight);
        stage.addActor(summonMenu);
    }

    private void addSummonButton(Table container, final String unitType,
            final GameInputController controller, final UnitFactory factory) {

        UnitFactory.UiInfo info = factory.getUnitUi(unitType);

        // ... (Asset loading stays the same) ...
        TextureRegionDrawable circleDrawable;
        try {
            Texture circleTex = assets.get(AssetManager.CIRCLE_UI);
            circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            circleDrawable = new TextureRegionDrawable(new TextureRegion(circleTex));
        } catch (Exception e) {
            circleDrawable = (TextureRegionDrawable) game.skin.newDrawable("white", Color.DARK_GRAY);
        }

        Stack buttonStack = new Stack();

        // --- FIX 1: ENABLE TRANSFORM ---
        // This allows the Group (Stack) to Scale and Rotate!
        buttonStack.setTransform(true);

        com.badlogic.gdx.scenes.scene2d.ui.Image circleBg = new com.badlogic.gdx.scenes.scene2d.ui.Image(circleDrawable);
        circleBg.setScaling(Scaling.fit);
        buttonStack.add(circleBg);

        com.badlogic.gdx.scenes.scene2d.ui.Image unitIcon = new com.badlogic.gdx.scenes.scene2d.ui.Image(info.region);
        unitIcon.setScaling(Scaling.fit);

        Container<com.badlogic.gdx.scenes.scene2d.ui.Image> iconContainer = new Container<>(unitIcon);
        iconContainer.size(50, 50).center();
        buttonStack.add(iconContainer);

        // --- FIX 2: SET INITIAL ORIGIN ---
        buttonStack.setOrigin(40, 40); // Center of 80x80
        
        buttonStack.addListener(new HoverListener());

        buttonStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int tx = controller.getLastClickedX();
                int ty = controller.getLastClickedY();
                if (tx != -1 && ty != -1) {
                    factory.createUnit(unitType, tx, ty, currentBaseOwner);
                    hideSummonMenu();
                    controller.resetLastClicked();
                }
            }
        });

        Table group = new Table();
        group.add(buttonStack).size(80, 80).row();

        Label nameLbl = new Label(info.name, game.skin, "default-font", Color.WHITE);
        nameLbl.setFontScale(0.7f);
        group.add(nameLbl).padTop(5);

        container.add(group).pad(10);
    }

    public void openSummonMenu(int owner) {
        this.currentBaseOwner = owner;
        tileInfoTable.clearActions();
        tileInfoTable.addAction(Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));

        summonMenu.clearActions();
        summonMenu.setWidth(stage.getWidth());
        summonMenu.setX(0);
        summonMenu.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));

        bottomContainer.clearActions();
        bottomContainer.addAction(Actions.moveTo(bottomContainer.getX(), -bottomContainer.getHeight(), 0.3f, Interpolation.pow2Out));
    }

    public void hideSummonMenu() {
        summonMenu.clearActions();
        summonMenu.addAction(Actions.moveTo(0, -summonMenu.getHeight(), 0.3f, Interpolation.pow2In));
        bottomContainer.clearActions();
        bottomContainer.addAction(Actions.moveTo(bottomContainer.getX(), 0, 0.3f, Interpolation.pow2In));
    }

    private void createTileInfoPanel() {
        tileInfoTable = new Table();
        tileInfoTable.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.9f)));
        tileInfoImage = new com.badlogic.gdx.scenes.scene2d.ui.Image();
        tileInfoImage.setScaling(Scaling.fit);
        tileInfoTable.add(tileInfoImage).size(70, 70).padLeft(20);
        tileInfoLabel = new Label("Terrain Name", game.skin, "default-font", Color.WHITE);
        tileInfoLabel.setFontScale(0.8f);
        tileInfoTable.add(tileInfoLabel).expandX().left().padLeft(20);
        ImageButton.ImageButtonStyle closeStyle = new ImageButton.ImageButtonStyle();
        try {
            Texture closeTex = assets.get(AssetManager.BTN_SLIDEDOWN);
            TextureRegionDrawable myDrawable = new TextureRegionDrawable(new TextureRegion(closeTex));
            closeStyle.imageUp = myDrawable;
            closeStyle.imageDown = myDrawable.tint(Color.GRAY);
        } catch (Exception e) {
            closeStyle.imageUp = game.skin.newDrawable("white", Color.RED);
        }
        ImageButton closeBtn = new ImageButton(closeStyle);
        closeBtn.addListener(new HoverListener());
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hideTileInfo();
            }
        });
        tileInfoTable.add(closeBtn).size(40, 40).padRight(20);
        float panelHeight = 80f;
        tileInfoTable.setPosition(0, -panelHeight);
        tileInfoTable.setSize(stage.getWidth(), panelHeight);
        stage.addActor(tileInfoTable);
    }

    public void showTileInfo(String name, TextureRegion region) {
        if (summonMenu.getY() > -50) {
        }
        tileInfoLabel.setText(name);
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));
        tileInfoTable.setWidth(stage.getWidth());
        tileInfoTable.setX(0);
        tileInfoTable.clearActions();
        bottomContainer.clearActions();
        tileInfoTable.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));
        bottomContainer.addAction(Actions.moveBy(0, -bottomContainer.getHeight(), 0.3f, Interpolation.pow2Out));
    }

    public void hideTileInfo() {
        tileInfoTable.clearActions();
        bottomContainer.clearActions();
        tileInfoTable.addAction(Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));
        bottomContainer.addAction(Actions.moveTo(bottomContainer.getX(), 0, 0.3f, Interpolation.pow2In));
    }

    private void createSettingsOverlay(final GameScreen screen) {
        settingsOverlay = new Table();
        settingsOverlay.setFillParent(true);
        settingsOverlay.setVisible(false);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0.85f);
        p.fill();
        settingsOverlay.setBackground(new TextureRegionDrawable(new TextureRegion(new Texture(p))));
        p.dispose();
        Table menuBox = new Table();
        menuBox.setBackground(game.skin.newDrawable("white", Color.DARK_GRAY));
        Label title = new Label("PAUSED", game.skin);
        title.setFontScale(1.5f);
        final TextButton fogBtn = new TextButton("Toggle Fog: ON", game.skin);
        fogBtn.addListener(new HoverListener());
        fogBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean newState = screen.toggleFog();
                fogBtn.setText("Toggle Fog: " + (newState ? "ON" : "OFF"));
            }
        });
        TextButton saveExitBtn = new TextButton("Save & Exit", game.skin);
        saveExitBtn.addListener(new HoverListener());
        saveExitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                screen.saveAndExit();
            }
        });
        TextButton resumeBtn = new TextButton("Resume", game.skin);
        resumeBtn.addListener(new HoverListener());
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.setVisible(false);
            }
        });
        menuBox.add(title).pad(20).row();
        menuBox.add(fogBtn).size(200, 50).pad(10).row();
        menuBox.add(saveExitBtn).size(200, 50).pad(10).row();
        menuBox.add(resumeBtn).size(200, 50).pad(10);
        settingsOverlay.add(menuBox).size(300, 300);
        stage.addActor(settingsOverlay);
    }

    private Table createStatGroup(String title, String placeholderValue) {
        Table t = new Table();
        Label titleLbl = new Label(title, game.skin, "default-font", Color.WHITE);
        titleLbl.setFontScale(0.8f);
        Label valLbl = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
        valLbl.setFontScale(1.2f);
        t.add(titleLbl).row();
        t.add(valLbl);
        return t;
    }

    private ImageButton createCircleButton(String iconName) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        try {
            Texture texture = assets.get(iconName + ".png");
            if (texture == null) {
                texture = new Texture(iconName + ".png");
            }
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
            style.imageUp = drawable;
            style.imageDown = drawable.tint(Color.GRAY);
        } catch (Exception e) {
            style.imageUp = game.skin.newDrawable("white", Color.GRAY);
        }
        ImageButton btn = new ImageButton(style);
        btn.setTransform(true);
        btn.setSize(60, 60);
        btn.setOrigin(30, 30);
        return btn;
    }

    private Table createIconGroup(ImageButton btn, String labelText) {
        Table t = new Table();
        t.add(btn).size(60, 60).row();
        Label lbl = new Label(labelText, game.skin, "default-font", Color.WHITE);
        lbl.setFontScale(0.7f);
        t.add(lbl).padTop(5);
        return t;
    }

    private TextureRegionDrawable createGradientDrawable(int height, boolean isTopDown) {
        Pixmap pixmap = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            float alpha = isTopDown ? 1.0f - ((float) y / height) : ((float) y / height);
            pixmap.setColor(0f, 0f, 0f, alpha);
            pixmap.drawPixel(0, y);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}