package io.github.aggarcia.game;

import java.util.Map;

import io.github.aggarcia.players.Player;

public final class GameEventHandler {
    private static final int TICKS_PER_SECOND =
        1000 / GameConstants.TICK_DELAY_MS;

    /** Response produced by advancing the game tick. */
    public record TickResponse(
        boolean isUpdateNeeded,
        int nextTickCount
    ) {}

    private GameEventHandler() {}

    /**
     * Move all players to thier position in the next tick.
     * @param players current state of all players
     * @returns response with isUpdatedNeeded flag and nextTickCount
     */
    public static TickResponse
    advanceToNextTick(Map<String, Player> players, int tickCount) {
        boolean isUpdateNeeded = false;
        int nextTickCount = (tickCount + 1) % TICKS_PER_SECOND;
        if (nextTickCount == 0) {
            isUpdateNeeded = true;  // update needed to refresh the server age
        }

        for (Player player : players.values()) {
            player.moveToNextTick();
            if (player.hasChanged()) {
                isUpdateNeeded = true;
                player.hasChanged(false);
            }
        }

        return new TickResponse(isUpdateNeeded, nextTickCount);
    }
}
