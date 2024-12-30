package io.github.aggarcia.game;

import java.util.List;

import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.PlayerState;
import io.github.aggarcia.shared.SocketMessage;

public record GamePing(
    int serverAge,
    List<PlayerState> players,
    List<GamePlatform> platforms
) implements SocketMessage {
    /**
     * Factory function to create a game ping message based on
     * the current state of the game.
     * @param store
     * @return new instance of GamePing
     */
    public static
    GamePing fromGameState(GameStore store, int gameAge) {
        List<PlayerState> playersState = store.players().values()
            .stream()
            .map(player -> player.toPlayerState())
            .toList();

        return new GamePing(
            gameAge,
            playersState,
            store.platforms().stream().toList()
        );
    }
}
