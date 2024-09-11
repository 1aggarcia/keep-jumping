package com.example.game.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

// naming convention: test_<unit>_<state>_<expected behavior>
@SpringBootTest
public class SessionManagerTest {
    private SessionManager sessionManager;

    @Mock
    private WebSocketSession mockSession;

    @BeforeEach
    void resetState() {
        sessionManager = new SessionManager();
    }

    @Test
    void test_afterConnectionEstablished_firstSession_isSaved() {
        assertEquals(sessionManager.sessions().size(), 0);
        
        sessionManager.afterConnectionEstablished(mockSession);
        assertEquals(sessionManager.sessions().size(), 1);
        assertEquals(sessionManager.sessions().get(0), mockSession);
    }

    @Test
    void test_afterConnectionEstablished_firstSession_createsPlayer() {
        assertEquals(sessionManager.players().size(), 0);
        sessionManager.afterConnectionEstablished(mockSession);
        assertEquals(sessionManager.players().size(), 1);
    }

    @Test
    void test_afterConnectionClosed_oneSession_removesFromMemory() {
        sessionManager.afterConnectionEstablished(mockSession);
        assertEquals(sessionManager.sessions().size(), 1);

        sessionManager.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(sessionManager.sessions().size(), 0);
    }

    @Test
    void test_afterConnectionClosed_noSessions_doesNotThrow() {
        assertEquals(sessionManager.sessions().size(), 0); 
        sessionManager.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(sessionManager.sessions().size(), 0); 
    }

    @Test
    void test_afterConnectionClosed_oneSession_deletesPlayer() {
        // creates the player
        sessionManager.afterConnectionEstablished(mockSession);
        assertEquals(sessionManager.players().size(), 1);
        sessionManager.afterConnectionClosed(mockSession, CloseStatus.NORMAL);
        assertEquals(sessionManager.players().size(), 0);
    }
}
