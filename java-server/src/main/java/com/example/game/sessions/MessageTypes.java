package com.example.game.sessions;

import java.util.List;
import java.util.Map;

import com.example.game.players.Player;
import com.example.game.players.PlayerControl;
import com.example.game.players.PlayerState;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Different types of messages that may be sent or received through
 * the web socket.
 */
public class MessageTypes {
    public enum SocketMessageType {
        GAME_UPDATE("gameUpdate"),
        GAME_OVER_UPDATE("gameOverUpdate"),
        PLAYER_CONTROL_UPDATE("playerControlUpdate"),
        SERVER_ERROR("serverError");

        @JsonValue
        public final String jsonValue;

        SocketMessageType(String value) {
            this.jsonValue = value;
        }
    }

    public interface SocketMessage {
        SocketMessageType type();
    }

    public static record GameUpdate(
        SocketMessageType type,
        int serverAge,
        List<PlayerState> players
    ) implements SocketMessage {

        /**
         * Factory function to create a game update message based on
         * the current state of players.
         * @param players list of players in the game
         * @return new instance of GameUpdate
         */
        public static GameUpdate fromPlayerState(Map<String, Player> players) {
            List<PlayerState> playersState = players.values()
                .stream()
                .map(player -> player.toPlayerState())
                .toList();

            return new GameUpdate(
                SocketMessageType.GAME_UPDATE,
                0,
                playersState
            );
        }
    }

    public static record GameOverUpdate(
        SocketMessageType type,
        String reason
    ) implements SocketMessage {}

    public static record PlayerControlUpdate(
        SocketMessageType type,
        PlayerControl[] pressedControls
    ) implements SocketMessage {}

    public static record ErrorResponse(
        SocketMessageType type,
        String message
    ) implements SocketMessage {}
}

