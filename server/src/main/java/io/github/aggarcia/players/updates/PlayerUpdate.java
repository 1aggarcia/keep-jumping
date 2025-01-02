package io.github.aggarcia.players.updates;

import java.util.Optional;

import io.github.aggarcia.game.GameStore;

public interface PlayerUpdate {
    /**
     * Optional reply to send back to the client. It should already
     * be encoded into binary
     * If empty, no reply should be sent
     */
    Optional<byte[]> reply();

    /**
     * Modify the game state in a deterministic manner based on this
     * PlayerUpdate.
     * @param store
     */
    void applyTo(GameStore store);
}
