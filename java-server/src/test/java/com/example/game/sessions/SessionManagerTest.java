package com.example.game.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

// naming convention: test_<unit>_<state>_<expected behavior>
@SpringBootTest
public class SessionManagerTest {
    @Autowired
    private SessionManager sessionManager;

    @Mock
    private WebSocketSession mockSession;

    @Test
    void test_afterConnectionEstablished_firstSession_isSaved() {
        assertEquals(sessionManager.sessions().size(), 0);
        
        sessionManager.afterConnectionEstablished(mockSession);
        assertEquals(sessionManager.sessions().size(), 1);
        assertEquals(sessionManager.sessions().get(0), mockSession);
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
}
