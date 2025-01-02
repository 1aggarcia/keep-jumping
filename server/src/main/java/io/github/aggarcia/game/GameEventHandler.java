package io.github.aggarcia.game;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.generated.SocketMessageOuterClass.GamePing;
import io.github.aggarcia.generated.SocketMessageOuterClass.Platform;
import io.github.aggarcia.generated.SocketMessageOuterClass.Player;
import io.github.aggarcia.generated.SocketMessageOuterClass.SocketMessage;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.PlayerStore;

public final class GameEventHandler {
    protected static final int SCORE_PER_SECOND = 5;
    protected static final int
        TICKS_PER_SECOND = 1000 / GameConstants.TICK_DELAY_MS;

    protected static final int MIN_PLATFORM_SPACING = 100;
    protected static final int MAX_PLATFORM_SPACING = 350;
    protected static final int INIT_PLATFORM_SPACING = 175;

    /** Response produced by advancing the game tick. */
    public record TickResponse(
        boolean isUpdateNeeded,
        List<GamePlatform> nextPlatformsState,
        int nextTickCount
    ) {}

    private GameEventHandler() {}

    /**
     * Move all players to thier position in the next tick.
     * @param store
     * @returns response with isUpdatedNeeded flag and nextTickCount
     */
    public static TickResponse advanceToNextTick(GameStore store) {
        boolean isUpdateNeeded = true;
        int nextTickCount = (store.tickCount() + 1) % TICKS_PER_SECOND;
        List<GamePlatform> nextPlatformsState = new ArrayList<>();
        // TODO: consider removing `isUpdateNeeded`
        // given that platforms always move, `isUpdateNeeded` is always true

        //if (nextTickCount == 0) {
        //    isUpdateNeeded = true;  // update needed to refresh the server age
        //}
        for (GamePlatform platform : store.platforms()) {
            var nextPlatform = platform.toNextTick();
            // to "delete" platforms that fall below the ground
            if (nextPlatform.y() <= GameConstants.HEIGHT) {
                nextPlatformsState.add(nextPlatform);
            }
        }
        // make a copy since we are modifying the original
        var playersCopy = new ArrayList<PlayerStore>(store.players().values());
        for (PlayerStore player : playersCopy) {
            player.moveToNextTick(nextPlatformsState);
            if (player.hasChanged()) {
                // isUpdateNeeded = true;
                player.hasChanged(false);
            }
            if (
                player.yPosition()
                >= GameConstants.HEIGHT - PlayerStore.PLAYER_HEIGHT
            ) {
                // game over for player
                // TODO: close associated client session
                store.players().values().remove(player);
                // TODO send event to player
            } else if (nextTickCount == 0) {
                player.addToScore(SCORE_PER_SECOND);
            }
        }

        return new TickResponse(
            isUpdateNeeded,
            nextPlatformsState,
            nextTickCount
        );
    }

     /**
     * Create a GamePing message based on the current state of the GameStore.
     * @param store
     * @param gameAge game loop age in seconds
     * @return GamePing message
     */
    public static SocketMessage createGamePing(GameStore store, int gameAge) {
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
            .setServerAge(gameAge)
            .addAllPlayers(players)
            .addAllPlatforms(platforms)
            .build();

        return SocketMessage.newBuilder().setGamePing(ping).build();
    }

    /**
     * Generate random platforms at fixed vertical
     * intervals for the first player.
     * @return new platforms
     */
    public static List<GamePlatform> spawnInitPlatforms() {
        List<GamePlatform> platforms = new ArrayList<>();

        int platformHeight = 0;
        while (platformHeight < GameConstants.HEIGHT) {
            platforms.add(GamePlatform.generateAtHeight(platformHeight));
            platformHeight += INIT_PLATFORM_SPACING;
        }

        return platforms;
    }

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
