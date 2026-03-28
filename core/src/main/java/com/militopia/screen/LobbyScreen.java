package com.militopia.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.militopia.MilitopiaGame;
import com.militopia.data.GameState;
import com.militopia.managers.VideoBackgroundManager;
import com.militopia.net.NetworkManager;
import com.militopia.net.NetworkMessage;
import com.militopia.utils.GameLogger;
import com.militopia.utils.HoverListener;

/**
 * LAN Lobby screen — allows hosting or joining a LAN game.
 *
 * HOST FLOW:
 *   1. Player configures game (name, seed, mode) and clicks "Host Game"
 *   2. Screen shows the local IP and "Waiting for opponent..."
 *   3. On client connect → host sends GAME_INIT → both enter GameScreen
 *
 * JOIN FLOW:
 *   1. Player sees auto-discovered hosts or enters IP manually
 *   2. Clicks "Join" → connects to host → receives GAME_INIT → enters GameScreen
 */
public class LobbyScreen implements Screen {

    private enum LobbyState { CHOOSE, HOST_CONFIG, HOST_WAITING, JOIN_SCAN }

    final MilitopiaGame game;
    Stage stage;
    LobbyState lobbyState = LobbyState.CHOOSE;

    // Host config
    TextField nameField, seedField, hostNameField;
    SelectBox<String> modeSelectBox;
    Label modeInfoLabel;
    int selectedWidth = 16;
    int selectedHeight = 16;

    // UI
    Table rootTable;
    Label statusLabel;
    Label errorLabel;

    // Join
    TextField ipField, clientNameField;

    // Name exchange state
    private String hostPlayerName = "Player 1";
    private String clientPlayerName = "Player 2";
    private boolean clientNameSent = false;
    private boolean clientNameReceived = false;
    private String receivedClientName = "Player 2";

    // Networking
    NetworkManager networkManager;
    private boolean transitioning = false;

    private final Json json = new Json();

    public LobbyScreen(final MilitopiaGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        showChooseScreen();
    }

    // =========================================================================
    // CHOOSE SCREEN (Host / Join)
    // =========================================================================

    private void showChooseScreen() {
        lobbyState = LobbyState.CHOOSE;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("LAN Game", game.skin);
        title.setFontScale(1.2f);

        TextButton hostBtn = new TextButton("Host Game", game.skin, "militopia-btn");
        hostBtn.addListener(new HoverListener());
        TextButton joinBtn = new TextButton("Join Game", game.skin, "militopia-btn");
        joinBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        rootTable.add(title).pad(20).colspan(2).row();
        rootTable.add(hostBtn).fillX().width(300).pad(10).row();
        rootTable.add(joinBtn).fillX().width(300).pad(10).row();
        rootTable.add(backBtn).fillX().width(300).pad(10).row();

        hostBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showHostConfig();
            }
        });

        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showJoinScan();
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
    }

    // =========================================================================
    // HOST CONFIG SCREEN
    // =========================================================================

    private void showHostConfig() {
        lobbyState = LobbyState.HOST_CONFIG;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("Host a LAN Game", game.skin);
        title.setFontScale(1.0f);

        hostNameField = new TextField("", game.skin);
        hostNameField.setMessageText("Your Name");

        nameField = new TextField("", game.skin);
        nameField.setMessageText("Game Name");

        seedField = new TextField("", game.skin);
        seedField.setMessageText("Seed");

        // --- MODE SELECTOR ---
        modeSelectBox = new SelectBox<>(game.skin);
        modeSelectBox.setItems("Game mode", "Blitz", "Marathon");
        modeSelectBox.setSelected("Game mode");

        modeInfoLabel = new Label("16x16 map", game.skin);
        modeInfoLabel.setColor(Color.LIGHT_GRAY);
        modeInfoLabel.setFontScale(0.8f);
        modeInfoLabel.setVisible(false);

        modeSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = modeSelectBox.getSelected();
                if (selected.contains("Blitz")) {
                    modeInfoLabel.setText("16x16 map");
                    selectedWidth = 16; selectedHeight = 16;
                } else if (selected.contains("Marathon")) {
                    modeInfoLabel.setText("32x32 map");
                    selectedWidth = 32; selectedHeight = 32;
                } else {
                    modeInfoLabel.setText("16x16 map");
                    selectedWidth = 16; selectedHeight = 16;
                }
            }
        });

        modeSelectBox.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) modeInfoLabel.setVisible(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1) modeInfoLabel.setVisible(false);
            }
        });

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);

        TextButton startBtn = new TextButton("Start Hosting", game.skin, "militopia-btn");
        startBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        rootTable.add(title).colspan(2).pad(15).row();
        rootTable.add(hostNameField).width(300).pad(10);
        rootTable.row();
        rootTable.add(nameField).width(300).pad(10);
        rootTable.row();
        rootTable.add(seedField).width(300).pad(10);
        rootTable.row();

        // Add Mode Selection Row using balancing spacer trick
        Table modeTable = new Table();
        modeTable.add().width(110); 
        modeTable.add(modeSelectBox).width(300).pad(5);
        modeTable.add(modeInfoLabel).width(100).padLeft(10);
        
        rootTable.row().pad(10);
        rootTable.add(modeTable).left().row();

        rootTable.add(errorLabel).colspan(2).pad(10).row();

        rootTable.add(errorLabel).colspan(2).pad(10).row();

        Table btnRow = new Table();
        btnRow.add(backBtn).fillX().width(200).pad(10);
        btnRow.add(startBtn).fillX().width(200).pad(10);
        rootTable.add(btnRow).colspan(2).padTop(120).row();

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (nameField.getText().trim().isEmpty() || seedField.getText().trim().isEmpty()) {
                    errorLabel.setText("All fields are required!");
                    return;
                }
                String hn = hostNameField.getText().trim();
                hostPlayerName = hn.isEmpty() ? "Player 1" : hn;
                clientNameReceived = false;
                receivedClientName = "Player 2";
                beginHosting();
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showChooseScreen();
            }
        });
    }

    private void beginHosting() {
        lobbyState = LobbyState.HOST_WAITING;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        String localIP = NetworkManager.getLocalIP();

        Label title = new Label("Waiting for Opponent...", game.skin);
        title.setFontScale(0.9f);
        Label ipLabel = new Label("Your IP: " + localIP, game.skin);
        ipLabel.setColor(Color.YELLOW);
        statusLabel = new Label("Broadcasting on LAN...", game.skin);
        statusLabel.setFontScale(0.7f);
        statusLabel.setColor(Color.LIGHT_GRAY);

        TextButton cancelBtn = new TextButton("Cancel", game.skin, "militopia-btn");
        cancelBtn.addListener(new HoverListener());

        rootTable.add(title).pad(20).row();
        rootTable.add(ipLabel).pad(10).row();
        rootTable.add(statusLabel).pad(10).row();
        rootTable.add(cancelBtn).fillX().width(200).pad(20).row();

        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (networkManager != null) networkManager.disconnect();
                showChooseScreen();
            }
        });

        // Start networking
        networkManager = new NetworkManager();
        networkManager.startHost();
    }

    // =========================================================================
    // JOIN SCREEN
    // =========================================================================

    private void showJoinScan() {
        lobbyState = LobbyState.JOIN_SCAN;
        stage.clear();

        rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        Label title = new Label("Join a LAN Game", game.skin);
        title.setFontScale(1.0f);

        statusLabel = new Label("Scanning for hosts...", game.skin);
        statusLabel.setFontScale(0.7f);
        statusLabel.setColor(Color.LIGHT_GRAY);

        clientNameField = new TextField("", game.skin);
        clientNameField.setMessageText("Your Name");

        ipField = new TextField("", game.skin);
        ipField.setMessageText("Enter Host IP");

        TextButton connectBtn = new TextButton("Connect", game.skin, "militopia-btn");
        connectBtn.addListener(new HoverListener());
        TextButton backBtn = new TextButton("Back", game.skin, "militopia-btn");
        backBtn.addListener(new HoverListener());

        Label.LabelStyle errorStyle = new Label.LabelStyle(game.skin.getFont("default-font"), Color.RED);
        errorLabel = new Label("", errorStyle);

        rootTable.add(title).colspan(2).pad(15).row();
        rootTable.add(clientNameField).colspan(2).width(300).pad(10).row();
        rootTable.add(statusLabel).colspan(2).pad(10).row();
        rootTable.add(new Label("Host IP:", game.skin)).right().pad(5);
        rootTable.add(ipField).width(200).pad(5).row();
        rootTable.add(errorLabel).colspan(2).pad(10).row();

        Table btnRow = new Table();
        btnRow.add(backBtn).fillX().width(150).pad(10);
        btnRow.add(connectBtn).fillX().width(150).pad(10);
        rootTable.add(btnRow).colspan(2).row();

        connectBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String ip = ipField.getText().trim();
                if (ip.isEmpty()) {
                    errorLabel.setText("Enter the host's IP address!");
                    return;
                }
                attemptConnect(ip);
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (networkManager != null) networkManager.disconnect();
                showChooseScreen();
            }
        });

        // Start LAN discovery
        networkManager = new NetworkManager();
        networkManager.startDiscoveryListener();
    }

    private void attemptConnect(String ip) {
        statusLabel.setText("Connecting to " + ip + "...");
        statusLabel.setColor(Color.YELLOW);
        errorLabel.setText("");

        String cn = clientNameField.getText().trim();
        clientPlayerName = cn.isEmpty() ? "Player 2" : cn;
        clientNameSent = false;

        if (networkManager != null) networkManager.disconnect();
        networkManager = new NetworkManager();
        networkManager.connectToHost(ip);
    }

    // =========================================================================
    // Render / Update
    // =========================================================================

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        VideoBackgroundManager.getInstance().render(game.batch);

        // --- Network state polling ---
        if (networkManager != null && !transitioning) {

            // HOST: waiting for connection → wait for client name → send GAME_INIT
            if (lobbyState == LobbyState.HOST_WAITING) {
                if (networkManager.getState() == NetworkManager.State.CONNECTED && !clientNameReceived) {
                    NetworkMessage nameMsg = networkManager.poll();
                    if (nameMsg != null && NetworkMessage.TYPE_PLAYER_NAME.equals(nameMsg.type)) {
                        receivedClientName = nameMsg.payload.trim().isEmpty() ? "Player 2" : nameMsg.payload.trim();
                        clientNameReceived = true;
                    }
                }
                if (clientNameReceived && !transitioning) {
                    transitioning = true;
                    GameLogger.logScreen("LAN: Client connected, sending game init");

                    long seed;
                    try {
                        seed = Long.parseLong(seedField.getText());
                    } catch (Exception e) {
                        seed = seedField.getText().hashCode();
                    }

                    GameState newState = new GameState(seed, nameField.getText() + "_LAN", selectedWidth, selectedHeight);
                    newState.isLanGame = true;
                    newState.localPlayerID = 1; // Host is always Player 1
                    newState.p1Name = hostPlayerName;
                    newState.p2Name = receivedClientName;

                    // Send the game init to client
                    String stateJson = json.toJson(newState);
                    networkManager.send(NetworkMessage.gameInit(stateJson));

                    // Transition to game
                    game.setScreen(new GameScreen(game, newState, networkManager));
                    return;
                }
                if (networkManager.getState() == NetworkManager.State.ERROR) {
                    statusLabel.setText("Error: " + networkManager.getErrorMessage());
                    statusLabel.setColor(Color.RED);
                }
            }

            // JOIN: check for auto-discovered host IP
            if (lobbyState == LobbyState.JOIN_SCAN) {
                String discovered = networkManager.getDiscoveredHostIP();
                if (discovered != null && (ipField.getText().isEmpty())) {
                    ipField.setText(discovered);
                    statusLabel.setText("Found host: " + discovered);
                    statusLabel.setColor(Color.GREEN);
                }

                if (networkManager.getState() == NetworkManager.State.CONNECTED) {
                    if (!clientNameSent) {
                        networkManager.send(NetworkMessage.playerName(clientPlayerName));
                        clientNameSent = true;
                    }
                    statusLabel.setText("Connected! Waiting for game data...");
                    statusLabel.setColor(Color.GREEN);
                }

                if (networkManager.getState() == NetworkManager.State.ERROR) {
                    statusLabel.setText("Connection failed");
                    statusLabel.setColor(Color.RED);
                    errorLabel.setText(networkManager.getErrorMessage());
                }

                // Check for GAME_INIT message
                NetworkMessage msg = networkManager.poll();
                if (msg != null && NetworkMessage.TYPE_GAME_INIT.equals(msg.type)) {
                    transitioning = true;
                    GameLogger.logScreen("LAN: Received game init from host");

                    GameState receivedState = json.fromJson(GameState.class, msg.payload);
                    receivedState.isLanGame = true;
                    receivedState.localPlayerID = 2; // Client is always Player 2

                    game.setScreen(new GameScreen(game, receivedState, networkManager));
                    return;
                }
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        GameLogger.logScreen("LAN Lobby opened");
        VideoBackgroundManager.getInstance().play();
    }

    @Override
    public void hide() {
        VideoBackgroundManager.getInstance().pause();
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
        if (networkManager != null && !transitioning) {
            networkManager.disconnect();
        }
    }

    private void addInputRow(Table t, String labelText, TextField field) {
        t.add(new Label(labelText, game.skin)).right().pad(5);
        t.add(field).width(200).pad(5);
        t.row();
    }
}
