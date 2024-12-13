package io.github.aggarcia.players.messages;

import io.github.aggarcia.shared.SocketMessage;

public record PlayerJoinUpdate(
    SocketMessage type,
    String name
) {}
