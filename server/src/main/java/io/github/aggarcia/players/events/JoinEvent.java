package io.github.aggarcia.players.events;

import io.github.aggarcia.shared.SocketMessage;

public record JoinEvent(
    String name
) implements SocketMessage {}
