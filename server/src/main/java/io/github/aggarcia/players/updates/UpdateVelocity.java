package io.github.aggarcia.players.updates;

import java.util.Map;
import java.util.Optional;
import io.github.aggarcia.players.Player;

public record UpdateVelocity(
    String clientId,
    int xVelocity,
    int yVelocity
) implements PlayerUpdate {
    @Override
    public Optional<String> reply() {
        return Optional.empty();
    }

    @Override
    public void applyTo(Map<String, Player> players) {
        if (!players.containsKey(this.clientId)) {
            throw new IllegalArgumentException(
                "no player for id: " + this.clientId);
        }
        var player = players.get(this.clientId);
        synchronized (player) {
            player.xVelocity(this.xVelocity);
            player.yVelocity(this.yVelocity);
        }
    }
}
