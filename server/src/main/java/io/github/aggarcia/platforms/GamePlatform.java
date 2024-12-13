package io.github.aggarcia.platforms;

import java.util.Random;

import io.github.aggarcia.game.GameConstants;

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
     * @return a new platform with a random width and X position,
     *  within a reasonbale size
     */
    public static GamePlatform createRandomPlatform() {
        var random = new Random();
        int width = random.nextInt(MAX_WIDTH - MIN_WIDTH) + MIN_WIDTH;
        int xPosition = random.nextInt(GameConstants.WIDTH - width);
        return new GamePlatform(width, xPosition, 0);
    }

    /**
     * @return a new platform moved downwards a constant amount
     */
    public GamePlatform toNextTick() {
        return new GamePlatform(this.width, this.x, this.y + PLATFORM_GRAVITY);
    }
}
