package com.militopia.ui; // or com.militopia.screen if you prefer

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.config.GameConfig;
import com.militopia.controller.GameInputController;
import com.militopia.factories.UnitFactory;
import com.militopia.map.MapGenerator;
import com.militopia.screen.GameScreen;
import com.militopia.utils.HoverListener;

public class GameHUD {

    private MilitopiaGame game;
    public Stage stage; // Public so GameScreen can access input/viewport

    // UI Components
    private Table rootTable;
    public Table summonMenu; // Public so InputController can toggle it
    private Table settingsOverlay;

    private Table tileInfoTable;
    private Table bottomContainer;
    private com.badlogic.gdx.scenes.scene2d.ui.Image tileInfoImage; // To update the icon
    private Label tileInfoLabel; // To update the text

    // Labels (So we can update them)
    private Label xpLabel, fundsLabel, turnLabel;

    public GameHUD(MilitopiaGame game) {
        this.game = game;
        // 1. Create Stage
        stage = new Stage(new ScreenViewport());

        // 2. Initialize Tables early (prevents NullPointer if accessed before build)
        summonMenu = new Table();
        rootTable = new Table();
    }
    private int currentBaseOwner = 1;

    /**
     * Builds the UI. We pass dependencies here because they might not exist
     * when GameHUD is first created.
     */
    public void build(final GameScreen screen, final GameInputController inputController,
            final UnitFactory unitFactory) {

        setupHUD(screen, inputController, unitFactory);
    }

    public void openSummonMenu(int owner) {
        this.currentBaseOwner = owner; // Save who owns the base we just clicked
        summonMenu.setVisible(true);
    }

    public void resize(int width, int height) {
        // Update Stage Viewport
        stage.getViewport().update(width, height, true);

        // --- FIX TILE INFO RESIZING ---
        if (tileInfoTable != null) {
            // 1. Force Width to match new screen width
            tileInfoTable.setWidth(width);

            // 2. Force X to 0 (Left edge)
            tileInfoTable.setX(0);
        }

        // --- FIX BOTTOM CONTAINER RESIZING ---
        // Since bottomContainer is inside rootTable (which uses .growX()), 
        // it SHOULD resize automatically, but we ensure layout is refreshed.
        rootTable.invalidateHierarchy();

        // Re-center summon menu
        if (summonMenu != null) {
            summonMenu.setPosition(
                    (stage.getWidth() - summonMenu.getWidth()) / 2f,
                    100
            );
        }
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }

    // ============================================
    //  MAIN SETUP LOGIC (Moved from GameScreen)
    // ============================================
    private void setupHUD(final GameScreen screen, final GameInputController inputController,
            final UnitFactory unitFactory) {

        rootTable.clear(); // Clear in case of rebuild
        rootTable.setFillParent(true);

        // Generate Gradients
        TextureRegionDrawable topBg = createGradientDrawable(80, true);
        TextureRegionDrawable bottomBg = createGradientDrawable(80, false);

        // --- TOP HUD ---
        Table topContent = new Table();
        topContent.add(createStatGroup("XP", "0")).expandX();
        topContent.add(createStatGroup("Funding", "1000")).expandX();
        topContent.add(createStatGroup("Turn", "1")).expandX();

        Table topContainer = new Table();
        topContainer.setBackground(topBg);
        topContainer.add(topContent).width(GameConfig.UI_WIDTH).padTop(10).padBottom(20);
        rootTable.add(topContainer).growX().top().row();

        // --- SPACER ---
        rootTable.add().expandY().row();

        // --- BOTTOM HUD ---
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

        createTileInfoPanel(); // Helper method defined below

        // --- SUMMON MENU ---
        configureSummonMenu(inputController, unitFactory);

        // --- SETTINGS OVERLAY ---
        createSettingsOverlay(screen); // Pass screen for Save Logic

        // --- LISTENERS ---
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

    private void createTileInfoPanel() {
        tileInfoTable = new Table();
        // Background: Dark Gray with transparency
        tileInfoTable.setBackground(game.skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.9f)));

        //font crisptness
        Label.LabelStyle style = new Label.LabelStyle(
                game.skin.getFont("default-font"),
                Color.WHITE
        );

        // 1. LEFT: The Icon
        tileInfoImage = new com.badlogic.gdx.scenes.scene2d.ui.Image();
        tileInfoTable.add(tileInfoImage).size(50, 50).padLeft(20);

        // 2. CENTER: The Name
        tileInfoLabel = new Label("Terrain Name", style);
        tileInfoLabel.setFontScale(0.8f);
        tileInfoTable.add(tileInfoLabel).expandX().left().padLeft(20);

        // 3. RIGHT: Close Button (CHANGED TO IMAGE BUTTON)
        ImageButton.ImageButtonStyle closeStyle = new ImageButton.ImageButtonStyle();

        try {
            Texture closeTex = new Texture("slidedown_btn.png");
            closeTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Create the specific drawable type so .tint() works
            TextureRegionDrawable myDrawable = new TextureRegionDrawable(new TextureRegion(closeTex));

            closeStyle.imageUp = myDrawable;
            closeStyle.imageDown = myDrawable.tint(Color.LIGHT_GRAY); // Darken when pressed

        } catch (Exception e) {
            Gdx.app.error("HUD", "close_btn.png not found.");
            closeStyle.imageUp = game.skin.newDrawable("white", Color.RED); // Fallback
        }

        ImageButton closeBtn = new ImageButton(closeStyle);

        // Optional: Add hover animation if you want it to pop
        closeBtn.setTransform(true);
        closeBtn.setOrigin(20, 20); // Center of 40x40
        closeBtn.addListener(new HoverListener());

        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hideTileInfo();
            }
        });

        // Keep your original padding logic
        tileInfoTable.add(closeBtn).size(40, 40).padRight(20);

        // 4. POSITIONING
        float panelHeight = 80f;
        // Ensure X starts at 0 so it doesn't slide diagonally
        tileInfoTable.setPosition(0, -panelHeight);
        tileInfoTable.setSize(stage.getWidth(), panelHeight);

        stage.addActor(tileInfoTable);
    }

    // --- PUBLIC METHODS FOR CONTROLLER TO CALL ---
    public void showTileInfo(String name, TextureRegion region) {
        // 1. Update Data
        tileInfoLabel.setText(name);
        tileInfoImage.setDrawable(new TextureRegionDrawable(region));

        // 2. FORCE POSITION & SIZE UPDATES (Fixes the diagonal slide)
        // Ensure it spans the full width of the CURRENT screen size
        tileInfoTable.setWidth(stage.getWidth());
        // Force X to 0 so it doesn't start from the right/center
        tileInfoTable.setX(0);

        // 3. ANIMATION
        tileInfoTable.clearActions();
        bottomContainer.clearActions();

        // Move straight UP to Y=0
        tileInfoTable.addAction(Actions.moveTo(0, 0, 0.3f, Interpolation.pow2Out));

        // Hide Bottom HUD
        bottomContainer.addAction(Actions.moveBy(0, -bottomContainer.getHeight(), 0.3f, Interpolation.pow2Out));
    }

    public void hideTileInfo() {
        tileInfoTable.clearActions();
        bottomContainer.clearActions();

        // Move Tile Info DOWN (Hidden)
        tileInfoTable.addAction(Actions.moveTo(0, -tileInfoTable.getHeight(), 0.3f, Interpolation.pow2In));

        // Move Bottom Buttons UP (Visible)
        // We use moveTo(x, 0) because 0 is the default bottom position in the Root Table
        // Note: Since it's in a Table layout, '0' is relative to its cell. 
        // Ideally, we just reset the translation offset to 0.
        bottomContainer.addAction(Actions.moveTo(bottomContainer.getX(), 0, 0.3f, Interpolation.pow2In));
    }

    private void configureSummonMenu(final GameInputController inputController,
            final UnitFactory unitFactory) {
        summonMenu.setVisible(false);
        summonMenu.setBackground(game.skin.newDrawable("white", Color.DARK_GRAY));
        summonMenu.setSize(200, 150);

        // Position set in resize()
        Label summonLabel = new Label("Summon Unit", game.skin);
        TextButton recruitBtn = new TextButton("Recruit", game.skin);
        TextButton cancelBtn = new TextButton("Cancel", game.skin);

        recruitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int tx = inputController.getLastClickedX();
                int ty = inputController.getLastClickedY();

                if (tx != -1 && ty != -1) {
                    unitFactory.createRecruit(tx, ty, currentBaseOwner);
                    summonMenu.setVisible(false);
                    inputController.resetLastClicked();
                }
            }
        });

        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                summonMenu.setVisible(false);
                inputController.resetLastClicked();
            }
        });

        recruitBtn.addListener(new HoverListener());
        cancelBtn.addListener(new HoverListener());

        summonMenu.add(summonLabel).pad(10).row();
        summonMenu.add(recruitBtn).fillX().pad(5).row();
        summonMenu.add(cancelBtn).fillX().pad(5);

        stage.addActor(summonMenu);
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

        // --- NEW: FOG TOGGLE ---
        final TextButton fogBtn = new TextButton("Toggle Fog: ON", game.skin);
        fogBtn.addListener(new HoverListener());
        fogBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean newState = screen.toggleFog(); // Call toggle method
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
        menuBox.add(fogBtn).size(200, 50).pad(10).row(); // Add fog button
        menuBox.add(saveExitBtn).size(200, 50).pad(10).row();
        menuBox.add(resumeBtn).size(200, 50).pad(10);
        settingsOverlay.add(menuBox).size(300, 300); // Increased height

        stage.addActor(settingsOverlay);
    }

    private Table createStatGroup(String title, String placeholderValue) {
        Table t = new Table();
        Label titleLbl = new Label(title, game.skin, "default-font", Color.WHITE);
        titleLbl.setFontScale(0.8f);
        Label valLbl = new Label(placeholderValue, game.skin, "default-font", Color.WHITE);
        valLbl.setFontScale(1.2f);

        if (title.equals("XP")) {
            xpLabel = valLbl;
        }
        if (title.equals("Funding")) {
            fundsLabel = valLbl;
        }
        if (title.equals("Turn")) {
            turnLabel = valLbl;
        }

        t.add(titleLbl).row();
        t.add(valLbl);
        return t;
    }

    private ImageButton createCircleButton(String iconName) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();

        try {
            // 1. Load the Texture Region once
            Texture texture = new Texture(iconName + ".png");
            // Linear filter makes scaling look smoother
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegion region = new TextureRegion(texture);

            // 2. Create the "Normal" Drawable
            TextureRegionDrawable drawable = new TextureRegionDrawable(region);
            style.imageUp = drawable;

            // 3. Create the "Clicked" Drawable (Highlight)
            // .tint(Color) creates a new drawable that is colored automatically.
            // Color.GRAY makes it look dark/pressed. 
            // You could use Color.YELLOW if you want it to glow gold.
            style.imageDown = drawable.tint(Color.GRAY);

        } catch (Exception e) {
            Gdx.app.error("HUD", "Icon not found: " + iconName);
        }

        ImageButton btn = new ImageButton(style);

        // Enable Animation Settings
        btn.setTransform(true);
        btn.setSize(60, 60);
        btn.setOrigin(btn.getWidth() / 2f, btn.getHeight() / 2f);

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
