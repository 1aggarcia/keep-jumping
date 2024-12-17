package io.github.aggarcia.players.updates;

import java.util.Map;
import java.util.Optional;

import io.github.aggarcia.players.Player;

public record ErrorUpdate(
    String message
) implements PlayerUpdate {
    @Override
    public Optional<String> reply() {
        return Optional.of(message);
    }

    @Override
    public void applyTo(Map<String, Player> players) {
        // do nothing
    }
}
