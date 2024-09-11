package com.example.game.players;

import java.util.Random;

import com.example.game.game.GameConstants;

public record Player (
    String color,
    int age,

    int xPosition,
    int yPosition,

    /** Change in X per tick */
    int xVelocity,

    /** Change in Y per tick */
    int yVelocity
) {
    private static final int HEX_STRING_LEN = 6;

    /**
     * Factory function to create a new player with a random color and position
     * @return new instance of Player
     */
    public static Player createNewPlayer() {
        Random random = new Random();

        StringBuilder color = new StringBuilder("#");
        for (int i = 0; i < HEX_STRING_LEN; i++) {
            String hex = Integer.toHexString(random.nextInt(16));
            color.append(hex);
        }

        int xPosition = random.nextInt(GameConstants.WIDTH);
        int yPosition = random.nextInt(GameConstants.HEIGHT);

        System.out.println(color.toString());
        return new Player(color.toString(), 0, xPosition, yPosition, 0, 0);
    }
}
