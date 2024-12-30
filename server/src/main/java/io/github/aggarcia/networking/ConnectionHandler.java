package io.github.aggarcia.networking;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.players.Player;
import static io.github.aggarcia.players.PlayerEventHandler.processEvent;

import java.io.IOException;
import java.util.List;

/**
 * State management for client sessions. Externally, state is read only.
 * None of the methods should throw exepctions, since that would cause the
 * server to crash.
 */
public class ConnectionHandler extends TextWebSocketHandler {
    private static final int INSTANCE_ID = RandomUtil.getPositiveInt() % 999;

    private final GameStore gameStore;

    public ConnectionHandler(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    /**
     * Record the client session when a new client connects,
     * create a new player for the client.
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        gameStore.sessions().add(session);
        // Entire string needs to be printed at once since the console is
        // shared with other threads
        var consoleMessage = new StringBuilder()
            .append(INSTANCE_ID)
            .append(" - New connection: ")
            .append(session.getId())
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
        var players = gameStore.players();
        if (players.remove(session.getId()) == null) {
            System.err.println(
                "No player was saved for session " + session.getId());
        }
        gameStore.sessions().remove(session);

        // Entire string needs to be printed at once since the console is
        // shared with other threads
        var consoleMessage = new StringBuilder()
            .append(INSTANCE_ID)
            .append(" - Player left")
            .append(" (total " + players.size() + ")")
            .toString();
        System.out.println(consoleMessage);
    }

    /**
     * Act on client events. Wrapper for logic in `PlayerEventHandler`.
     */
    @Override
    public void handleTextMessage(
        @NonNull WebSocketSession client, @NonNull TextMessage event
    ) {
        try {
            if (!gameStore.sessions().contains(client)) {
                return;
            }
            var update = processEvent(client.getId(), event, gameStore);
            update.applyTo(gameStore);
            if (!update.reply().isPresent()) {
                return;
            }
            synchronized (client) {
                var message = new TextMessage(update.reply().get());
                client.sendMessage(message);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * @return read only list of active web socket sessions
     */
    public List<WebSocketSession> sessions() {
        return gameStore.sessions().stream().toList();
    }

    /**
     * @return read only list of players
     */
    public List<Player> players() {
        return gameStore.players().values().stream().toList();
    }
}
