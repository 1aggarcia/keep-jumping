package io.github.aggarcia.players;

import java.util.Random;

import io.github.aggarcia.game.GameConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
public final class Player {
    public static final int PLAYER_WIDTH = 20;
    public static final int PLAYER_HEIGHT = 20;
    private static final int HEX_STRING_LEN = 6;

    private final String color;
    private final int age;

    // we don't want setters on position
    @Getter
    private int xPosition;

    @Getter
    private int yPosition;

    /** Change in X per tick. */
    private int xVelocity;

    /** Change in Y per tick. */
    private int yVelocity;

    /**
     * true if the player state has changes since the last tick,
     * false otherwise.
     */
    private boolean hasChanged;

    /**
     * Factory function to create a new player with a random color and position.
     * @return new instance of Player, with position in game bounds and as a
     * factor of the player size
     */
    public static Player createRandomPlayer() {
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
            .nextInt(GameConstants.HEIGHT / PLAYER_HEIGHT) * PLAYER_HEIGHT;

        return new Player(
            color.toString(),
            0,
            xPosition,
            yPosition,
            0,
            0,
            true
        );
    }

    /**
     * Changes the players position to that of the next tick
     * according to the player velocity.
     */
    public void moveToNextTick() {
        int newX = this.xPosition + this.xVelocity;
        boolean isXInBounds =
            0 <= newX && newX + PLAYER_WIDTH <= GameConstants.WIDTH;

        if (isXInBounds && newX != this.xPosition) {
            this.xPosition = newX;
            this.hasChanged = true;
        }

        int newY = this.yPosition + this.yVelocity;
        boolean isYInBounds =
            0 <= newY && newY + PLAYER_HEIGHT <= GameConstants.HEIGHT;

        if (isYInBounds && newY != this.yPosition) {
            this.yPosition = newY;
            this.hasChanged = true;
        }
    }

    /**
     * Convert a Player record to a PlayerState record.
     * @return instance of PlayerState
     */
    public PlayerState toPlayerState() {
        return new PlayerState(
            this.color(),
            this.xPosition(),
            this.yPosition(),
            this.age()
        );
    }

    public Player clone() {
        return new Player(
            this.color(),
            this.age(),
            this.xPosition(),
            this.yPosition(),
            this.xVelocity(),
            this.yVelocity(),
            this.hasChanged()
        );
    }
}
