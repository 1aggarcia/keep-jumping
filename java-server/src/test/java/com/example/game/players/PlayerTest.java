package com.example.game.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.game.game.GameConstants;

@SpringBootTest
public class PlayerTest {
    @Test
    void test_createNewPlayer_initializesToZeroWhereExpected() {
        Player player = Player.createNewPlayer();

        assertEquals(player.age(), 0);
        assertEquals(player.xVelocity(), 0);
        assertEquals(player.yVelocity(), 0);
        assertNotNull(player.color());
    }

    @Test
    void test_createNewPlayer_generatesPositionInGameBounds() {
        // run test multiple times since it involves randomness
        for (int i = 0; i < 10; i++) {
            Player player = Player.createNewPlayer();

            assertTrue(player.xPosition() >= 0);
            assertTrue(player.yPosition() >= 0);
            assertTrue(player.xPosition() <= GameConstants.WIDTH);
            assertTrue(player.yPosition() <= GameConstants.HEIGHT);
        }
    }

    @Test
    void test_createNewPlayer_generatesLegalHexColor() {
        String color = Player.createNewPlayer().color();
        assertEquals(color.length(), 7);
        assertEquals(color.charAt(0), '#');

        for (int i = 1; i < color.length(); i++) {
            char character = Character.toUpperCase(color.charAt(i));
            assertTrue(character >= '0');
            assertTrue(character <= 'F');
        }
    }
}
