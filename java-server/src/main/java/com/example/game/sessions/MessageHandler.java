package com.example.game.sessions;

import java.util.Map;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            case "playerControlUpdate" -> GameUpdate.fromPlayerState(players);
            default -> serverError("Unsupported message type: " + type);
        };
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
