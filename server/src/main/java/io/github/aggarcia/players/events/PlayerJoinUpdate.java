package io.github.aggarcia.players.events;

import io.github.aggarcia.shared.SocketMessage;

public record PlayerJoinUpdate(
    SocketMessage type,
    String name
) {}
