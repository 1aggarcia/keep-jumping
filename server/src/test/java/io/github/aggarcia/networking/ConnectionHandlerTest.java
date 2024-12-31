package io.github.aggarcia.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.players.events.JoinEvent;

// naming convention: test_<unit>_<state>_<expected behavior>
@SpringBootTest
public class ConnectionHandlerTest {
    private GameStore gameStore;
    private ConnectionHandler connectionHandler;

    @Mock
    private WebSocketSession mockSession;

    @BeforeEach
    void resetState() {
        gameStore = new GameStore();
        connectionHandler = new ConnectionHandler(gameStore);
    }

    @Test
    void test_afterConnectionEstablished_firstSession_isSaved() {
        assertEquals(0, gameStore.sessions().size());
        
        connectionHandler.afterConnectionEstablished(mockSession);
        assertEquals(1, gameStore.sessions().size(), 1);
        assertEquals(
            mockSession,
            gameStore.sessions().stream().findAny().get()
        );
    }

    @Test
    void test_afterConnectionEstablished_firstSession_doesNotCreatePlayer() {
        assertEquals(gameStore.players().size(), 0);
        connectionHandler.afterConnectionEstablished(mockSession);
        assertEquals(gameStore.players().size(), 0);
    }

    @Test
    void test_afterConnectionClosed_oneSession_removesFromMemory() {
        connectionHandler.afterConnectionEstablished(mockSession);
        assertEquals(gameStore.sessions().size(), 1);

        connectionHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(gameStore.sessions().size(), 0);
    }

    @Test
    void test_afterConnectionClosed_noSessions_doesNotThrow() {
        assertEquals(gameStore.sessions().size(), 0); 
        connectionHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(gameStore.sessions().size(), 0); 
    }

    @Test
    void test_handleTextMessage_joinEvent_updatesGameStore() throws Exception {
        assertNull(gameStore.players().get(mockSession.getId()));

        var event = new JoinEvent("player1");
        var serialized = new ObjectMapper().writeValueAsString(event);
        var message = new TextMessage(serialized);

        // add the session first, or else join isn't allowed
        connectionHandler.afterConnectionEstablished(mockSession);
        connectionHandler.handleTextMessage(mockSession, message);
        assertNotNull(gameStore.players().get(mockSession.getId()));
    }
}
