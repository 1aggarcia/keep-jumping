package io.github.aggarcia.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.aggarcia.game.GameConstants;
import io.github.aggarcia.game.GameEventHandler;
import io.github.aggarcia.players.Player;

public class GameEventHandlerTest {
    private static final int TICKS_PER_SECOND =
        1000 / GameConstants.TICK_DELAY_MS;

    @Test
    void test_advanceToNextTick_tickCountZero_returnsTickCountOne() {
        var response = GameEventHandler.advanceToNextTick(new HashMap<>(), 0);
        assertEquals(1, response.nextTickCount());
    }

    @Test
    void test_advanceToNextTick_tickCountZero_doesNotRequireUpdate() {
        var response = GameEventHandler.advanceToNextTick(new HashMap<>(), 0);
        assertFalse(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_maxTickCount_returnsZeroTickCount() {
        var response = GameEventHandler
            .advanceToNextTick(new HashMap<>(), TICKS_PER_SECOND - 1);
        assertEquals(0, response.nextTickCount());
    }

    @Test
    void test_advanceToNextTick_maxTickCount_requiresUpdate() {
        var response = GameEventHandler
            .advanceToNextTick(new HashMap<>(), TICKS_PER_SECOND - 1);
        assertTrue(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_newPlayer_requiresUpdate() {
        var players = Map.of("", Player.createRandomPlayer());
        var response = GameEventHandler.advanceToNextTick(players, 0);
        assertTrue(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_newPlayerOnSecondTick_doesNotRequireUpdate() {
        var players = Map.of("", Player.createRandomPlayer());
        GameEventHandler.advanceToNextTick(players, 0);
        var secondResponse = GameEventHandler.advanceToNextTick(players, 0);
        assertFalse(secondResponse.isUpdateNeeded()); 
    }

    @Test
    void test_advanceToNextTick_motionlessPlayers_doesNotRequireUpdate() {
        var players = createTestPlayers();
        var response = GameEventHandler.advanceToNextTick(players, 0);
        assertFalse(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_motionlessPlayers_doesNotMutatePlayers() {
        var players = createTestPlayers();
        var expected1 = players.get("1").clone();
        var expected2 = players.get("2").clone();

        var response = GameEventHandler.advanceToNextTick(players, 0);
        assertFalse(response.isUpdateNeeded());
        assertEquals(expected1, players.get("1"));
        assertEquals(expected2, players.get("2"));
    }

    @Test
    void test_advanceToNextTick_oneMovingPlayer_requiresUpdate() {
        var players = createTestPlayers();
        players.put("2", new Player("", 0, 0, 0, 1, 0, false));
    
        var response = GameEventHandler.advanceToNextTick(players, 0);
        assertTrue(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_oneMovingPlayer_mutatesCorrectPlayer() {
        var players = Map.of(
            "1", Player.createRandomPlayer(),
            "2", new Player("", 0, 0, 0, 1, 0, true)
        );
        var expected1 = players.get("1").clone();
        expected1.hasChanged(false);
        var expected2 = new Player("", 0, 1, 0, 1, 0, false);

        var response = GameEventHandler.advanceToNextTick(players, 0);
        assertTrue(response.isUpdateNeeded());
        assertEquals(expected1, players.get("1"));
        assertEquals(expected2, players.get("2"));
    }

    private Map<String, Player> createTestPlayers() {
        var player1 = Player.createRandomPlayer();
        var player2 = Player.createRandomPlayer();
        player1.hasChanged(false);
        player2.hasChanged(false);

        // Map.of creates an immutable map, so to make
        //it mutable is has to be copied
        return new HashMap<>(Map.of(
            "1", player1,
            "2", player2
        ));
    }
}
