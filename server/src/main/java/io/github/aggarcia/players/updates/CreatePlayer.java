package io.github.aggarcia.players.updates;

import java.util.Optional;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.players.Player;

public record CreatePlayer(
    boolean isError,
    String client,
    Player player
) implements PlayerUpdate {
    @Override
    public Optional<String> reply() {
        return Optional.empty();
    }

    /**
     * Add the mapping `client -> player`.
     */
    @Override
    public void applyTo(GameStore store) {
        if (!isError) {
            store.players().put(client, player);
        }
    }
}
