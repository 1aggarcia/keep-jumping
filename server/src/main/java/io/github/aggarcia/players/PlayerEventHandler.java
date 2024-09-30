package io.github.aggarcia.players;

import java.util.HashSet;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class PlayerEventHandler {
    private static final int PLAYER_SPEED = 20;

    public record PlayerVelocity(
        int xVelocity,
        int yVelocity
    ) {}

    private PlayerEventHandler() {}

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

        int xVelocity = 0;
        int yVelocity = 0;
        var pressedControls = new HashSet<>(data.pressedControls());

        // Prioritizes right over left - arbitrary decision
        if (pressedControls.contains(PlayerControl.RIGHT)) {
            xVelocity = PLAYER_SPEED;
        } else if (pressedControls.contains(PlayerControl.LEFT)) {
            xVelocity = -PLAYER_SPEED;
        }

        // Prioritizes down over up - also arbitrary
        if (pressedControls.contains(PlayerControl.DOWN)) {
            yVelocity = PLAYER_SPEED;
        } else if (pressedControls.contains(PlayerControl.UP)) {
            yVelocity = -PLAYER_SPEED;
        }
        return new PlayerVelocity(xVelocity, yVelocity);
    }
}
