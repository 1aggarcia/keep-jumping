package io.github.aggarcia.players.updates;

import java.util.Map;

import io.github.aggarcia.players.Player;

public record UpdateVelocity(
    String clientId,
    int xVelocity,
    int yVelocity
) implements PlayerUpdate {
    @Override
    public void applyTo(Map<String, Player> players) {
        if (!players.containsKey(this.clientId)) {
            throw new IllegalArgumentException(
                "session not in state: " + this.clientId);
        }
        var player = players.get(this.clientId);
        player.xVelocity(this.xVelocity);
        player.yVelocity(this.yVelocity);
    }
}
