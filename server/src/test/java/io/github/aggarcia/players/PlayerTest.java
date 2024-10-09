package io.github.aggarcia.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.aggarcia.game.GameConstants;

@SpringBootTest
public class PlayerTest {
    @Test
    void test_createRandomPlayer_initializesCorrectConstantValues() {
        var player = Player.createRandomPlayer();

        assertEquals(0, player.score());
        assertEquals(0, player.xVelocity());
        assertEquals(0, player.yVelocity());
        assertEquals("~", player.name());
        assertNotNull(player.color());
        assertTrue(player.hasChanged());
    }

    @Test
    void test_createRandomPlayer_generatesPositionInGameBounds() {
        // run test multiple times since it involves randomness
        for (int i = 0; i < 100; i++) {
            var player = Player.createRandomPlayer();

            assertTrue(player.xPosition() >= 0);
            assertTrue(player.yPosition() >= 0);
            assertTrue(player.xPosition() <= GameConstants.WIDTH - Player.PLAYER_WIDTH);
            assertTrue(player.yPosition() <= GameConstants.HEIGHT - Player.PLAYER_HEIGHT);
        }
    }

    @Test
    void test_createRandomPlayer_generatesPositionAtAFactorOfPlayerSize() {
        // run test multiple times since it involves randomness
        for (int i = 0; i < 100; i++) {
            var player = Player.createRandomPlayer();
            assertEquals(0, player.xPosition() % Player.PLAYER_WIDTH);
            assertEquals(0, player.yPosition() % Player.PLAYER_HEIGHT);
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
        var testPlayer = new Player("", "color", 1, 2, 3, 4, 5, true);
        var result = testPlayer.toPlayerState();

        assertEquals(testPlayer.color(), result.color());
        assertEquals(testPlayer.score(), result.score());
        assertEquals(testPlayer.xPosition(), result.x());
        assertEquals(testPlayer.yPosition(), result.y());
    }

    @Test
    void test_moveToNextTick_noXVelocity_doesNotChangeXPhysics() {
        var testPlayer = Player.createRandomPlayer();
        var expectedX = testPlayer.xPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(0, testPlayer.xVelocity());
    }

    @Test
    void test_moveToNextTick_touchingGround_doesNotChangeYPhysics() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .yPosition(Player.MAX_PLAYER_Y);

        testPlayer.moveToNextTick();
        assertFalse(testPlayer.hasChanged());
        assertEquals(Player.MAX_PLAYER_Y, testPlayer.yPosition());
        assertEquals(0, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_notTouchingGround_appliesGravityToYPhysics() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .yPosition(0)
            .yVelocity(15);
        int expectedY = testPlayer.yVelocity() + Player.GRAVITY;

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(expectedY, testPlayer.yPosition());
        assertEquals(expectedY, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_collisionWithBottom_stopsPlayerOnBottom() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .yPosition(Player.MAX_PLAYER_Y - 1)
            .yVelocity(150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.yVelocity());
        assertEquals(Player.MAX_PLAYER_Y, testPlayer.yPosition());
    }
    
    @Test
    void test_moveToNextTick_collisionWithTop_stopsPlayerOnTop() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .yPosition(1);
        // send the player past the top
        testPlayer.yVelocity(-150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.yVelocity());
        assertEquals(0, testPlayer.yPosition());
    }

    @Test
    void test_moveToNextTick_collisionWithLeft_stopsPlayerOnLeft() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .xPosition(1)
            .xVelocity(-150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(0, testPlayer.xPosition());
    }

    @Test
    void test_moveToNextTick_collisionWithRight_stopsPlayerOnRight() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .xPosition(Player.MAX_PLAYER_X - 1)
            .xVelocity(150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(Player.MAX_PLAYER_X, testPlayer.xPosition());
    }

    @Test
    void test_moveToNextTick_nonZeroXVelocity_changesXPosition() {
        var testPlayer = Player.builder()
            .xPosition(0) 
            .xVelocity(31)
            .build();
        var expectedX = testPlayer.xPosition() + testPlayer.xVelocity();

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(expectedX, testPlayer.xPosition());
    }

    @Test
    void test_moveToNextTick_minXPositionAndNegativeXVelocity_doesNothing() {
        var testPlayer = Player.createRandomPlayer()
            .hasChanged(false)
            .xPosition(0)
            .xVelocity(-1)
            // so that gravity doesnt cause problems
            .yPosition(Player.MAX_PLAYER_Y);

        var expectedX = testPlayer.xPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertFalse(testPlayer.hasChanged());
    }

    @Test
    void test_moveToNextTick_maxPositionAndPositiveVelocity_doesNothing() {
        var testPlayer = Player.builder()
            .xPosition(Player.MAX_PLAYER_X)
            .yPosition(Player.MAX_PLAYER_Y)
            .xPosition(1)
            .yVelocity(1)
            .build();

        var expectedX = testPlayer.xPosition();
        var expectedY = testPlayer.yPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(expectedY, testPlayer.yPosition());
        assertFalse(testPlayer.hasChanged());
    }

    @Test
    void test_addToScore_zeroScore_updatesScore() {
        var testPlayer = Player.createRandomPlayer();

        testPlayer.addToScore(35);
        assertEquals(35, testPlayer.score());
        testPlayer.addToScore(9234);
        assertEquals(35 + 9234, testPlayer.score());
    }
}
