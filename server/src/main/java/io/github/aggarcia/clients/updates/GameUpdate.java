package io.github.aggarcia.clients.updates;

import java.util.Optional;

import io.github.aggarcia.models.GameStore;

public interface GameUpdate {
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
