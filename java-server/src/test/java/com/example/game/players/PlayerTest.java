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
    void test_createRandomPlayer_initializesCorrectConstantValues() {
        var player = Player.createRandomPlayer();

        assertEquals(player.age(), 0);
        assertEquals(player.xVelocity(), 0);
        assertEquals(player.yVelocity(), 0);
        assertNotNull(player.color());
    }

    @Test
    void test_createRandomPlayer_generatesPositionInGameBounds() {
        // run test multiple times since it involves randomness
        for (int i = 0; i < 10; i++) {
            var player = Player.createRandomPlayer();

            assertTrue(player.xPosition() >= 0);
            assertTrue(player.yPosition() >= 0);
            assertTrue(player.xPosition() <= GameConstants.WIDTH);
            assertTrue(player.yPosition() <= GameConstants.HEIGHT);
        }
    }

    @Test
    void test_createRandomPlayer_generatesLegalHexColor() {
        String color = Player.createRandomPlayer().color();
        assertEquals(color.length(), 7);
        assertEquals(color.charAt(0), '#');

        for (int i = 1; i < color.length(); i++) {
            char character = Character.toUpperCase(color.charAt(i));
            assertTrue(character >= '0');
            assertTrue(character <= 'F');
        }
    }

    @Test
    void test_toPlayerState_mapsFieldsCorrectly() {
        var testPlayer = new Player("color", 1, 2, 3, 4, 5);
        var result = testPlayer.toPlayerState();

        assertEquals(testPlayer.color(), result.color());
        assertEquals(testPlayer.age(), result.age());
        assertEquals(testPlayer.xPosition(), result.x());
        assertEquals(testPlayer.yPosition(), result.y());
    }

    @Test
    void test_moveToNextTick_noVelocity_doesNothing() {
        var testPlayer = Player.createRandomPlayer();
        var expectedX = testPlayer.xPosition();
        var expectedY = testPlayer.yPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(expectedY, testPlayer.yPosition());
    }

    @Test
    void test_moveToNextTick_nonZeroVelocity_changesPosition() {
        var testPlayer = new Player("", 0, 10, 31, 4, 33);
        var expectedX = testPlayer.xPosition() + testPlayer.xVelocity();
        var expectedY = testPlayer.yPosition() + testPlayer.yVelocity();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(expectedY, testPlayer.yPosition());
    }

    @Test
    void test_moveToNextTick_minPositionAndNegativeVelocity_doesNothing() {
        var testPlayer = new Player("", 0, 0, 0, -1, -1);
        var expectedX = testPlayer.xPosition();
        var expectedY = testPlayer.yPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(expectedY, testPlayer.yPosition());
    }

    @Test
    void test_moveToNextTick_maxPositionAndPositiveVelocity_doesNothing() {
        var testPlayer = new Player(
            "",
            0,
            GameConstants.WIDTH,
            GameConstants.HEIGHT,
            1,
            1
        );
        var expectedX = testPlayer.xPosition();
        var expectedY = testPlayer.yPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(expectedY, testPlayer.yPosition());
    }
}
