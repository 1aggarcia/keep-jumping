package io.github.aggarcia.models;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import io.github.aggarcia.engine.GameConstants;
import io.github.aggarcia.leaderboard.LeaderboardEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor  // for the builder
@Accessors(fluent = true)
public final class PlayerStore {
    public static final int PLAYER_WIDTH = 40;
    public static final int PLAYER_HEIGHT = 40;
    public static final int GRAVITY = 2;

    // how much extra height to give above a platform
    public static final int SPAWN_HEIGHT = PLAYER_HEIGHT + 100;

    public static final int
        MAX_PLAYER_X = GameConstants.WIDTH - PLAYER_WIDTH;
    public static final int
        MAX_PLAYER_Y = GameConstants.HEIGHT - PLAYER_HEIGHT;

    // players are allowed to jump slighly above the visible space
    public static final int MIN_PLAYER_Y = -500;

    // so that players don't spawn close to the bottom
    protected static final int MAX_SPAWN_HEIGHT = MAX_PLAYER_Y / 3;

    private static final int HEX_STRING_LEN = 6;

    private final String color;
    private final String name;

    // we don't want setters on position
    @Getter
    private int xPosition;

    @Getter
    private int yPosition;

    /** Change in X per tick. */
    private int xVelocity;

    /** Change in Y per tick. */
    private int yVelocity;

    private int score;

    /**
     * true if the player state has changes since the last tick,
     * false otherwise.
     */
    private boolean hasChanged;

    // TODO: only used by tests, try to remove
    /**
     * Factory function to create a new player with a random color and position.
     * @param name unique name
     * @return new instance of Player, with position in game bounds and as a
     * factor of the player size
     */
    public static PlayerStore createRandomPlayer(String name) {
        Random random = new Random();

        StringBuilder color = new StringBuilder("#");
        for (int i = 0; i < HEX_STRING_LEN; i++) {
            String hex = Integer.toHexString(random.nextInt(16));
            color.append(hex);
        }

        // this division & multiplication generates
        // random ints as a factor of player size
        int xPosition = random
            .nextInt(GameConstants.WIDTH / PLAYER_WIDTH) * PLAYER_WIDTH;
        int yPosition = random
            .nextInt(MAX_SPAWN_HEIGHT / PLAYER_HEIGHT) * PLAYER_HEIGHT;

        return PlayerStore.builder()
            .color(color.toString())
            .name(name)
            .xPosition(xPosition)
            .yPosition(yPosition)
            .xVelocity(0)
            .yVelocity(0)
            .score(0)
            .hasChanged(true)
            .build();
    }

    /**
     * Factory function to create a new player with a random color above a
     * platform.
     * @param name unique name
     * @param platforms platforms to consider for spawning the player
     * @return new PlayerStore instance
     */
    public static PlayerStore
    createAbovePlatform(String name, GamePlatform platform) {
        Random random = new Random();

        StringBuilder color = new StringBuilder("#");
        for (int i = 0; i < HEX_STRING_LEN; i++) {
            String hex = Integer.toHexString(random.nextInt(16));
            color.append(hex);
        }

        // center the player vertically on the platform
        int xPosition = platform.x() + ((platform.width() - PLAYER_WIDTH) / 2);
        int yPosition = platform.y() - SPAWN_HEIGHT;

        return PlayerStore.builder()
            .color(color.toString())
            .name(name)
            .xPosition(xPosition)
            .yPosition(yPosition)
            .xVelocity(0)
            .yVelocity(0)
            .score(0)
            .hasChanged(true)
            .build();
    }

    /**
     * @see PlayerStore#moveToNextTick(Collection)
     */
    public PlayerStore moveToNextTick() {
        return this.moveToNextTick(Collections.emptyList(), 0);
    }

    /**
     * Changes the players position to that of the next tick according to the
     * player velocity. Applies gravity to the Y axis, if the player is not
     * touching the ground.
     *
     * @param platforms collidable blocks that the player should not touch
     * @param platformGravity gravity to apply to the platforms
     * @return reference to the same object
     */
    public synchronized PlayerStore moveToNextTick(
        Collection<GamePlatform> platforms,
        int platformGravity
    ) {
        // bounds checking with sides
        int newX = this.xPosition + this.xVelocity;
        if (newX > MAX_PLAYER_X) {
            newX = MAX_PLAYER_X;
            this.xVelocity = 0;
        } else if (newX < 0) {
            newX = 0;
            this.xVelocity = 0;
        }

        if (newX != this.xPosition) {
            this.xPosition = newX;
            this.hasChanged = true;
        }

        this.yVelocity += GRAVITY;
        int oldY = this.yPosition;
        int newY = this.yPosition + this.yVelocity;
        if (newY > MAX_PLAYER_Y) {
            newY = MAX_PLAYER_Y;
            this.yVelocity = 0;
        } else if (newY < MIN_PLAYER_Y) {
            newY = MIN_PLAYER_Y;
            this.yVelocity = 0;
        }

        if (newY != this.yPosition) {
            this.yPosition = newY;
            this.hasChanged = true;
        }

        // collision correction with platforms
        for (var platform : platforms) {
            boolean hasCollided =
                isTouchingPlatform(platform)
                || crossesPlatform(oldY, platform);

            if (this.yVelocity > 0 && hasCollided) {
                this.yPosition = platform.y() - PLAYER_HEIGHT;
                this.yVelocity = platformGravity;
            }
        }
        return this;
    }

    /**
     * @param lastY - Y position in the previous tick
     * @param platform - platform to check collision against
     * @return true if the player has passed through the platform between the
     * previous tick and this one, false otherwise.
     */
    private boolean crossesPlatform(int lastY, GamePlatform platform) {
        if (this.xPosition + PLAYER_WIDTH < platform.x()) {
            return false;
        }
        if (platform.x() + platform.width() < this.xPosition) {
            return false;
        }
        int oldSign = Integer.signum(platform.y() - lastY);
        int newSign = Integer.signum(platform.y() - this.yPosition);
        return oldSign != newSign;
    }

    /**
     * Determine if the player is touching a platform.
     * @param platform
     * @return true if the player rectangle makes contact with the platform,
     *  false otherwise
     */
    private synchronized boolean isTouchingPlatform(GamePlatform platform) {
        int minY = this.yPosition;
        int maxY = this.yPosition + PLAYER_HEIGHT;

        // are both sides of the player on the same side of the platform?
        if (minY >= platform.y() || platform.y() >= maxY) {
            return false;
        }
        int minX = this.xPosition;
        int maxX = this.xPosition + PLAYER_WIDTH;
        if (minX < platform.x() && platform.x() < maxX) {
            return true;
        }
        int platformMax = platform.x() + platform.width();
        if (minX < platformMax && platformMax < maxX) {
            return true;
        }

        boolean isPlayerBehind = maxX < platform.x();
        boolean isPlayerAhead = platformMax < minX;

        // player must either be behind or ahead
        return !isPlayerBehind && !isPlayerAhead;
    }

    /**
     * @param points number of points to add
     * @return reference to the same object
     */
    public synchronized PlayerStore addToScore(int points) {
        this.score += points;
        return this;
    }

    public synchronized PlayerStore clone() {
        return PlayerStore.builder()
            .color(this.color())
            .name(this.name())
            .xPosition(this.xPosition())
            .yPosition(this.yPosition())
            .yVelocity(this.yVelocity())
            .score(this.score)
            .hasChanged(this.hasChanged())
            .build();
    }

    public synchronized LeaderboardEntry createLeaderboardEntry() {
        var now = new Timestamp(System.currentTimeMillis());
        return new LeaderboardEntry(this.name(), this.score(), now);
    }
}
