package io.github.aggarcia.players.updates;

import java.util.Map;

import io.github.aggarcia.players.Player;

public interface PlayerUpdate {
    /**
     * Modify the game state in a deterministic manner based on this
     * PlayerUpdate.
     * @param players
     */
    void applyTo(Map<String, Player> players);
}
