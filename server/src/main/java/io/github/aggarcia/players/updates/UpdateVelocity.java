package io.github.aggarcia.players.updates;

import java.util.Optional;

import io.github.aggarcia.game.GameStore;

public record UpdateVelocity(
    String clientId,
    int xVelocity,
    int yVelocity
) implements PlayerUpdate {
    @Override
    public Optional<byte[]> reply() {
        return Optional.empty();
    }

    @Override
    public void applyTo(GameStore store) {
        if (!store.players().containsKey(this.clientId)) {
            throw new IllegalArgumentException(
                "no player for id: " + this.clientId);
        }
        var player = store.players().get(this.clientId);
        synchronized (player) {
            player.xVelocity(this.xVelocity);
            player.yVelocity(this.yVelocity);
        }
    }
}
