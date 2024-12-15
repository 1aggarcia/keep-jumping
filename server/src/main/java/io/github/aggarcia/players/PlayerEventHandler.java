package io.github.aggarcia.players;

import java.util.HashSet;
import java.util.Map;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.players.messages.PlayerControlUpdate;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.PlayerUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;

/**
 * Collection of (static) pure functions which determine how to update game
 * state based on client events.
 */
public final class PlayerEventHandler {
    private static final int PLAYER_MOVE_SPEED = 20;
    private static final int PLAYER_JUMP_SPEED = 40;

    private PlayerEventHandler() {}

    /**
     * Dispatch client events to the correct handler, return the
     * result.
     * @param client client, session used for identification of player
     * @param event message containing JSON event
     * @param sessions complete game state to determine updates.
     *  Treated as read only
     * @return response, either PlayerVelocity or an empty Object
     */
    public static PlayerUpdate processEvent(
        String client,
        TextMessage event,
        Map<String, Player> sessions
    ) throws JsonProcessingException {
        Map<String, Object> data = new ObjectMapper().readValue(
            event.getPayload(),
            new TypeReference<>() {}
        );
        if (!data.containsKey("type")) {
            throw new IllegalArgumentException("No type provided");
        }
        String type = data.get("type").toString();
        return switch (type) {
            case "playerControlUpdate" ->
                processControlUpdate(client, event, sessions);
            case "playerJoinUpdate" -> processJoinUpdate(
                client,
                data.get("name").toString(),
                sessions
            );
            default -> throw new IllegalArgumentException(
                "unknown event type: " + type);
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
    public static UpdateVelocity
    processControlUpdate(
        String client,
        TextMessage event,
        Map<String, Player> sessions
    ) throws JsonProcessingException {
        PlayerControlUpdate eventData = new ObjectMapper().readValue(
            event.getPayload(),
            PlayerControlUpdate.class
        );

        // TODO: use previous player velocity, not 0
        int xVelocity = 0;
        int yVelocity = 0;
        var pressedControls = new HashSet<>(eventData.pressedControls());

        // Prioritizes right over left - arbitrary decision
        if (pressedControls.contains(PlayerControl.RIGHT)) {
            xVelocity = PLAYER_MOVE_SPEED;
        } else if (pressedControls.contains(PlayerControl.LEFT)) {
            xVelocity = -PLAYER_MOVE_SPEED;
        }

        if (pressedControls.contains(PlayerControl.UP)) {
            yVelocity = -PLAYER_JUMP_SPEED;
        }
        return new UpdateVelocity(client, xVelocity, yVelocity);
    }

    public static CreatePlayer processJoinUpdate(
        String client,
        String name,
        Map<String, Player> sessions
    ) {
        if (sessions.containsKey(client)) {
            // client already has a player
            return new CreatePlayer(true, null, null);
        }
        for (var player : sessions.values()) {
            if (player.name().equals(name)) {
                // name already taken
                return new CreatePlayer(true, null, null);
            }
        }
        var newPlayer = Player.createRandomPlayer(name);
        return new CreatePlayer(false, client, newPlayer);
    }
}
