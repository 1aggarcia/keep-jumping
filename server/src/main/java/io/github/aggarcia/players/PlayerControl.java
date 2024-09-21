package io.github.aggarcia.players;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PlayerControl {
    UP("Up"),
    DOWN("Down"),
    LEFT("Left"),
    RIGHT("Right");

    @JsonValue
    public final String jsonValue;

    PlayerControl(String value) {
        this.jsonValue = value;
    }
}
