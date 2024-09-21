package io.github.aggarcia.networking;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.game.GameLoop;
import io.github.aggarcia.players.Player;
import io.github.aggarcia.players.PlayerEventHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * State management for client sessions. Externally, state is read only.
 * None of the methods should throw exepctions, since that would cause the
 * server to crash.
 */
public class ConnectionHandler extends TextWebSocketHandler {
    private static final int INSTANCE_ID = RandomUtil.getPositiveInt() % 999;
    private static final int IDLE_TIMEOUT_SECONDS = 5 * 60;  // 5 minutes

    // Server state
    private final Set<WebSocketSession> sessions =
        Collections.synchronizedSet(new HashSet<>());

    // Game state
    private final Map<String, Player> players =
        Collections.synchronizedMap(new HashMap<>());

    // Game loop state
    private GameLoop gameLoop = new GameLoop().onIdleTimeout(() -> {
        System.out.println(
            "Shutting down: Idle timeout reached ("
            + IDLE_TIMEOUT_SECONDS + "s)"
        );
        System.exit(0);
    }, IDLE_TIMEOUT_SECONDS * 1000);

    /**
     * Record the client session when a new client connects,
     * create a new player for the client.
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
        var newPlayer = Player.createRandomPlayer();
        players.put(session.getId(), newPlayer);
        if (!gameLoop.isRunning()) {
            gameLoop.start(sessions, players);
        }

        // Entire string needs to be printed at once since the console is
        // shared with other threads
        var consoleMessage = new StringBuilder()
            .append(INSTANCE_ID)
            .append(" - New player joined")
            .append(" (total " + sessions.size() + ")")
            .toString();
        System.out.println(consoleMessage);
    }

    /**
     * Remove the client session from memory when the client disconnects,
     * remove the instance of the player.
     */
    @Override
    public void afterConnectionClosed(
        @NonNull WebSocketSession session, @NonNull CloseStatus status
    ) {
        if (!sessions.remove(session)) {
            System.err.println(
                "No session was saved with id " + session.getId());
        }
        if (players.remove(session.getId()) == null) {
            System.err.println(
                "No player was saved for session " + session.getId());
        }

        // Entire string needs to be printed at once since the console is
        // shared with other threads
        var consoleMessage = new StringBuilder()
            .append(INSTANCE_ID)
            .append(" - Player left")
            .append(" (total " + sessions.size() + ")")
            .toString();
        System.out.println(consoleMessage);
    }

    /**
     * Respond to client messages.
     */
    @Override
    public void handleTextMessage(
        @NonNull WebSocketSession session, @NonNull TextMessage message
    ) {
        try {
            var player = players.get(session.getId());
            if (player == null) {
                System.err.println("Player not found, id: " + session.getId());
                return;
            }
            var velocity = PlayerEventHandler.computeVelocity(message);
            player.xVelocity(velocity.xVelocity());
            player.yVelocity(velocity.yVelocity());
        } catch (JsonProcessingException e) {
            System.err.println(e);
        }
    }

    /**
     * @return read only list of active web socket sessions
     */
    public List<WebSocketSession> sessions() {
        return this.sessions.stream().toList();
    }

    /**
     * @return read only list of players
     */
    public List<Player> players() {
        return this.players.values().stream().toList();
    }
}
