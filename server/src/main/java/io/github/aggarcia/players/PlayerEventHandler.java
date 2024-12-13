package io.github.aggarcia.players;

import java.util.HashSet;
import java.util.Map;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.players.messages.PlayerControlUpdate;

public final class PlayerEventHandler {
    private static final int PLAYER_MOVE_SPEED = 20;
    private static final int PLAYER_JUMP_SPEED = 40;

    public record PlayerVelocity(
        int xVelocity,
        int yVelocity
    ) {}

    private PlayerEventHandler() {}

    /**
     * Handle a client message, dispatch to the correct handler, return the
     * result.
     * @param message message, should either be a serialized
     * PlayerControlUpdate or PlayerJoinUpdate
     * @return response, either PlayerVelocity or an empty Object
     */
    public static Object processMessage(TextMessage message)
    throws JsonProcessingException {
        Map<String, Object> data = new ObjectMapper().readValue(
            message.getPayload(),
            new TypeReference<>() {}
        );
        if (!data.containsKey("type")) {
            throw new IllegalArgumentException("No type provided");
        }
        String type = data.get("type").toString();
        return switch (type) {
            case "playerControlUpdate" -> computeVelocity(message);
            case "playerJoinUpdate" -> data.get("name").toString();
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Computes the player velocity given a client message, treating it
     * as a PlayerControlUpdate object encoded as JSON.
     * @param message The message from the client. Must be a
     *  PlayerControlUpdate formatted as JSON.
     * @return The player velocity according to the keys pressed in the
     *  incoming message. Behavior is undefined if two conflicting keys
     *  are pressed in the incoming message, e.g. "Right" and "Left".
     * @throws JsonProcessingException
     */
    public static PlayerVelocity
    computeVelocity(TextMessage message) throws JsonProcessingException {
        PlayerControlUpdate data = new ObjectMapper().readValue(
            message.getPayload(),
            PlayerControlUpdate.class
        );

        // TODO: use previous player velocity, not 0
        int xVelocity = 0;
        int yVelocity = 0;
        var pressedControls = new HashSet<>(data.pressedControls());

        // Prioritizes right over left - arbitrary decision
        if (pressedControls.contains(PlayerControl.RIGHT)) {
            xVelocity = PLAYER_MOVE_SPEED;
        } else if (pressedControls.contains(PlayerControl.LEFT)) {
            xVelocity = -PLAYER_MOVE_SPEED;
        }

        if (pressedControls.contains(PlayerControl.UP)) {
            yVelocity = -PLAYER_JUMP_SPEED;
        }
        return new PlayerVelocity(xVelocity, yVelocity);
    }
}
