package io.github.aggarcia.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.Player;

public final class GameEventHandler {
    private static final int TICKS_PER_SECOND =
        1000 / GameConstants.TICK_DELAY_MS;

    /** Response produced by advancing the game tick. */
    public record TickResponse(
        boolean isUpdateNeeded,
        List<GamePlatform> nextPlatformsState,
        int nextTickCount
    ) {}

    private GameEventHandler() {}

    /**
     * Move all players to thier position in the next tick.
     * @param players current state of all players
     * @param platforms current state of all platforms
     * @returns response with isUpdatedNeeded flag and nextTickCount
     */
    public static TickResponse advanceToNextTick(
        Collection<Player> players,
        Collection<GamePlatform> platforms,
        int tickCount
    ) {
        boolean isUpdateNeeded = true;
        int nextTickCount = (tickCount + 1) % TICKS_PER_SECOND;
        List<GamePlatform> nextPlatformsState = new ArrayList<>();
        // TODO: consider removing `isUpdateNeeded`
        // given that platforms always move, `isUpdateNeeded` is always true

        // if (nextTickCount == 0) {
        //     isUpdateNeeded = true;  // update needed to refresh the server age
        // }

        for (GamePlatform platform : platforms) {
            var nextPlatform = platform.toNextTick();
            // to "delete" platforms that fall below the ground
            if (nextPlatform.y() <= GameConstants.HEIGHT) {
                nextPlatformsState.add(nextPlatform);
            }
        }
        for (Player player : players) {
            player.moveToNextTick();
            if (player.hasChanged()) {
                // isUpdateNeeded = true;
                player.hasChanged(false);
            }
        }

        return new TickResponse(
            isUpdateNeeded,
            nextPlatformsState,
            nextTickCount
        );
    }
}
