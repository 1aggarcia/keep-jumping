package io.github.aggarcia.players;

import java.util.List;

import io.github.aggarcia.shared.SocketMessage;

public record PlayerControlUpdate(
    SocketMessage type,
    List<PlayerControl> pressedControls
) {}
