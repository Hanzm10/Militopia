package com.militopia.ui;

import com.badlogic.ashley.core.Entity;
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
import com.militopia.components.StatsComponent;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
import com.militopia.data.GameState;
import com.militopia.factories.UnitFactory;
import com.militopia.managers.AssetManager;
import com.militopia.map.MapGenerator;
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
    private GameScreen gameScreen;

    private Label xpLabel;
    private Label fundsLabel;
    private Label fundingTitleLabel;
    private Label turnLabel;

    private int currentBaseOwner = 1;
    private int currentIncome = 0;

    private int buildX, buildY;
    private int buildParentX, buildParentY;

    private Table popupTable = new Table(); // For Level Up
    private int currentBaseLevel = 1; // Track level for menu

    private GameInputController inputController;
    private UnitFactory unitFactory;

    public GameHUD(MilitopiaGame game) {
        this.game = game;
        this.assets = game.assets;
        stage = new Stage(new ScreenViewport());
        summonMenu = new Table();
        rootTable = new Table();

        stage.addActor(rootTable);
        stage.addActor(popupTable); // NEW
    }

    public void build(final GameScreen screen, final GameInputController inputController,
            final UnitFactory unitFactory, final GameState state) {
        this.gameScreen = screen;
        this.inputController = inputController;
        this.unitFactory = unitFactory;

        setupHUD(screen, inputController, unitFactory, state);
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

    private void setupHUD(final GameScreen screen, final GameInputController inputController, final UnitFactory unitFactory, final GameState state) {
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

        bottomContent.add(createIconGroup(settingsBtn, "Settings")).expandX();
        bottomContent.add(createIconGroup(statsBtn, "Game Stats")).expandX();
        bottomContent.add(createIconGroup(endTurnBtn, "End Turn")).expandX();

        bottomContainer = new Table();
        bottomContainer.setBackground(bottomBg);
        bottomContainer.add(bottomContent).width(GameConfig.UI_WIDTH).padBottom(10).padTop(20);
        rootTable.add(bottomContainer).growX().bottom();

        stage.addActor(rootTable);

        createTileInfoPanel();
        configureSummonMenu(state);
        createSettingsOverlay(screen);

        settingsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settingsOverlay.setVisible(true);
            }
        });
        endTurnBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                screen.endTurnAction();
            }
        });

        settingsBtn.addListener(new HoverListener());
        statsBtn.addListener(new HoverListener());
        endTurnBtn.addListener(new HoverListener());
    }

    public void updateXP(int xp) {
        if (xpLabel != null) {
            xpLabel.setText(String.valueOf(xp));
        }
    }

    public void updateTurn(int turn) {
        if (turnLabel != null) {
            turnLabel.setText(String.valueOf(turn));
        }
    }

    public void updateFunding(int funding, int income) {
        this.currentIncome = income;
        if (fundsLabel != null) {
            // Only shows the total count now
            fundsLabel.setText(String.valueOf(funding));
        }
        if (fundingTitleLabel != null) {
            // Shows "Funding (+X)"
            fundingTitleLabel.setText("Funding (+" + income + ")");
        }
    }

    private void configureSummonMenu(GameState state) {
        populateSummonMenu(state);
        float panelHeight = 140f;
        summonMenu.setSize(stage.getWidth(), panelHeight);
        summonMenu.setPosition(0, -panelHeight);
        stage.addActor(summonMenu);
    }

    private void populateSummonMenu(GameState state) {
        summonMenu.clear();
        summonMenu.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.95f)));
        Table contentTable = new Table();

        // --- Filter by Level ---
        java.util.Set<String> unlocked = new java.util.HashSet<>();
        for (int i = 1; i <= currentBaseLevel; i++) {
            com.militopia.config.BaseLevelConfig.LevelData data = com.militopia.config.BaseLevelConfig.getLevel(i);
            for (String u : data.unlockedUnits) {
                unlocked.add(u);
            }
        }

        // Only add buttons if unlocked
        if (unlocked.contains("RECRUIT")) {
            addSummonButton(contentTable, "RECRUIT", inputController, unitFactory, state);
        }
        if (unlocked.contains("RANGER")) {
            addSummonButton(contentTable, "RANGER", inputController, unitFactory, state);
        }
        if (unlocked.contains("SNIPER")) {
            addSummonButton(contentTable, "SNIPER", inputController, unitFactory, state);
        }
        if (unlocked.contains("TANK")) {
            addSummonButton(contentTable, "TANK", inputController, unitFactory, state);
        }
        if (unlocked.contains("RECON_DRONE")) {
            addSummonButton(contentTable, "RECON_DRONE", inputController, unitFactory, state);
        }
        if (unlocked.contains("SUICIDE_DRONE")) {
            addSummonButton(contentTable, "SUICIDE_DRONE", inputController, unitFactory, state);
        }
        if (unlocked.contains("APACHE")) {
            addSummonButton(contentTable, "APACHE", inputController, unitFactory, state);
        }
        if (unlocked.contains("GUNBOAT")) {
            addSummonButton(contentTable, "GUNBOAT", inputController, unitFactory, state);
        }
        if (unlocked.contains("DESTROYER")) {
            addSummonButton(contentTable, "DESTROYER", inputController, unitFactory, state);
        }
        if (unlocked.contains("CARRIER")) {
            addSummonButton(contentTable, "CARRIER", inputController, unitFactory, state);
        }

        TextButton closeBtn = new TextButton("Cancel", game.skin);
        closeBtn.addListener(new HoverListener());
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hideSummonMenu();
                inputController.resetLastClicked();
            }
        });
        summonMenu.add(contentTable).expandX().center();
    }

    private void addSummonButton(Table container, final String unitType, final GameInputController controller, final UnitFactory factory, final GameState state) {
        UnitFactory.UiInfo info = factory.getUnitUi(unitType);
        final int cost = factory.getUnitCost(unitType);
        TextureRegionDrawable circleDrawable;
        try {
            Texture circleTex = assets.get(AssetManager.CIRCLE_UI);
            circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            circleDrawable = new TextureRegionDrawable(new TextureRegion(circleTex));
        } catch (Exception e) {
            circleDrawable = (TextureRegionDrawable) game.skin.newDrawable("white", Color.DARK_GRAY);
        }
        Stack buttonStack = new Stack();
        buttonStack.setTransform(true);
        com.badlogic.gdx.scenes.scene2d.ui.Image circleBg = new com.badlogic.gdx.scenes.scene2d.ui.Image(circleDrawable);
        circleBg.setScaling(Scaling.fit);
        buttonStack.add(circleBg);
        com.badlogic.gdx.scenes.scene2d.ui.Image unitIcon = new com.badlogic.gdx.scenes.scene2d.ui.Image(info.region);
        unitIcon.setScaling(Scaling.fit);
        Container<com.badlogic.gdx.scenes.scene2d.ui.Image> iconContainer = new Container<>(unitIcon);
        iconContainer.size(50, 50).center();
        buttonStack.add(iconContainer);
        buttonStack.setOrigin(40, 40);
        buttonStack.addListener(new HoverListener());
        buttonStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int currentFunds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                if (currentFunds >= cost) {
                    if (currentBaseOwner == 1) {
                        state.p1Funding -= cost;
                    } else {
                        state.p2Funding -= cost;
                    }
                    int tx = controller.getLastClickedX();
                    int ty = controller.getLastClickedY();
                    if (tx != -1 && ty != -1) {
                        factory.createUnit(unitType, tx, ty, currentBaseOwner, true);
                    }
                    updateFunding((currentBaseOwner == 1) ? state.p1Funding : state.p2Funding, currentIncome);
                    hideSummonMenu();
                    controller.resetLastClicked();
                }
            }
        });
        Table group = new Table();
        group.add(buttonStack).size(80, 80).row();
        Label nameLbl = new Label(info.name + " (" + cost + ")", game.skin, "default-font", Color.WHITE);
        nameLbl.setFontScale(0.7f);
        group.add(nameLbl).padTop(5);
        container.add(group).pad(10);
    }

    public void openSummonMenu(int owner, GameState state, int level) {
        this.currentBaseOwner = owner;
        this.currentBaseLevel = level;
        populateSummonMenu(state);
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

    // --- NEW: HUNT MENU ---
    public void openHuntMenu(final Entity animalEntity, final Entity hunterUnit,
            final MapGenerator.ObjectType animalType,
            final UnitFactory factory, final GameInputController controller) {

        summonMenu.clear();
        summonMenu.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.95f)));
        Table contentTable = new Table();

        // Add Dynamic Button
        addHuntButton(contentTable, animalEntity, hunterUnit, animalType, factory, controller);

        summonMenu.add(contentTable).expandX().center();

        animateMenuOpen();
    }

    private void addHuntButton(Table container, final Entity animalEntity, final Entity hunter,
            final MapGenerator.ObjectType animalType,
            final UnitFactory factory, final GameInputController controller) {

        // Get Animal UI Info (Icon & Name)
        UnitFactory.UiInfo info = factory.getObjectUi(animalType);

        // Setup Button Style (Circle)
        TextureRegionDrawable circleDrawable;
        try {
            Texture circleTex = assets.get(AssetManager.CIRCLE_UI);
            circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            circleDrawable = new TextureRegionDrawable(new TextureRegion(circleTex));
        } catch (Exception e) {
            circleDrawable = (TextureRegionDrawable) game.skin.newDrawable("white", Color.DARK_GRAY);
        }
        Stack buttonStack = new Stack();
        buttonStack.setTransform(true);
        com.badlogic.gdx.scenes.scene2d.ui.Image circleBg = new com.badlogic.gdx.scenes.scene2d.ui.Image(circleDrawable);
        circleBg.setScaling(Scaling.fit);
        buttonStack.add(circleBg);

        TextureRegion iconRegion = factory.getHudIcon(animalType);

        // The Animal Icon
        com.badlogic.gdx.scenes.scene2d.ui.Image icon = new com.badlogic.gdx.scenes.scene2d.ui.Image(iconRegion);
        icon.setScaling(Scaling.fit);
        Container<com.badlogic.gdx.scenes.scene2d.ui.Image> iconContainer = new Container<>(icon);
        iconContainer.size(50, 50).center();
        buttonStack.add(iconContainer);

        buttonStack.setOrigin(40, 40);
        buttonStack.addListener(new HoverListener());

        // Click Logic
        buttonStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                controller.performHunt(animalEntity, hunter);
            }
        });

        Table group = new Table();
        group.add(buttonStack).size(80, 80).row();
        Label nameLbl = new Label("Hunt " + info.name, game.skin, "default-font", Color.WHITE);
        nameLbl.setFontScale(0.7f);
        group.add(nameLbl).padTop(5);
        container.add(group).pad(10);
    }

    public void openCaptureMenu(final Entity townEntity, final Entity capturingUnit, final UnitFactory factory, final GameInputController controller, final MapGenerator.GameMap map, final GameState state) {
        summonMenu.clear();
        summonMenu.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.95f)));
        Table contentTable = new Table();
        addCaptureButton(contentTable, townEntity, capturingUnit, factory, controller, map, state);

        summonMenu.add(contentTable).expandX().center();

        animateMenuOpen();
    }

    private void animateMenuOpen() {
        float panelHeight = 140f;
        summonMenu.setSize(stage.getWidth(), panelHeight);
        summonMenu.setPosition(0, -panelHeight);
        stage.addActor(summonMenu);
        tileInfoTable.clearActions();
        tileInfoTable.addAction(Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));
        summonMenu.clearActions();
        summonMenu.setX(0);
        summonMenu.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));
        bottomContainer.clearActions();
        bottomContainer.addAction(Actions.moveTo(bottomContainer.getX(), -bottomContainer.getHeight(), 0.3f, Interpolation.pow2Out));
    }

    private void addCaptureButton(Table container, final Entity structureEntity, final Entity capturingUnit, final UnitFactory factory, final GameInputController controller, final MapGenerator.GameMap map, final GameState state) {
        final int newOwner = capturingUnit.getComponent(StatsComponent.class).owner;
        TextureRegion baseRegion;
        if (newOwner == 1) {
            baseRegion = factory.getHudIcon(MapGenerator.ObjectType.BASE_P1);
        } else {
            baseRegion = factory.getHudIcon(MapGenerator.ObjectType.BASE_P2);
        }
        TextureRegionDrawable circleDrawable;
        try {
            Texture circleTex = assets.get(AssetManager.CIRCLE_UI);
            circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            circleDrawable = new TextureRegionDrawable(new TextureRegion(circleTex));
        } catch (Exception e) {
            circleDrawable = (TextureRegionDrawable) game.skin.newDrawable("white", Color.DARK_GRAY);
        }
        Stack buttonStack = new Stack();
        buttonStack.setTransform(true);
        com.badlogic.gdx.scenes.scene2d.ui.Image circleBg = new com.badlogic.gdx.scenes.scene2d.ui.Image(circleDrawable);
        circleBg.setScaling(Scaling.fit);
        buttonStack.add(circleBg);
        com.badlogic.gdx.scenes.scene2d.ui.Image unitIcon = new com.badlogic.gdx.scenes.scene2d.ui.Image(baseRegion);
        unitIcon.setScaling(Scaling.fit);
        Container<com.badlogic.gdx.scenes.scene2d.ui.Image> iconContainer = new Container<>(unitIcon);
        iconContainer.size(50, 50).center();
        buttonStack.add(iconContainer);
        buttonStack.setOrigin(40, 40);
        buttonStack.addListener(new HoverListener());
        buttonStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                factory.captureStructure(structureEntity, newOwner, map, state);
                StatsComponent unitStats = capturingUnit.getComponent(StatsComponent.class);
                if (unitStats != null) {
                    unitStats.hasActed = true;
                }
                int newTotalXP = (newOwner == 1) ? state.p1XP : state.p2XP;
                updateXP(newTotalXP);
                int newIncome = gameScreen.calculateIncome(newOwner);
                int currentFunds = (newOwner == 1) ? state.p1Funding : state.p2Funding;
                updateFunding(currentFunds, newIncome);
                hideSummonMenu();
                controller.deselect();
            }
        });
        Table group = new Table();
        group.add(buttonStack).size(80, 80).row();
        String labelText = "Capture Structure";
        StatsComponent stats = structureEntity.getComponent(StatsComponent.class);
        if (stats != null) {
            if (stats.name.contains("Town")) {
                labelText = "Capture Town";
            } else {
                labelText = "Capture Enemy Base";
            }
        }
        Label nameLbl = new Label(labelText, game.skin, "default-font", Color.WHITE);
        nameLbl.setFontScale(0.7f);
        group.add(nameLbl).padTop(5);
        container.add(group).pad(10);
    }

    public void openBuildMenu(int x, int y, int owner, int maxLevel, boolean isWater, boolean isCoastal, GameState state, int parentX, int parentY) {
        this.buildX = x;
        this.buildY = y;
        this.currentBaseOwner = owner;
        this.buildParentX = parentX;
        this.buildParentY = parentY;

        populateBuildMenu(state, maxLevel, isWater, isCoastal);
        // ... (Animations same as before) ...
        tileInfoTable.clearActions();
        tileInfoTable.addAction(Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));
        summonMenu.clearActions();
        summonMenu.setWidth(stage.getWidth());
        summonMenu.setX(0);
        summonMenu.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));
        bottomContainer.clearActions();
        bottomContainer.addAction(Actions.moveTo(bottomContainer.getX(), -bottomContainer.getHeight(), 0.3f, Interpolation.pow2Out));
    }

    private void populateBuildMenu(GameState state, int maxLevel, boolean isWater, boolean isCoastal) {
        summonMenu.clear();
        summonMenu.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.95f)));
        Table contentTable = new Table();

        java.util.Set<String> unlocked = new java.util.HashSet<>();
        for (int i = 1; i <= maxLevel; i++) {
            com.militopia.config.BaseLevelConfig.LevelData data = com.militopia.config.BaseLevelConfig.getLevel(i);
            if (data.unlockedStructures != null) {
                for (String s : data.unlockedStructures) {
                    unlocked.add(s);
                }
            }
        }

        for (String struct : unlocked) {
            if (struct.equals("PORT")) {
                if (isWater && isCoastal) {
                    addBuildButton(contentTable, struct, inputController, unitFactory, state);
                }
            } else if (struct.equals("OIL_DERRICK")) {
                if (!isWater) {
                    addBuildButton(contentTable, struct, inputController, unitFactory, state);
                }
            } else {
                if (!isWater) {
                    addBuildButton(contentTable, struct, inputController, unitFactory, state);
                }
            }
        }

        summonMenu.add(contentTable).expandX().center();
        summonMenu.row();
    }

    private void addBuildButton(Table container, final String structType, final GameInputController controller, final UnitFactory factory, final GameState state) {
        TextureRegion iconRegion = factory.getTextureForPopup(structType);
        final int cost = factory.getStructureCost(structType);

        // --- FIX: Use 'Drawable' interface instead of 'TextureRegionDrawable' ---
        // This prevents the ClassCastException if the skin returns a SpriteDrawable
        com.badlogic.gdx.scenes.scene2d.utils.Drawable circleDrawable;
        try {
            Texture circleTex = assets.get(AssetManager.CIRCLE_UI);
            circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            circleDrawable = new TextureRegionDrawable(new TextureRegion(circleTex));
        } catch (Exception e) {
            // No casting needed here anymore
            circleDrawable = game.skin.newDrawable("white", Color.DARK_GRAY);
        }

        Stack buttonStack = new Stack();
        buttonStack.setTransform(true);
        com.badlogic.gdx.scenes.scene2d.ui.Image circleBg = new com.badlogic.gdx.scenes.scene2d.ui.Image(circleDrawable);
        circleBg.setScaling(Scaling.fit);
        buttonStack.add(circleBg);

        com.badlogic.gdx.scenes.scene2d.ui.Image unitIcon = new com.badlogic.gdx.scenes.scene2d.ui.Image(iconRegion);
        unitIcon.setScaling(Scaling.fit);
        Container<com.badlogic.gdx.scenes.scene2d.ui.Image> iconContainer = new Container<>(unitIcon);
        iconContainer.size(50, 50).center();
        buttonStack.add(iconContainer);
        buttonStack.setOrigin(40, 40);
        buttonStack.addListener(new HoverListener());

        buttonStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int currentFunds = (currentBaseOwner == 1) ? state.p1Funding : state.p2Funding;
                if (currentFunds >= cost) {
                    if (currentBaseOwner == 1) {
                        state.p1Funding -= cost;
                    } else {
                        state.p2Funding -= cost;
                    }

                    // Create Structure with Parent Link
                    factory.createStructure(structType, buildX, buildY, currentBaseOwner, buildParentX, buildParentY);

                    updateFunding((currentBaseOwner == 1) ? state.p1Funding : state.p2Funding, currentIncome);
                    hideSummonMenu();
                    controller.resetLastClicked();
                }
            }
        });

        Table group = new Table();
        group.add(buttonStack).size(80, 80).row();

        // Formatting Name
        Label nameLbl = new Label(factory.toNiceName(structType) + " (" + cost + ")", game.skin, "default-font", Color.WHITE);
        nameLbl.setFontScale(0.6f);
        nameLbl.setWrap(true);
        nameLbl.setAlignment(com.badlogic.gdx.utils.Align.center);

        group.add(nameLbl).width(90).padTop(5);
        container.add(group).pad(10);
    }

    // ... (rest of helper methods createTileInfoPanel, createSettingsOverlay, createStatGroup, createCircleButton, createIconGroup, createGradientDrawable unchanged) ...
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

        if (title.equals("Funding")) {
            // --- NEW DESIGN FOR FUNDING ---

            // Row 1: Label "Funding (+0)"
            fundingTitleLabel = new Label(title + " (+0)", game.skin, "default-font", Color.WHITE);
            fundingTitleLabel.setFontScale(0.8f);
            t.add(fundingTitleLabel).left().row();

            // Row 2: Icon + Value
            Table valueRow = new Table();

            // Icon
            try {
                Texture iconTex = assets.get(AssetManager.FUNDING_ICON);
                iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                com.badlogic.gdx.scenes.scene2d.ui.Image iconParams = new com.badlogic.gdx.scenes.scene2d.ui.Image(new TextureRegion(iconTex));
                iconParams.setScaling(Scaling.fit);
                valueRow.add(iconParams).size(40, 40).padRight(0); // Small size
            } catch (Exception e) {
                // Fallback if asset missing
            }

            // Value
            fundsLabel = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
            fundsLabel.setFontScale(1.2f);
            valueRow.add(fundsLabel);

            t.add(valueRow).left();

        } else {
            // --- STANDARD DESIGN (XP / Turn) ---
            Label titleLbl = new Label(title, game.skin, "default-font", Color.WHITE);
            titleLbl.setFontScale(0.8f);
            Label valLbl = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
            valLbl.setFontScale(1.2f);

            if (title.equals("XP")) {
                xpLabel = valLbl;
            }
            if (title.equals("Turn")) {
                turnLabel = valLbl;
            }

            t.add(titleLbl).row();
            t.add(valLbl);
        }

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

    public void showLevelUpPopup(int owner, String baseName, int newLevel, int bonusFunds,
            String[] units, String[] structs, UnitFactory factory) {
        popupTable.clear();
        popupTable.setFillParent(true);
        popupTable.setBackground(game.skin.newDrawable("white", new Color(0, 0, 0, 0.8f)));

        Table modal = new Table();
        // --- FIX 1: Use "default-font" for HD look ---
        Label title = new Label(baseName + " Leveled Up!", game.skin, "default-font", Color.YELLOW);
        title.setFontScale(1.2f); // Match HUD scaling
        modal.add(title).pad(20).row();

        Label sub = new Label("Reached Level " + newLevel, game.skin, "default-font", Color.WHITE);
        sub.setFontScale(0.9f);
        modal.add(sub).padBottom(20).row();

        // Incentives
        Table incentives = new Table();
        if (bonusFunds > 0) {
            // --- FIX: Use FUNDING_ICON2 for the popup bubble ---
            TextureRegion fundingIcon2 = null;
            try {
                fundingIcon2 = new TextureRegion(assets.get(AssetManager.FUNDING_ICON2));
            } catch (Exception e) {
                /* fallback if missing */ }

            incentives.add(createIncentiveBubble("+" + bonusFunds + " Funding", fundingIcon2)).pad(10);
        }
        for (String u : units) {
            incentives.add(createIncentiveBubble("Unlock:\n" + unitFactory.toNiceName(u), factory.getTextureForPopup(u))).pad(10);
        }
        for (String s : structs) {
            incentives.add(createIncentiveBubble("Unlock:\n" + unitFactory.toNiceName(s), factory.getTextureForPopup(s))).pad(10);
        }

        modal.add(incentives).row();

        TextButton okBtn = new TextButton("Awesome!", game.skin);
        okBtn.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                // --- FIX 2: Completely remove the table from stage ---
                popupTable.remove();
            }
        });
        modal.add(okBtn).padTop(30).size(150, 50);

        popupTable.add(modal);

        // Ensure it's on top
        popupTable.toFront();
        stage.addActor(popupTable);
    }

    private Table createIncentiveBubble(String text, TextureRegion icon) {
        Stack stack = new Stack();
        TextureRegionDrawable bg;
        try {
            bg = new TextureRegionDrawable(new TextureRegion(game.assets.get(AssetManager.CIRCLE_UI2)));
        } catch (Exception e) {
            bg = (TextureRegionDrawable) game.skin.newDrawable("white", Color.GRAY);
        }

        stack.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(bg));
        if (icon != null) {
            Container<com.badlogic.gdx.scenes.scene2d.ui.Image> c = new Container<>(new com.badlogic.gdx.scenes.scene2d.ui.Image(icon));
            c.size(50, 50).center();
            stack.add(c);
        }
        Table t = new Table();
        t.add(stack).size(80, 80).row();
        // --- FIX 3: Use "default-font" here too ---
        Label l = new Label(text, game.skin, "default-font", Color.WHITE);
        l.setFontScale(0.6f);
        l.setWrap(true);
        l.setAlignment(com.badlogic.gdx.utils.Align.center);
        t.add(l).width(90).padTop(5);
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
