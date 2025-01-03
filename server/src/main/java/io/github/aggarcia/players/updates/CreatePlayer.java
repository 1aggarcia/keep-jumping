package io.github.aggarcia.players.updates;

import java.util.Optional;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.players.PlayerStore;

public record CreatePlayer(
    String client,
    PlayerStore player
) implements GameUpdate {
    @Override
    public Optional<byte[]> reply() {
        return Optional.empty();
    }

    /**
     * Add the mapping `client -> player`.
     */
    @Override
    public void applyTo(GameStore store) {
        store.players().put(client, player);
    }
}
