package io.github.aggarcia.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import io.github.aggarcia.game.GameStore;

// naming convention: test_<unit>_<state>_<expected behavior>
@SpringBootTest
public class ConnectionHandlerTest {
    private ConnectionHandler connectionHandler;

    @Mock
    private WebSocketSession mockSession;

    @BeforeEach
    void resetState() {
        connectionHandler = new ConnectionHandler(new GameStore());
    }

    @Test
    void test_afterConnectionEstablished_firstSession_isSaved() {
        assertEquals(connectionHandler.sessions().size(), 0);
        
        connectionHandler.afterConnectionEstablished(mockSession);
        assertEquals(connectionHandler.sessions().size(), 1);
        assertEquals(connectionHandler.sessions().get(0), mockSession);
    }

    @Test
    void test_afterConnectionEstablished_firstSession_doesNotCreatePlayer() {
        assertEquals(connectionHandler.players().size(), 0);
        connectionHandler.afterConnectionEstablished(mockSession);
        assertEquals(connectionHandler.players().size(), 0);
    }

    @Test
    void test_afterConnectionClosed_oneSession_removesFromMemory() {
        connectionHandler.afterConnectionEstablished(mockSession);
        assertEquals(connectionHandler.sessions().size(), 1);

        connectionHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(connectionHandler.sessions().size(), 0);
    }

    @Test
    void test_afterConnectionClosed_noSessions_doesNotThrow() {
        assertEquals(connectionHandler.sessions().size(), 0); 
        connectionHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(connectionHandler.sessions().size(), 0); 
    }

    // TODO: test player creation & deletion with handleTextMessage
    // @Test
    // void test_afterConnectionClosed_oneSession_deletesPlayer() {
    //     // creates the player
    //     connectionHandler.afterConnectionEstablished(mockSession);
    //     assertEquals(connectionHandler.players().size(), 1);
    //     connectionHandler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
    //     assertEquals(connectionHandler.players().size(), 0);
    // }
}
