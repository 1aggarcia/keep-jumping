package io.github.aggarcia.models;

import java.util.Random;

import io.github.aggarcia.engine.GameConstants;

/**
 * Read only model of a platform.
 */
public record GamePlatform(
    int width,
    int x,
    int y
) {
    // public for Player access
    public static final int PLATFORM_GRAVITY = 5;
    protected static final int MIN_WIDTH = 300;
    protected static final int MAX_WIDTH = 600;

    /**
     * @param height Y position for the new platform
     * @return a new platform with a random width and X position,
     *  within a reasonable size
     */
    public static GamePlatform generateAtHeight(int height) {
        if (height < 0 || GameConstants.HEIGHT < height) {
            throw new IllegalArgumentException(
                "Platform height out of bounds: " + height);
        }
        var random = new Random();
        int width = random.nextInt(MAX_WIDTH - MIN_WIDTH) + MIN_WIDTH;
        int xPosition = random.nextInt(GameConstants.WIDTH - width);
        return new GamePlatform(width, xPosition, height);
    }

    /**
     * @return a new platform moved downwards a constant amount
     */
    public GamePlatform toNextTick() {
        return new GamePlatform(this.width, this.x, this.y + PLATFORM_GRAVITY);
    }
}
