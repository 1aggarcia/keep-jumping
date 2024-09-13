package com.example.game.sessions;

import java.util.HashSet;
import java.util.Map;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.game.game.GameConstants;
import com.example.game.players.Player;
import com.example.game.players.PlayerControl;
import com.example.game.sessions.MessageTypes.PlayerControlUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Collection of methods defining how to respond to a client message.
 * This module has no state.
 */
public class MessageHandler {
    private static final ObjectMapper objectMapper = Singletons.objectMapper;
    private static final int PLAYER_SPEED = 10;
    private static final int TICKS_PER_SECOND =
        1000 / GameConstants.TICK_DELAY_MS;

    public record PlayerVelocity(
        int xVelocity,
        int yVelocity
    ) {}

    /** Response produced by advancing the game tick. */
    public record TickResponse(
        boolean isUpdateNeeded,
        int nextTickCount
    ) {}

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
    public PlayerVelocity
    computeVelocity(TextMessage message) throws JsonProcessingException {
        PlayerControlUpdate data = objectMapper.readValue(
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

    /**
     * Move all players to thier position in the next tick.
     * @param players current state of all players
     * @returns response with isUpdatedNeeded flag and nextTickCount
     */
    public TickResponse
    advanceToNextTick(Map<String, Player> players, int tickCount) {
        boolean isUpdateNeeded = false;
        int nextTickCount = (tickCount + 1) % TICKS_PER_SECOND;
        if (nextTickCount == 0) {
            isUpdateNeeded = true;  // update needed to refresh the server age
        }

        for (Player player : players.values()) {
            if (player.xVelocity() == 0 && player.yVelocity() == 0) {
                continue;
            }
            player.moveToNextTick();
            isUpdateNeeded = true;
        }

        return new TickResponse(isUpdateNeeded, nextTickCount);
    }
}
