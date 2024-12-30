package io.github.aggarcia.players.updates;

import java.util.Optional;

import io.github.aggarcia.game.GameStore;

public record ErrorUpdate(
    String message
) implements PlayerUpdate {
    @Override
    public Optional<String> reply() {
        return Optional.of(message);
    }

    @Override
    public void applyTo(GameStore store) {
        // do nothing
    }
}
