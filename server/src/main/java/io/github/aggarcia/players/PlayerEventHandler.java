package io.github.aggarcia.players;

import java.util.HashSet;
import java.util.List;

import io.github.aggarcia.game.GameEventHandler;
import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.generated.SocketMessageOuterClass.ControlChangeEvent;
import io.github.aggarcia.generated.SocketMessageOuterClass.JoinEvent;
import io.github.aggarcia.generated.SocketMessageOuterClass.PlayerControl;
import io.github.aggarcia.generated.SocketMessageOuterClass.SocketMessage;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.updates.CreateFirstPlayer;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.ErrorUpdate;
import io.github.aggarcia.players.updates.GameUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;
import static io.github.aggarcia.players.PlayerStore.SPAWN_HEIGHT;

/**
 * Collection of (static) pure functions which determine how to update game
 * state based on client events.
 */
public final class PlayerEventHandler {
    private static final int PLAYER_MOVE_SPEED = 20;
    protected static final int PLAYER_JUMP_SPEED = 40;
    protected static final int MAX_PLAYER_COUNT = 15;
    protected static final int MAX_NAME_LENGTH = 25;

    private PlayerEventHandler() {}

    /**
     * Dispatch client events to the correct handler, return the
     * result.
     * @param client client, session used for identification of player
     * @param event message containing event
     * @param store complete game state to determine updates.
     *  Treated as read only
     * @return response, either PlayerVelocity or an empty Object
     */
    public static GameUpdate processEvent(
        String client,
        SocketMessage event,
        GameStore store
    ) {
        // this is the coolest thing ive ever seen Java do
        return switch (event.getPayloadCase()) {
            case CONTROLCHANGEEVENT ->
                processControlChange(
                    client, event.getControlChangeEvent(), store
                );
            case JOINEVENT ->
                processJoin(
                    client, event.getJoinEvent(), store
                );
            default ->
                ErrorUpdate.fromText(
                    "Unsupported event type: " + event.getPayloadCase()
                );
        };
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
    public static GameUpdate
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
        PlayerStore player = sessions.get(client);
        int oldYVelocity;
        // read y velocity once before other threads can change it
        synchronized (player) {
            oldYVelocity = player.yVelocity();
        }


        int newXVelocity = 0;
        int newYVelocity = oldYVelocity;
        var pressedControls = new HashSet<>(event.getPressedControlsList());

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
    public static GameUpdate
    processJoin(String client, JoinEvent event, GameStore store) {
        String name = event.getName();
        if (name.isEmpty()) {
            return ErrorUpdate.fromText("Username cannot be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return ErrorUpdate.fromText("Username is too long");
        }

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
            .anyMatch(player -> player.name().equalsIgnoreCase(name));
        if (isUsernameTaken) {
            return ErrorUpdate
                .fromText("Username already in use: " + name);
        }

        // validation complete
        var choicePlatforms = store.platforms().isEmpty()
            ? GameEventHandler.spawnInitPlatforms()
            : store.platforms();
        var choicePlatform = choosePlatformForPlayer(choicePlatforms);
        var newPlayer = PlayerStore.createAbovePlatform(name, choicePlatform);

        if (players.isEmpty()) {
            return new CreateFirstPlayer(client, newPlayer, choicePlatforms);
        }
        return new CreatePlayer(client, newPlayer);
    }

    /**
     * Choose the platform that is closest to the top of the screen, but
     * leaving some room above to spawn a player.
     * @param platforms there must be at least one platform
     * @return platform with y closest to but not smaller than SPAWN_HEIGHT
     */
    private static GamePlatform choosePlatformForPlayer(
        List<GamePlatform> platforms
    ) {
        if (platforms.isEmpty()) {
            throw new IllegalArgumentException("No platforms to choose from");
        }
        // minimize by y with lower bound of SPAWN_HEIGHT
        return platforms
            .stream()
            .reduce((old, curr) -> {
                if (old.y() <= curr.y()) {
                    return old;
                }
                if (curr.y() < SPAWN_HEIGHT) {
                    return old;
                }
                return curr;
            })
            .get();
    }
}
