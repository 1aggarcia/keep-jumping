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
    protected static final int PLAYER_WIDTH = 40;
    public static final int PLAYER_HEIGHT = 40;
    protected static final int GRAVITY = 2;

    public static final int
        MAX_PLAYER_X = GameConstants.WIDTH - PLAYER_WIDTH;
    public static final int
        MAX_PLAYER_Y = GameConstants.HEIGHT - PLAYER_HEIGHT;

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
     * Changes the players position to that of the next tick according to the
     * player velocity. Applies gravity to the Y axis, if the player is not
     * touching the ground.
     * @return reference to the same object
     */
    public Player moveToNextTick() {
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
        int newY = this.yPosition + this.yVelocity;
        if (newY > MAX_PLAYER_Y) {
            newY = MAX_PLAYER_Y;
            this.yVelocity = 0;
        } else if (newY < 0) {
            newY = 0;
            this.yVelocity = 0;
        }

        if (newY != this.yPosition) {
            this.yPosition = newY;
            this.hasChanged = true;
        }
        return this;
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
