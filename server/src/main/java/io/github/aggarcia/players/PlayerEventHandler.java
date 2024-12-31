package io.github.aggarcia.players;

import java.util.HashSet;

import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.platforms.GamePlatform;
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
    protected static final int PLAYER_JUMP_SPEED = 40;
    protected static final int MAX_PLAYER_COUNT = 15;

    private PlayerEventHandler() {}

    /**
     * Dispatch client events to the correct handler, return the
     * result.
     * @param client client, session used for identification of player
     * @param event message containing JSON event
     * @param store complete game state to determine updates.
     *  Treated as read only
     * @return response, either PlayerVelocity or an empty Object
     */
    public static PlayerUpdate processEvent(
        String client,
        TextMessage event,
        GameStore store
    ) throws JsonProcessingException {
        SocketMessage payload = new ObjectMapper()
            .readValue(event.getPayload(), SocketMessage.class);

        if (payload instanceof ControlChangeEvent control) {
            return processControlChange(client, control, store);
        } else if (payload instanceof JoinEvent join) {
            return processJoin(client, join, store);
        } else {
            return ErrorUpdate.fromText("Unsupported event type: " + payload);
        }
    }

    /**
     * Computes the new player velocity according to the controls in the event
     * and the current player velocity.
     * @param client id for player
     * @param event event with controls
     * @param sessions reference to game state
     * @return The new player velocity. Behavior is undefined if two
     *  conflicting keys are pressed in the incoming message, e.g.
     *  "Right" and "Left".
     */
    public static PlayerUpdate
    processControlChange(
        String client,
        ControlChangeEvent event,
        GameStore store
    ) {
        var sessions = store.players();
        if (!sessions.containsKey(client)) {
            return ErrorUpdate
                .fromText("No player exists for client " + client);
        }
        Player player = sessions.get(client);
        int oldYVelocity;
        // read y velocity once before other threads can change it
        synchronized (player) {
            oldYVelocity = player.yVelocity();
        }


        int newXVelocity = 0;
        int newYVelocity = oldYVelocity;
        var pressedControls = new HashSet<>(event.pressedControls());

        // Prioritizes right over left - arbitrary decision
        if (pressedControls.contains(PlayerControl.RIGHT)) {
            newXVelocity = PLAYER_MOVE_SPEED;
        } else if (pressedControls.contains(PlayerControl.LEFT)) {
            newXVelocity = -PLAYER_MOVE_SPEED;
        }

        boolean isPressingUp = pressedControls.contains(PlayerControl.UP);
        // not great since this allows mid-air jumping
        boolean canJump = (
            0 <= oldYVelocity
            && oldYVelocity < (2 * GamePlatform.PLATFORM_GRAVITY)
        );
        if (isPressingUp && canJump) {
            newYVelocity = -PLAYER_JUMP_SPEED;
        } else if (!isPressingUp && oldYVelocity < 0) {
            newYVelocity = 0;
        }
        return new UpdateVelocity(client, newXVelocity, newYVelocity);
    }

    /**
     * Instantiates a new player according to the passed in name,
     * if the name is unique.
     * @param client id for client
     * @param event
     * @param store current game state
     * @return A new player with the name in the event, if the name is unique.
     *  otherwise, an error.
     */
    public static PlayerUpdate
    processJoin(String client, JoinEvent event, GameStore store) {
        var players = store.players();
        if (players.size() >= MAX_PLAYER_COUNT) {
            return ErrorUpdate
                .fromText("Player limit reached: " + players.size());
        }
        if (players.containsKey(client)) {
            return ErrorUpdate
                .fromText("Client is already playing: " + client);
        }

        boolean isUsernameTaken = players
            .values()
            .stream()
            .anyMatch(player -> player.name().equalsIgnoreCase(event.name()));
        if (isUsernameTaken) {
            return ErrorUpdate
                .fromText("Username already in use: " + event.name());
        }
        var isFirstPlayer = players.isEmpty();
        var newPlayer = Player.createRandomPlayer(event.name());
        return new CreatePlayer(isFirstPlayer, client, newPlayer);
    }
}
