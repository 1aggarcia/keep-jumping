package com.example.game.sessions;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.game.game.GameConstants;
import com.example.game.players.Player;
import com.example.game.sessions.MessageTypes.GameUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.core.testUtil.RandomUtil;

import java.io.IOException;
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

    private static final ObjectMapper mapper = Singletons.objectMapper;
    private static final MessageHandler handler = new MessageHandler();

    // Server state
    private final Set<WebSocketSession> sessions =
        Collections.synchronizedSet(new HashSet<>());

    // Game state
    private final Map<String, Player> players =
        Collections.synchronizedMap(new HashMap<>());

    // Game loop state
    private Thread gameLoop;

    /**
     * Record the client session when a new client connects,
     * create a new player for the client.
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
        var newPlayer = Player.createRandomPlayer();
        players.put(session.getId(), newPlayer);
        if (gameLoop == null || !gameLoop.isAlive()) {
            gameLoop = new Thread(() -> runGameLoop(players));
            gameLoop.start();
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
        // TODO: send message to handler, process returned events
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


    // HELPERS //
    /**
     * Send a message to all open sessions.
     * @param message message of any type
     */
    private synchronized <T> void broadcast(T message) {
        try {
            for (WebSocketSession session: sessions) {
                safeSendMessage(session, mapper.writeValueAsString(message));
            }
        } catch (JsonProcessingException e) {
            System.err.println(e);
        }
    }

    /**
     * Try to send a message to a client with safety from data races
     * and exceptions. Exepctions are printed to the error stream.
     * @param session - client web socket session
     * @param message
     */
    private void safeSendMessage(WebSocketSession session, String message) {
        synchronized (session) {
            if (!session.isOpen()) return;
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Game loop thread function which runs as long as there is at least one
     * player in the map of players passed in. Every tick, the loop broadcasts
     * the current game state to all connected web socket sessions.
     *
     * @param players Reference to player state. Should be created via
     * `Collections.syncronizedMap`.
     */
    private void runGameLoop(Map<String, Player> players) {
        int serverAge = 0;  // in seconds
        int tickCount = 0;

        System.out.println("Starting game loop");
        while (players.size() > 0) {
            var response = handler.advanceToNextTick(players, tickCount);
            tickCount = response.nextTickCount();
            if (tickCount == 0) {
                serverAge++;
            }
            if (response.isUpdateNeeded()) {
                broadcast(GameUpdate.fromGameState(players, serverAge));
            }
            try {
                Thread.sleep(GameConstants.TICK_DELAY_MS);
            } catch (InterruptedException e) {
                System.err.println(e);
                Thread.interrupted();  // clears the interrupted state
            }
        }
        System.out.println("Closing game loop");
    }
}
