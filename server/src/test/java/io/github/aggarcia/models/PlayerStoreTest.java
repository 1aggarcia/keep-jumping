package io.github.aggarcia.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.engine.GameConstants;
import io.github.aggarcia.leaderboard.LeaderboardEntry;

@SpringBootTest
public class PlayerStoreTest {
    // for testing platform collisions, reused across a few tests
    static final PlayerStore testCollisionPlayer = PlayerStore.builder()
        .xPosition(200)
        .yPosition(200)
        .xVelocity(0)
        .yVelocity(0)
        .build();

    static final GamePlatform testCollisionPlatform =
        new GamePlatform(100, 150, 200 + PlayerStore.PLAYER_HEIGHT);

    // collides only on the X axis
    static final GamePlatform testCollisionPlatformX =
        new GamePlatform(100, 150, 0);

    // collides only on the Y axis
    static final GamePlatform testCollisionPlatformY =
        new GamePlatform(100, 0, 200 + PlayerStore.PLAYER_HEIGHT);

    @Test
    void test_createRandomPlayer_initializesCorrectConstantValues() {
        var player = PlayerStore.createRandomPlayer("new test player");

        assertEquals(0, player.score());
        assertEquals(0, player.xVelocity());
        assertEquals(0, player.yVelocity());
        assertEquals("new test player", player.name());
        assertNotNull(player.color());
        assertTrue(player.hasChanged());
    }

    @Test
    void test_createRandomPlayer_generatesPositionInGameBounds() {
        // run test multiple times since it involves randomness
        for (int i = 0; i < 100; i++) {
            var player = PlayerStore.createRandomPlayer("");

            assertTrue(player.xPosition() >= 0);
            assertTrue(player.yPosition() >= 0);
            assertTrue(player.xPosition() <= GameConstants.WIDTH - PlayerStore.PLAYER_WIDTH);
            assertTrue(player.yPosition() <= PlayerStore.MAX_SPAWN_HEIGHT);
        }
    }

    @Test
    void test_createRandomPlayer_generatesPositionAtAFactorOfPlayerSize() {
        // run test multiple times since it involves randomness
        for (int i = 0; i < 100; i++) {
            var player = PlayerStore.createRandomPlayer("");
            assertEquals(0, player.xPosition() % PlayerStore.PLAYER_WIDTH);
            assertEquals(0, player.yPosition() % PlayerStore.PLAYER_HEIGHT);
        }
    }

    @Test
    void test_createRandomPlayer_generatesLegalHexColor() {
        String color = PlayerStore.createRandomPlayer("").color();
        assertEquals(color.length(), 7);
        assertEquals(color.charAt(0), '#');

        for (int i = 1; i < color.length(); i++) {
            char character = Character.toUpperCase(color.charAt(i));
            assertTrue(character >= '0');
            assertTrue(character <= 'F');
        }
    }

    @Test
    void test_createAbovePlatform_placesPlayerAbovePlatform() {
        var platform = GamePlatform.generateAtHeight(100);
        var player = PlayerStore.createAbovePlatform("", platform);

        assertEquals(
            platform.y() - PlayerStore.SPAWN_HEIGHT,
            player.yPosition(),
            "Player is above platform"
        );

        int middleOfPlatform = 
            platform.x() + ((platform.width() - PlayerStore.PLAYER_WIDTH) / 2);
        assertEquals(
            middleOfPlatform, player.xPosition(),
            "Player is in the middle of the platform"
        );
    }

    @Test
    void test_moveToNextTick_noXVelocity_doesNotChangeXPhysics() {
        var testPlayer = PlayerStore.createRandomPlayer("");
        var expectedX = testPlayer.xPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertEquals(0, testPlayer.xVelocity());
    }

    @Test
    void test_moveToNextTick_touchingGround_doesNotChangeYPhysics() {
        var testPlayer = PlayerStore.createRandomPlayer("")
            .hasChanged(false)
            .yPosition(PlayerStore.MAX_PLAYER_Y);

        testPlayer.moveToNextTick();
        assertFalse(testPlayer.hasChanged());
        assertEquals(PlayerStore.MAX_PLAYER_Y, testPlayer.yPosition());
        assertEquals(0, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_notTouchingGround_appliesGravityToYPhysics() {
        var testPlayer = PlayerStore.createRandomPlayer("")
            .hasChanged(false)
            .yPosition(0)
            .yVelocity(15);
        int expectedY = testPlayer.yVelocity() + PlayerStore.GRAVITY;

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(expectedY, testPlayer.yPosition());
        assertEquals(expectedY, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_collisionWithBottom_stopsPlayerOnBottom() {
        var testPlayer = PlayerStore.createRandomPlayer("")
            .hasChanged(false)
            .yPosition(PlayerStore.MAX_PLAYER_Y - 1)
            .yVelocity(150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.yVelocity());
        assertEquals(PlayerStore.MAX_PLAYER_Y, testPlayer.yPosition());
    }
    
    @Test
    void test_moveToNextTick_collisionWithTop_stopsPlayerOnTop() {
        var testPlayer = PlayerStore.createRandomPlayer("")
            .hasChanged(false)
            .yPosition(PlayerStore.MIN_PLAYER_Y + 1);
        // send the player past the top
        testPlayer.yVelocity(-150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.yVelocity());
        assertEquals(PlayerStore.MIN_PLAYER_Y, testPlayer.yPosition());
    }

    @Test
    void test_moveToNextTick_collisionWithLeft_stopsPlayerOnLeft() {
        var testPlayer = PlayerStore.createRandomPlayer("")
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
        var testPlayer = PlayerStore.createRandomPlayer("")
            .hasChanged(false)
            .xPosition(PlayerStore.MAX_PLAYER_X - 1)
            .xVelocity(150);

        testPlayer.moveToNextTick();
        assertTrue(testPlayer.hasChanged());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(PlayerStore.MAX_PLAYER_X, testPlayer.xPosition());
    }

    @Test
    void test_moveToNextTick_nonZeroXVelocity_changesXPosition() {
        PlayerStore testPlayer = PlayerStore.builder()
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
        var testPlayer = PlayerStore.createRandomPlayer("")
            .hasChanged(false)
            .xPosition(0)
            .xVelocity(-1)
            // so that gravity doesnt cause problems
            .yPosition(PlayerStore.MAX_PLAYER_Y);

        var expectedX = testPlayer.xPosition();

        testPlayer.moveToNextTick();
        assertEquals(expectedX, testPlayer.xPosition());
        assertFalse(testPlayer.hasChanged());
    }

    @Test
    void test_moveToNextTick_maxPositionAndPositiveVelocity_doesNothing() {
        PlayerStore testPlayer = PlayerStore.builder()
            .xPosition(PlayerStore.MAX_PLAYER_X)
            .yPosition(PlayerStore.MAX_PLAYER_Y)
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
    void test_moveToNextTick_onTopOfPlatform_setsYToPlatformGravity() {
        int platformGravity = RandomUtil.getPositiveInt();
        PlayerStore testPlayer = testCollisionPlayer.clone();
        testPlayer.moveToNextTick(
            List.of(testCollisionPlatform), platformGravity
        );
        assertEquals(200, testPlayer.xPosition());
        assertEquals(200, testPlayer.yPosition());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(platformGravity, testPlayer.yVelocity());
        assertTrue(testPlayer.hasChanged());
    }

    @Test
    void test_moveToNextTick_platformCollidesOnlyX_appliesGravity() {
        PlayerStore testPlayer = testCollisionPlayer.clone(); 

        // crosses the player on the X axis but not the Y axis
        testPlayer.moveToNextTick(
            List.of(testCollisionPlatformX), RandomUtil.getPositiveInt()
        );
        assertEquals(200, testPlayer.xPosition());
        assertEquals(200 + PlayerStore.GRAVITY, testPlayer.yPosition());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(PlayerStore.GRAVITY, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_platformCollidesOnlyY_appliesGravity() {
        PlayerStore testPlayer = testCollisionPlayer.clone();

        // crosses the player on the Y axis but not the X axis
        testPlayer.moveToNextTick(
            List.of(testCollisionPlatformY), RandomUtil.getPositiveInt()
        );
        assertEquals(200, testPlayer.xPosition());
        assertEquals(200 + PlayerStore.GRAVITY, testPlayer.yPosition());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(PlayerStore.GRAVITY, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_multipleNonCollidingPlatforms_appliesGravity() {
        PlayerStore testPlayer = testCollisionPlayer.clone();
        // none of these collide with the player
        var platforms = List.of(
            testCollisionPlatformX,
            testCollisionPlatformY,
            testCollisionPlatformX
        );
        testPlayer.moveToNextTick(platforms, RandomUtil.getPositiveInt());

        assertEquals(200, testPlayer.xPosition());
        assertEquals(200 + PlayerStore.GRAVITY, testPlayer.yPosition());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(PlayerStore.GRAVITY, testPlayer.yVelocity());
    }

    @Test
    void test_moveToNextTick_oneCollidingPlatform_doesNothing() {
        PlayerStore testPlayer = testCollisionPlayer.clone();
        // last platform collides wiht player
        var platforms = List.of(
            testCollisionPlatformX,
            testCollisionPlatformY,
            testCollisionPlatform
        );
        int gravity = RandomUtil.getPositiveInt();
        testPlayer.moveToNextTick(platforms, gravity);

        assertEquals(200, testPlayer.xPosition());
        assertEquals(200, testPlayer.yPosition());
        assertEquals(0, testPlayer.xVelocity());
        assertEquals(gravity, testPlayer.yVelocity());
    }

    @Test
    void test_addToScore_zeroScore_updatesScore() {
        var testPlayer = PlayerStore.createRandomPlayer("");

        testPlayer.addToScore(35);
        assertEquals(35, testPlayer.score());
        testPlayer.addToScore(9234);
        assertEquals(35 + 9234, testPlayer.score());
    }

    @Test
    void test_createLeaderboardEntry_storesCorrectData() {
        int randomScore = RandomUtil.getPositiveInt();
        var testPlayer = PlayerStore
            .createRandomPlayer("test")
            .score(randomScore);

        LeaderboardEntry entry = testPlayer.createLeaderboardEntry();
        assertEquals(entry.player(), testPlayer.name());
        assertEquals(entry.score(), randomScore);
    }
}
