package io.github.aggarcia.players.updates;

import java.util.Optional;

import io.github.aggarcia.game.GameStore;

public interface PlayerUpdate {
    /**
     * Optional reply to send back to the client.
     * If empty, no reply should be sent
     */
    Optional<String> reply();

    /**
     * Modify the game state in a deterministic manner based on this
     * PlayerUpdate.
     * @param store
     */
    void applyTo(GameStore store);
}
