package com.example.game.sessions;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.game.game.GameLoop;
import com.example.game.players.Player;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.qos.logback.core.testUtil.RandomUtil;

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
public class SessionManager extends TextWebSocketHandler {
    private static final int INSTANCE_ID = RandomUtil.getPositiveInt() % 999;
    private static final int IDLE_TIMEOUT_SECONDS = 5;  // 5 minutes

    private static final MessageHandler handler = new MessageHandler();

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

        System.out.print(INSTANCE_ID);
        System.out.print(" - New player joined");
        System.out.printf(" (total %d)\n", sessions.size());
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

        System.out.print(INSTANCE_ID);
        System.out.print(" - Player left");
        System.out.printf(" (total %d)\n", sessions.size());
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
            var velocity = handler.computeVelocity(message);
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
