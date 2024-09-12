package com.example.game.sessions;

import java.util.Map;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.game.game.GameConstants;
import com.example.game.players.Player;
import com.example.game.sessions.MessageTypes.GameUpdate;
import com.example.game.sessions.MessageTypes.ErrorResponse;
import com.example.game.sessions.MessageTypes.SocketMessage;
import com.example.game.sessions.MessageTypes.SocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Collection of methods defining how to respond to a client message.
 * This module has no state.
 */
public class MessageHandler {
    private static final ObjectMapper objectMapper = Singletons.objectMapper;
    private static final int TICKS_PER_SECOND =
        1000 / GameConstants.TICK_DELAY_MS;

    /** Response produced by advancing the game tick. */
    public record TickResponse(
        boolean isUpdateNeeded,
        int nextTickCount
    ) {}

    // TODO: rewrite this method to return game update events
    // currently unused
    /**
     * Given a request from a client and map of players, produces the correct
     * response.
     * @param message the message from the client
     * @param players current state of all players
     * @return the response to be broadcast to all clients
     * @throws JsonProcessingException
     */
    public SocketMessage getResponse(
        TextMessage message,
        Map<String, Player> players
    ) throws JsonProcessingException {
        Map<String, Object> data = objectMapper.readValue(
            message.getPayload(),
            new TypeReference<Map<String, Object>>() {}
        );

        var type = String.valueOf(data.get("type"));
        return switch (type) {
            case "playerControlUpdate" -> GameUpdate.fromGameState(players, 0);
            default -> serverError("Unsupported message type: " + type);
        };
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

    /**
     * Shorthand for ServerError constructor.
     * @param message
     */
    private ErrorResponse
    serverError(String message) throws JsonProcessingException {
        return new ErrorResponse(SocketMessageType.SERVER_ERROR, message);
    }
}
