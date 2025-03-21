package io.github.aggarcia.engine;

import static io.github.aggarcia.engine.TickProcessor.createGamePing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.engine.TickProcessor.TickResponse;
import io.github.aggarcia.messages.Generated.GamePing;
import io.github.aggarcia.messages.Generated.Platform;
import io.github.aggarcia.messages.Generated.Player;
import io.github.aggarcia.messages.Generated.SocketMessage;
import io.github.aggarcia.messages.Generated.SocketMessage.PayloadCase;
import io.github.aggarcia.models.GamePlatform;
import io.github.aggarcia.models.GameStore;
import io.github.aggarcia.models.PlayerStore;
import static io.github.aggarcia.engine.GameConstants.INIT_PLATFORM_GRAVITY;
import static io.github.aggarcia.engine.GameConstants.LEVELUP_PLATFORM_GRAVITY;
import static io.github.aggarcia.engine.GameConstants.PLATFORM_SPEEDUP_INTERVAL;
import static io.github.aggarcia.engine.TickProcessor.TICKS_PER_SECOND;
import static io.github.aggarcia.engine.TickProcessor.advanceToNextTick;

public class TickProcessorTest {
    static final int RANDOM_TRIALS = 1000;

    // commenting out all the tests on `isUpdateNeeded` since it serves
    // no purpose (for now)

    @Test
    void test_advanceToNextTick_tickCountZero_setTickCountToOne() {
        var store = new GameStore().tickCount(0);
        advanceToNextTick(store);
        assertEquals(1, store.tickCount());
    }

    // @Test
    // void test_advanceToNextTick_tickCountZero_doesNotRequireUpdate() {
    //     var response = advanceTickWithTickCount(0);
    //     assertFalse(response.isUpdateNeeded());
    // }

    @Test
    void test_advanceToNextTick_maxTickCount_setsTickCountToZero() {
        var store = new GameStore()
            .tickCount(TickProcessor.TICKS_PER_SECOND - 1);
        advanceToNextTick(store);
        assertEquals(0, store.tickCount());
    }

    @Test
    void test_advanceToNextTick_maxTickCount_incrementsAge() {
        var store = new GameStore()
            .tickCount(TICKS_PER_SECOND - 1)
            .gameAgeSeconds(0);
        advanceToNextTick(store);
        assertEquals(1, store.gameAgeSeconds());
    }

    @Test
    void test_advanceToNextTick_gameAgeAtNextLevel_increasesPlatformGravity() {
        int initGravity = RandomUtil.getPositiveInt();
        var store = new GameStore()
            .tickCount(TICKS_PER_SECOND - 1)
            .gameAgeSeconds(PLATFORM_SPEEDUP_INTERVAL - 1)
            .platformGravity(initGravity);

        advanceToNextTick(store);
        assertEquals(
            store.platformGravity(),
            initGravity + LEVELUP_PLATFORM_GRAVITY
        );
    }

    @Test
    void test_advanceToNextTick_ticksNotAtNextLevel_doesNotChangeGraivty() {
        int initGravity = RandomUtil.getPositiveInt();
        var store = new GameStore()
            .tickCount(TICKS_PER_SECOND - 2)
            .gameAgeSeconds(PLATFORM_SPEEDUP_INTERVAL - 1)
            .platformGravity(initGravity);

        advanceToNextTick(store);
        assertEquals(store.platformGravity(), initGravity);
    }

    // @Test
    // void test_advanceToNextTick_maxTickCount_requiresUpdate() {
    //     var response = advanceTickWithTickCount(TICKS_PER_SECOND - 1);
    //     assertTrue(response.isUpdateNeeded());
    // }

    // @Test
    // void test_advanceToNextTick_newPlayer_requiresUpdate() {
    //     var players = Map.of("", Player.createRandomPlayer());
    //     var response = advanceTickWithPlayers(players);
    //     assertTrue(response.isUpdateNeeded());
    // }

    // @Test
    // void test_advanceToNextTick_newPlayerOnSecondTick_doesNotRequireUpdate() {
    //     var players = Map.of(
    //         "", Player.createRandomPlayer().yPosition(GameConstants.HEIGHT)
    //     );
    //     advanceTickWithPlayers(players);
    //     var secondResponse = advanceTickWithPlayers(players);
    //     assertFalse(secondResponse.isUpdateNeeded()); 
    // }

    // @Test
    // void test_advanceToNextTick_motionlessPlayers_doesNotRequireUpdate() {
    //     var players = createTestPlayers();
    //     var response = advanceTickWithPlayers(players);
    //     assertFalse(response.isUpdateNeeded());
    // }

    // @Test
    // void test_advanceToNextTick_oneMovingPlayer_requiresUpdate() {
    //     var players = createTestPlayers();
    //     players.put("2", new Player("", 0, 0, 0, 1, 0, false));
    
    //     var response = advanceTickWithPlayers(players);
    //     assertTrue(response.isUpdateNeeded());
    // }

    @Test
    void test_advanceToNextTick_onePlayer_advancesPlayer() {
        var player = PlayerStore.createRandomPlayer("");
        var expected = player.clone()
            .moveToNextTick()
            .hasChanged(false);
        advanceTickWithPlayers(Map.of("1", player));
        assertEquals(expected, player);
    }

    @Test
    void test_advanceToNextTick_multiplePlayers_advancesEachPlayer() {
        // player 1 should not move
        PlayerStore player1 = PlayerStore.createRandomPlayer("");
        PlayerStore player2 = PlayerStore.builder()
            .xPosition(0)
            .yPosition(0)
            .xPosition(0)
            .yVelocity(1)
            .hasChanged(true)
            .build();

        var players = Map.of(
            "1", player1,
            "2", player2
        );
        var expected1 = players.get("1")
            .clone()
            .moveToNextTick()
            .hasChanged(false);
        var expected2 = players.get("2")
            .clone()
            .moveToNextTick()
            .hasChanged(false);

        var response = advanceTickWithPlayers(players);
        assertTrue(response.isUpdateNeeded());
        assertEquals(expected1, players.get("1"));
        assertEquals(expected2, players.get("2"));
    }

    @Test
    void test_advanceToNextTick_playerTouchingGround_removesPlayerInResponse() {
        var player1 = PlayerStore
            .createRandomPlayer("")
            .yPosition(PlayerStore.MAX_PLAYER_Y);
        var players = Map.of("player1", player1);
        var response = advanceTickWithPlayers(players);
    
        assertEquals(List.of("player1"), response.playersToRemove());
    }

    @Test
    void test_advanceToNextTick_platformInBounds_incluesPlatformInResponse() {
        List<GamePlatform> platforms = new ArrayList<>();
        platforms.add(new GamePlatform(0, 0, 0));

        var expected = platforms.get(0).toNextTick(INIT_PLATFORM_GRAVITY);
        var nextPlatformsState = advanceTickWithPlatforms(platforms).nextPlatformsState();
        assertEquals(1, nextPlatformsState.size());
        assertEquals(expected, nextPlatformsState.get(0));
    }

    @Test
    void test_advanceToNextTick_platformOutOfBounds_excludesPlatform() {
        List<GamePlatform> platforms = new ArrayList<>();
        platforms.add(new GamePlatform(0, 0, GameConstants.HEIGHT - 1));

        var nextPlatformsState = advanceTickWithPlatforms(platforms).nextPlatformsState();
        assertEquals(0, nextPlatformsState.size());
    }

    @Test
    void test_advanceToNextTick_nextTickIsZero_updatesPlayerScore() {
        var players = createTestPlayers();
        assertEquals(0, players.get("1").score());
        assertEquals(0, players.get("2").score());

        var store = GameStore.builder()
            .players(players)
            .tickCount(-1)
            .build();

        advanceToNextTick(store);
        assertEquals(0, store.tickCount());
        assertEquals(
            TickProcessor.SCORE_PER_SECOND, players.get("1").score());
        assertEquals(
            TickProcessor.SCORE_PER_SECOND, players.get("2").score());
    }

    @Test
    void test_advanceToNextTick_nextTickIsNotZero_doesNotChangePlayerScore() {
        var players = createTestPlayers();
        assertEquals(0, players.get("1").score());
        assertEquals(0, players.get("2").score());

        var store = GameStore.builder()
            .players(players)
            .tickCount(0)
            .build();

        advanceToNextTick(store);
        assertEquals(1, store.tickCount());
        assertEquals(0, players.get("1").score());
        assertEquals(0, players.get("2").score());
    }

    @Test
    void test_advanceToNextTick_playerOnPlatform_playerFallsWithPlatform() {
        PlayerStore testPlayer = PlayerStore.builder()
            .xPosition(50)
            .yPosition(50)
            .xVelocity(50)
            .yVelocity(10)
            .build();
        var testPlatform = new GamePlatform(200, 0, 50 + PlayerStore.PLAYER_HEIGHT);

        var store = GameStore.builder()
            .players(Map.of("", testPlayer))
            .platforms(List.of(testPlatform))
            .build();

        // modifies player
        advanceToNextTick(store);

        assertEquals(100, testPlayer.xPosition());
        assertEquals(50, testPlayer.xVelocity());
        assertEquals(50 + INIT_PLATFORM_GRAVITY, testPlayer.yPosition());
        assertEquals(INIT_PLATFORM_GRAVITY, testPlayer.yVelocity());
    }

    @Test
    void test_createGamePing_minimalStore_returnsPingSocketMessage() {
        SocketMessage ping = createGamePing(new GameStore());
        assertTrue(ping.hasGamePing());
        assertEquals(PayloadCase.GAMEPING, ping.getPayloadCase());
    }

    @Test
    void test_createGamePing_minimalStore_returnsCorrectPing() {
        var store = new GameStore();
        GamePing ping = createGamePing(store).getGamePing();

        assertEquals(0, ping.getServerAge());
        assertEquals(0, ping.getPlayersCount());
        assertEquals(0, ping.getPlatformsCount());
    }

    @Test
    void test_createGamePing_storeWithManyValues_returnsCorrectPing() {
        var players = createTestPlayers();
        var platform = GamePlatform.generateAtHeight(0);
        int age = RandomUtil.getPositiveInt();
        var store = GameStore.builder()
            .gameAgeSeconds(age)
            .players(players)
            .platforms(List.of(platform))
            .build();
        
        GamePing ping = createGamePing(store).getGamePing();
        assertEquals(age, ping.getServerAge());

        List<Player> playersList = players.values().stream()
            .map(p -> Player.newBuilder()
                .setColor(p.color())
                .setName(p.name())
                .setScore(p.score())
                .setX(p.xPosition())
                .setY(p.yPosition())
                .build()
            )
            .toList();
        assertEquals(playersList, ping.getPlayersList());

        List<Platform> platformsList = List.of(
            Platform.newBuilder()
                .setWidth(platform.width())
                .setX(platform.x())
                .setY(platform.y())
                .build()
        );
        assertEquals(platformsList, ping.getPlatformsList());
    }

    @Test
    void test_shouldSpawnPlatform_platformCloseToTop_returnsFalse() {
        for (int i = 0; i < RANDOM_TRIALS; i++) {
            var minPlatform = GamePlatform.generateAtHeight(0);
            var maxPlatform = GamePlatform
                .generateAtHeight(TickProcessor.MIN_PLATFORM_SPACING - 1);

            assertFalse(TickProcessor.shouldSpawnPlatform(List.of(minPlatform)));
            assertFalse(TickProcessor.shouldSpawnPlatform(List.of(maxPlatform)));
        }
    }

    @Test
    void test_shouldSpawnPlatform_platformFarFromTop_returnsTrue() {
        for (int i = 0; i < RANDOM_TRIALS; i++) {
            var platform = GamePlatform
                .generateAtHeight(TickProcessor.MAX_PLATFORM_SPACING + 1);
            assertTrue(TickProcessor.shouldSpawnPlatform(List.of(platform)));
        }
    }

    /**
     * @return map of two players with keys "1", "2".
     *  Players are on the ground and are motionless
     */
    private Map<String, PlayerStore> createTestPlayers() {
        var player1 = PlayerStore.createRandomPlayer("")
            .yPosition(0)
            .hasChanged(false);

        var player2 = PlayerStore.createRandomPlayer("")
            .yPosition(0)
            .hasChanged(false);

        // Map.of creates an immutable map, so to make
        //it mutable is has to be copied
        return new HashMap<>(Map.of(
            "1", player1,
            "2", player2
        ));
    }


    // Helpers for common test patterns
    private TickResponse advanceTickWithPlayers(Map<String, PlayerStore> players) {
        var store = GameStore.builder().players(players).build();
        return advanceToNextTick(store);
    }

    private TickResponse advanceTickWithPlatforms(List<GamePlatform> platforms) {
        var store = new GameStore()
            .platforms(platforms)
            .platformGravity(GameConstants.INIT_PLATFORM_GRAVITY);
        return advanceToNextTick(store);
    }
}
