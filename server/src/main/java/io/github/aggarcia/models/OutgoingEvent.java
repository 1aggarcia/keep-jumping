package io.github.aggarcia.models;

import org.springframework.web.socket.WebSocketSession;

import io.github.aggarcia.messages.Generated.SocketMessage;

/**
 * An event to be sent to a client
 */
public record OutgoingEvent(
    WebSocketSession client,
    SocketMessage event
) {}
