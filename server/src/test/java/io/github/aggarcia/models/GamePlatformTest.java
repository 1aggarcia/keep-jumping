package io.github.aggarcia.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.engine.GameConstants;

@SpringBootTest
public class GamePlatformTest {
    private static final int RANDOM_TEST_ITERATIONS = 100;

    @Test
    void test_generateAtHeight_validHeights_returnsSameHeights() {
        var height15 = GamePlatform.generateAtHeight(15);
        assertEquals(15, height15.y());

        var height345 = GamePlatform.generateAtHeight(345);
        assertEquals(345, height345.y());
    }

    @Test
    void test_generateAtHeight_boundHeights_doesNotThrow() {
        var heightMax = GamePlatform.generateAtHeight(GameConstants.HEIGHT);
        assertEquals(GameConstants.HEIGHT, heightMax.y());
        
        var heightMin = GamePlatform.generateAtHeight(0);
        assertEquals(0, heightMin.y());
    }

    @Test
    void test_generateAtHeight_generatesWidthWithinBounds() {
        for (int i = 0; i < RANDOM_TEST_ITERATIONS; i++) {
            var platform = GamePlatform.generateAtHeight(0);
            assertTrue(platform.width() >= GamePlatform.MIN_WIDTH);
            assertTrue(platform.width() <= GamePlatform.MAX_WIDTH);
        }
    }

    @Test
    void test_generateAtHeight_setsXWithinBounds() {
        for (int i = 0; i < RANDOM_TEST_ITERATIONS; i++) {
            var platform = GamePlatform.generateAtHeight(0);
            assertTrue(platform.x() >= 0);
            assertTrue(platform.x() <= GameConstants.WIDTH - platform.width());
        }
    }

    @Test
    void test_toNextTick_zeroedValues_changesYByGravity() {
        var origin = new GamePlatform(0, 0, 0);
        int gravity = RandomUtil.getPositiveInt();
        var nextPlatform = origin.toNextTick(gravity);
        
        var expected = new GamePlatform(0, 0, gravity);
        assertEquals(expected, nextPlatform);
    }
}

