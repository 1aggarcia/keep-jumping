package com.example.game.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;

import com.example.game.sessions.MessageTypes.GameUpdate;
import com.example.game.sessions.MessageTypes.ErrorResponse;
import com.example.game.sessions.MessageTypes.SocketMessage;
import com.example.game.sessions.MessageTypes.SocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class MessageHandlerTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    private MessageHandler messageHandler;

    @BeforeEach
    void resetState() {
        messageHandler = new MessageHandler();
    }

    @Test
    void test_getResponse_playerUpdate_returnsGameState() throws Exception {
        TextMessage message = jsonToMessage(Map.of(
            "type", "playerControlUpdate",
            "pressedControls", Collections.emptyList()
        ));

        SocketMessage response = messageHandler.getResponse(message, new HashMap<>());
        assertTrue(response instanceof GameUpdate);
        GameUpdate update = (GameUpdate) response;
    
        assertEquals(SocketMessageType.GAME_UPDATE, update.type());
        assertEquals(0, update.players().size());
        assertEquals(0, update.serverAge());
    }

    @Test
    void test_getResponse_plainString_throwsError() throws Exception {
        TextMessage message = new TextMessage("plain string");
        assertThrows(Exception.class, () -> {
            messageHandler.getResponse(message, null);
        });
    }

    @Test
    void test_getResponse_unknownType_returnsError() throws Exception {
        TextMessage message = jsonToMessage(Map.of("type", "badType")); 
        SocketMessage response = messageHandler.getResponse(message, null);
        assertTrue(response instanceof ErrorResponse);
        assertEquals(SocketMessageType.SERVER_ERROR, response.type());
    }

    @Test
    void test_getResponse_nullType_returnsError() throws Exception {
        TextMessage message = jsonToMessage(Map.of("key", "value"));
        SocketMessage response = messageHandler.getResponse(message, null);
        assertTrue(response instanceof ErrorResponse);
        assertEquals(SocketMessageType.SERVER_ERROR, response.type());
    }

    private TextMessage jsonToMessage(Map<String, Object> json) {
        try {
            return new TextMessage(mapper.writeValueAsString(json));
        } catch (JsonProcessingException e) {
            fail(e);
            return null;  // to make the compiler happy
        }
    }
}
