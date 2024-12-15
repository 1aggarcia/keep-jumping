package io.github.aggarcia.players.updates;

import java.util.Map;

import io.github.aggarcia.players.Player;

public record CreatePlayer(
    boolean isError,
    String client,
    Player player
) implements PlayerUpdate {
    /**
     * Add the mapping `client -> player`.
     */
    @Override
    public void applyTo(Map<String, Player> players) {
        if (!isError) {
            players.put(client, player);
        }
    }
}
