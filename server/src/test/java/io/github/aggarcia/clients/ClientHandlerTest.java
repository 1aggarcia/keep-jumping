package io.github.aggarcia.clients;

import static io.github.aggarcia.messages.Serializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import io.github.aggarcia.messages.Generated.JoinEvent;
import io.github.aggarcia.messages.Generated.SocketMessage;
import io.github.aggarcia.models.GameStore;

// naming convention: test_<unit>_<state>_<expected behavior>
@SpringBootTest
public class ClientHandlerTest {
    private GameStore gameStore;
    private ClientHandler connectionHandler;

    @Mock
    private WebSocketSession mockSession;

    @BeforeEach
    void resetState() {
        gameStore = new GameStore();
        connectionHandler = new ClientHandler(gameStore);
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
    void test_handleBinaryMessage_joinEvent_updatesGameStore() {
        assertNull(gameStore.players().get(mockSession.getId()));

        var event = JoinEvent.newBuilder().setName("testEvent").build();
        var wrappedEvent = SocketMessage
            .newBuilder().setJoinEvent(event).build();

        var message = new BinaryMessage(serialize(wrappedEvent));

        // add the session first, or else join isn't allowed
        connectionHandler.afterConnectionEstablished(mockSession);
        connectionHandler.handleBinaryMessage(mockSession, message);
        assertNotNull(gameStore.players().get(mockSession.getId()));
    }
}
