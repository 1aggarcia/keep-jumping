package io.github.aggarcia.engine;

import static io.github.aggarcia.engine.GameConstants.LEVELUP_PLATFORM_GRAVITY;
import static io.github.aggarcia.engine.GameConstants.PLATFORM_SPEEDUP_INTERVAL;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.messages.Generated.GamePing;
import io.github.aggarcia.messages.Generated.Platform;
import io.github.aggarcia.messages.Generated.Player;
import io.github.aggarcia.messages.Generated.SocketMessage;
import io.github.aggarcia.models.GamePlatform;
import io.github.aggarcia.models.GameStore;
import io.github.aggarcia.models.PlayerStore;

public final class TickProcessor {
    protected static final int SCORE_PER_SECOND = 5;
    protected static final int
        TICKS_PER_SECOND = 1000 / GameConstants.TICK_DELAY_MS;

    protected static final int MIN_PLATFORM_SPACING = 100;
    protected static final int MAX_PLATFORM_SPACING = 350;

    /** Response produced by advancing the game tick. */
    public record TickResponse(
        boolean isUpdateNeeded,
        List<GamePlatform> nextPlatformsState,
        List<String> playersToRemove
    ) {}

    private TickProcessor() {}

    /**
     * Move all players and platforms to their position in the next tick.
     * Update the tickCount and gameAgeSeconds fields in the GameStore
     * @param store to be updated
     * @returns response with isUpdatedNeeded flag and nextPlatformsState.
     */
    public static TickResponse advanceToNextTick(GameStore store) {
        // handle time
        int nextTickCount = (store.tickCount() + 1) % TICKS_PER_SECOND;
        store.tickCount(nextTickCount);
        if (nextTickCount == 0) {
            addOneSecond(store);
        }

        // handle platforms
        List<GamePlatform> nextPlatformsState = new ArrayList<>();
        for (GamePlatform platform : store.platforms()) {
            var nextPlatform = platform.toNextTick(store.platformGravity());
            // to "delete" platforms that fall below the ground
            if (nextPlatform.y() <= GameConstants.HEIGHT) {
                nextPlatformsState.add(nextPlatform);
            }
        }

        // handle players
        List<String> playersToRemove = new ArrayList<>();
        for (var playerEntry : store.players().entrySet()) {
            PlayerStore player = playerEntry.getValue();
            player.moveToNextTick(nextPlatformsState, store.platformGravity());
            if (player.hasChanged()) {
                player.hasChanged(false);
            }
            if (
                player.yPosition()
                >= GameConstants.HEIGHT - PlayerStore.PLAYER_HEIGHT
            ) {
                playersToRemove.add(playerEntry.getKey());
            } else if (nextTickCount == 0) {
                player.addToScore(SCORE_PER_SECOND);
            }
        }

        // TODO: remove boolean arg
        return new TickResponse(true, nextPlatformsState, playersToRemove);
    }

    /**
     * Add one second to the game store, update platform gravity if necessary.
     * @param store
     */
    private static void addOneSecond(GameStore store) {
        int newAge = store.gameAgeSeconds() + 1;
        store.gameAgeSeconds(newAge);
        if (newAge % PLATFORM_SPEEDUP_INTERVAL == 0) {
            System.out.println("Advancing to next level");
            store.platformGravity(
                store.platformGravity() + LEVELUP_PLATFORM_GRAVITY);
        }
    }

     /**
     * Create a GamePing message based on the current state of the GameStore.
     * @param store
     * @return GamePing message
     */
    public static SocketMessage createGamePing(GameStore store) {
        List<Player> players = store.players().values()
            .stream()
            .map(p -> Player.newBuilder()
                .setColor(p.color())
                .setName(p.name())
                .setScore(p.score())
                .setX(p.xPosition())
                .setY(p.yPosition())
                .build()
            )
            .toList();

        List<Platform> platforms = store.platforms()
            .stream()
            .map(p -> Platform.newBuilder()
                .setWidth(p.width())
                .setX(p.x())
                .setY(p.y())
                .build()
            )
            .toList();

        var ping = GamePing.newBuilder()
            .setServerAge(store.gameAgeSeconds())
            .addAllPlayers(players)
            .addAllPlatforms(platforms)
            .build();

        return SocketMessage.newBuilder().setGamePing(ping).build();
    }

    // /**
    //  * Generate random platforms at fixed vertical
    //  * intervals for the first player.
    //  * @return new platforms
    //  */
    // public static List<GamePlatform> spawnInitPlatforms() {
    //     List<GamePlatform> platforms = new ArrayList<>();

    //     int platformHeight = 0;
    //     while (platformHeight < GameConstants.HEIGHT) {
    //         platforms.add(GamePlatform.generateAtHeight(platformHeight));
    //         platformHeight += INIT_PLATFORM_SPACING;
    //     }

    //     return platforms;
    // }

    /**
     * Semi-randomly decides whether or not to add a platform to the platform
     * list. If the highest platform (smallest Y) is outside of certain bounds,
     * the result is deterministic, so that platforms are not too far or too
     * close.
     * @param platforms
     * @return boolean decision
     */
    public static boolean shouldSpawnPlatform(List<GamePlatform> platforms) {
        final int spawnProbability = (1000 / GameConstants.TICK_DELAY_MS) / 2;

        int smallestY = platforms.stream()
            .map(platform -> platform.y())
            .min(Integer::compareTo)
            .orElse(0);

        // so that platforms are not too close to each other
        if (smallestY < MIN_PLATFORM_SPACING) {
            return false;
        }
        if (smallestY > MAX_PLATFORM_SPACING) {
            return true;
        }
        return RandomUtil.getPositiveInt() % spawnProbability == 0;
    }
}
