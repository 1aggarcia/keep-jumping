package io.github.aggarcia.players.replies;

import io.github.aggarcia.shared.SocketMessage;

public record ErrorReply(String message) implements SocketMessage {}
