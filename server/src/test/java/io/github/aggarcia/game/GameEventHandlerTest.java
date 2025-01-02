package io.github.aggarcia.game;

import static io.github.aggarcia.game.GameEventHandler.createGamePing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.aggarcia.game.GameEventHandler.TickResponse;
import io.github.aggarcia.generated.SocketMessageOuterClass.GamePing;
import io.github.aggarcia.generated.SocketMessageOuterClass.Platform;
import io.github.aggarcia.generated.SocketMessageOuterClass.Player;
import io.github.aggarcia.generated.SocketMessageOuterClass.SocketMessage;
import io.github.aggarcia.generated.SocketMessageOuterClass.SocketMessage.PayloadCase;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.PlayerStore;

public class GameEventHandlerTest {
    static final int RANDOM_TRIALS = 1000;

    // commenting out all the tests on `isUpdateNeeded` since it serves
    // no purpose (for now)

    @Test
    void test_advanceToNextTick_tickCountZero_returnsTickCountOne() {
        var response = advanceTickWithTickCount(0);
        assertEquals(1, response.nextTickCount());
    }

    // @Test
    // void test_advanceToNextTick_tickCountZero_doesNotRequireUpdate() {
    //     var response = advanceTickWithTickCount(0);
    //     assertFalse(response.isUpdateNeeded());
    // }

    @Test
    void test_advanceToNextTick_maxTickCount_returnsZeroTickCount() {
        var response = advanceTickWithTickCount(
            GameEventHandler.TICKS_PER_SECOND - 1
        );
        assertEquals(0, response.nextTickCount());
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
    void test_advanceToNextTick_playerTouchingGround_removesPlayer() {
        // must be mutable, List.of creates immutable lists
        var player1 = PlayerStore
            .createRandomPlayer("")
            .yPosition(PlayerStore.MAX_PLAYER_Y);
        var store = new GameStore();
        store.players().put("", player1);

        GameEventHandler.advanceToNextTick(store);
        assertTrue(store.players().isEmpty());
    }

    @Test
    void test_advanceToNextTick_platformInBounds_incluesPlatformInResponse() {
        List<GamePlatform> platforms = new ArrayList<>();
        platforms.add(new GamePlatform(0, 0, 0));

        var expected = platforms.get(0).toNextTick();
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

        var result = GameEventHandler.advanceToNextTick(store);
        assertEquals(0, result.nextTickCount());
        assertEquals(
            GameEventHandler.SCORE_PER_SECOND, players.get("1").score());
        assertEquals(
            GameEventHandler.SCORE_PER_SECOND, players.get("2").score());
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

        var result = GameEventHandler.advanceToNextTick(store);
        assertEquals(1, result.nextTickCount());
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
        GameEventHandler.advanceToNextTick(store);

        assertEquals(100, testPlayer.xPosition());
        assertEquals(50, testPlayer.xVelocity());
        assertEquals(50 + GamePlatform.PLATFORM_GRAVITY, testPlayer.yPosition());
        assertEquals(GamePlatform.PLATFORM_GRAVITY, testPlayer.yVelocity());
    }

    @Test
    void test_createGamePing_minimalStore_returnsPingSocketMessage() {
        SocketMessage ping = createGamePing(new GameStore(), 0);
        assertTrue(ping.hasGamePing());
        assertEquals(PayloadCase.GAMEPING, ping.getPayloadCase());
    }

    @Test
    void test_createGamePing_minimalStore_returnsCorrectPing() {
        var store = new GameStore();
        GamePing ping = createGamePing(store, 15).getGamePing();

        assertEquals(15, ping.getServerAge());
        assertEquals(0, ping.getPlayersCount());
        assertEquals(0, ping.getPlatformsCount());
    }

    @Test
    void test_createGamePing_storeWithManyValues_returnsCorrectPing() {
        var players = createTestPlayers();
        var platform = GamePlatform.generateAtHeight(0);
        var store = GameStore.builder()
            .players(players)
            .platforms(List.of(platform))
            .build();
        
        GamePing ping = createGamePing(store, 5).getGamePing();

        assertEquals(5, ping.getServerAge());

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
                .generateAtHeight(GameEventHandler.MIN_PLATFORM_SPACING - 1);

            assertFalse(GameEventHandler.shouldSpawnPlatform(List.of(minPlatform)));
            assertFalse(GameEventHandler.shouldSpawnPlatform(List.of(maxPlatform)));
        }
    }

    @Test
    void test_shouldSpawnPlatform_platformFarFromTop_returnsTrue() {
        for (int i = 0; i < RANDOM_TRIALS; i++) {
            var platform = GamePlatform
                .generateAtHeight(GameEventHandler.MAX_PLATFORM_SPACING + 1);
            assertTrue(GameEventHandler.shouldSpawnPlatform(List.of(platform)));
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

    private TickResponse advanceTickWithTickCount(int tickCount) {
        var store = new GameStore().tickCount(tickCount);
        return GameEventHandler.advanceToNextTick(store);
    }

    private TickResponse advanceTickWithPlayers(Map<String, PlayerStore> players) {
        var store = GameStore.builder().players(players).build();
        return GameEventHandler.advanceToNextTick(store);
    }

    private TickResponse advanceTickWithPlatforms(List<GamePlatform> platforms) {
        var store = new GameStore().platforms(platforms);
        return GameEventHandler.advanceToNextTick(store);
    }
}
