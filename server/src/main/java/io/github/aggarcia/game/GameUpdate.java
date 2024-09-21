package io.github.aggarcia.game;

import java.util.List;
import java.util.Map;

import io.github.aggarcia.players.Player;
import io.github.aggarcia.players.PlayerState;
import io.github.aggarcia.shared.SocketMessage;

public record GameUpdate(
    /** Should always be GAME_UPDATE */
    SocketMessage type,
    int serverAge,
    List<PlayerState> players
) {
    /**
     * Factory function to create a game update message based on
     * the current state of the game.
     * @param players list of players in the game
     * @param serverAge age in seconds since the game started
     * @return new instance of GameUpdate
     */
    public static
    GameUpdate fromGameState(Map<String, Player> players, int serverAge) {
        List<PlayerState> playersState = players.values()
            .stream()
            .map(player -> player.toPlayerState())
            .toList();

        return new GameUpdate(
            SocketMessage.GAME_UPDATE,
            serverAge,
            playersState
        );
    }
}
