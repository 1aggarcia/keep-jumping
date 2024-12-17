package io.github.aggarcia.players.events;

import java.util.List;

import io.github.aggarcia.players.PlayerControl;
import io.github.aggarcia.shared.SocketMessage;

public record PlayerControlUpdate(
    SocketMessage type,
    List<PlayerControl> pressedControls
) {}

