package io.github.aggarcia.players;

import java.util.HashSet;
import java.util.Map;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.players.events.ControlChangeEvent;
import io.github.aggarcia.players.events.JoinEvent;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.ErrorUpdate;
import io.github.aggarcia.players.updates.PlayerUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;
import io.github.aggarcia.shared.SocketMessage;

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
        SocketMessage payload = new ObjectMapper()
            .readValue(event.getPayload(), SocketMessage.class);

        if (payload instanceof ControlChangeEvent control) {
            return processControlChange(client, control, sessions);
        } else if (payload instanceof JoinEvent join) {
            return processJoin(client, join, sessions);
        } else {
            return new ErrorUpdate("Unsupported event type: " + payload);
        }
    }

    /**
     * Computes the new player velocity according to the controls in the event 
     * @param client id for player
     * @param event event with controls
     * @param sessions reference to game state 
     * @return The player velocity according to the keys pressed in the
     *  incoming event. Behavior is undefined if two conflicting keys
     *  are pressed in the incoming message, e.g. "Right" and "Left".
     * @throws JsonProcessingException
     */
    public static PlayerUpdate
    processControlChange(
        String client,
        ControlChangeEvent event,
        Map<String, Player> sessions
    ) throws JsonProcessingException {
        if (!sessions.containsKey(client)) {
            return new ErrorUpdate("Player does not exist with id " + client);
        }

        // TODO: use previous player velocity, not 0
        int xVelocity = 0;
        int yVelocity = 0;
        var pressedControls = new HashSet<>(event.pressedControls());

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

    /**
     * Instantiates a new player according to the passed in name,
     * if the name is unique
     * @param client id for clinet
     * @param event 
     * @param sessions current game state
     * @return A new player with the name in the event, if the name is unique.
     *  otherwise, an error
     */
    public static CreatePlayer processJoin(
        String client,
        JoinEvent event,
        Map<String, Player> sessions
    ) {
        if (sessions.containsKey(client)) {
            // client already has a player
            return new CreatePlayer(true, null, null);
        }
        for (var player : sessions.values()) {
            if (player.name().equals(event.name())) {
                // name already taken
                return new CreatePlayer(true, null, null);
            }
        }
        var newPlayer = Player.createRandomPlayer(event.name());
        return new CreatePlayer(false, client, newPlayer);
    }
}
