package io.github.aggarcia.game;

import java.util.Collection;
import java.util.List;

import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.Player;
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
     * @param players list of players in the game
     * @param serverAge age in seconds since the game started
     * @return new instance of GamePing
     */
    public static
    GamePing fromGameState(
        Collection<Player> players,
        Collection<GamePlatform> platforms,
        int serverAge
    ) {
        List<PlayerState> playersState = players
            .stream()
            .map(player -> player.toPlayerState())
            .toList();

        return new GamePing(
            serverAge,
            playersState,
            platforms.stream().toList()
        );
    }
}
