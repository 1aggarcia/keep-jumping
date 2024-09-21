package io.github.aggarcia.shared;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SocketMessage {
    GAME_UPDATE("gameUpdate"),
    GAME_OVER_UPDATE("gameOverUpdate"),
    PLAYER_CONTROL_UPDATE("playerControlUpdate"),
    SERVER_ERROR("serverError");

    @JsonValue
    public final String jsonValue;

    SocketMessage(String value) {
        this.jsonValue = value;
    }
}
