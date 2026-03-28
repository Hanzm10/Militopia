package com.militopia.net;

/**
 * Simple network message POJO for LAN multiplayer.
 * Serialized to/from JSON via libGDX Json.
 */
public class NetworkMessage {

    public static final String TYPE_GAME_INIT = "GAME_INIT";
    public static final String TYPE_END_TURN = "END_TURN";
    public static final String TYPE_DISCONNECT = "DISCONNECT";

    public String type;
    public String payload;

    /** Default constructor required for JSON deserialization. */
    public NetworkMessage() {
    }

    public NetworkMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public static NetworkMessage gameInit(String gameStateJson) {
        return new NetworkMessage(TYPE_GAME_INIT, gameStateJson);
    }

    public static NetworkMessage endTurn(String snapshotJson) {
        return new NetworkMessage(TYPE_END_TURN, snapshotJson);
    }

    public static NetworkMessage disconnect() {
        return new NetworkMessage(TYPE_DISCONNECT, "");
    }
}
