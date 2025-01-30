package io.github.aggarcia.clients.updates;

import java.util.Optional;

import io.github.aggarcia.models.GameStore;

public record UpdateVelocity(
    String clientId,
    int xVelocity,
    int yVelocity
) implements GameUpdate {
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
