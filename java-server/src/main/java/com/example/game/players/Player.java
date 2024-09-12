package com.example.game.players;

import java.util.Random;

import com.example.game.game.GameConstants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
public final class Player {
    private static final int HEX_STRING_LEN = 6;
    private static final int PLAYER_WIDTH = 20;
    private static final int PLAYER_HEIGHT = 20;

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
     * Factory function to create a new player with a random color and position.
     * @return new instance of Player
     */
    public static Player createRandomPlayer() {
        Random random = new Random();

        StringBuilder color = new StringBuilder("#");
        for (int i = 0; i < HEX_STRING_LEN; i++) {
            String hex = Integer.toHexString(random.nextInt(16));
            color.append(hex);
        }

        int xPosition = random.nextInt(GameConstants.WIDTH);
        int yPosition = random.nextInt(GameConstants.HEIGHT);

        return new Player(color.toString(), 0, xPosition, yPosition, 0, 0);
    }

    /**
     * Changes the players position to that of the next tick
     * according to the player velocity.
     */
    public void moveToNextTick() {
        int newX = this.xPosition + this.xVelocity;
        int newY = this.yPosition + this.yVelocity;

        if (0 <= newX && newX + PLAYER_WIDTH <= GameConstants.WIDTH) {
            this.xPosition = newX;
        }
        if (0 <= newY && newY + PLAYER_HEIGHT <= GameConstants.HEIGHT) {
            this.yPosition = newY;
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
            this.color,
            this.age(),
            this.xPosition(),
            this.yPosition(),
            this.xVelocity(),
            this.yVelocity()
        );
    }
}
