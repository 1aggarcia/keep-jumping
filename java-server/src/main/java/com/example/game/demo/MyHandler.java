package com.example.game.demo;

import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class MyHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int age = 0;

    @Override
    public void handleTextMessage(
        @NonNull WebSocketSession session, @NonNull TextMessage message
    ) throws Exception {
        Map<String, Object> data = objectMapper.readValue(
            message.getPayload(),
            new TypeReference<Map<String, Object>>() {}
        );

        System.out.println("Received: " + data);
        
        // random test response
        age++;
        Map<String, Object> playerState = Map.of(
            "id", "0",
            "color", "#ff0000",
            "position", new int[] {50, 100},
            "age", "0"
        );
        Map<String, Object> response = Map.of(
            "type", "gameUpdate",
            "serverAge", age,
            "players", new Object[] { playerState }
        );
        String textResponse = objectMapper.writeValueAsString(response);

        session.sendMessage(new TextMessage(textResponse));
    }
}

