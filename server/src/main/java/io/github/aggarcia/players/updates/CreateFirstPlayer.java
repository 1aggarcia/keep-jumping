package io.github.aggarcia.players.updates;

import java.util.List;
import java.util.Optional;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.PlayerStore;

public record CreateFirstPlayer(
    String client,
    PlayerStore player,
    List<GamePlatform> platforms
) implements GameUpdate {
    @Override
    public void applyTo(GameStore store) {
        store.players().put(client, player);
        store.platforms(platforms);
        store.tiggerStartEvent();
    }

    @Override
    public Optional<byte[]> reply() {
        return Optional.empty();
    }
}
