package com.example.game.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import com.example.game.players.Player;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

@SpringBootTest
public class GameLoopTest {
    private final List<WebSocketSession> emptySesisons = Collections.emptyList();

    @Test
    void test_isRunning_afterConstruction_returnsFalse() {
        assertFalse(new GameLoop().isRunning());
    }

    @Test
    void test_isRunning_loopStartedWithOnePlayer_returnsTrue() {
        var gameLoop = new GameLoop().withTickDelay(0);
        gameLoop.start(emptySesisons, getTestPlayers());
        assertTrue(gameLoop.isRunning());
        gameLoop.interrupt();
    }

    @Test
    void test_isRunning_afterPlayersCleared_returnsFalse() throws Exception  {
        var gameLoop = new GameLoop().withTickDelay(0);
        var players = getTestPlayers();
        gameLoop.start(emptySesisons, players);

        players.clear();
        // gives the loop thread time to notice the change and stop execution
        Thread.sleep(10);
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_interrupt_activeLoop_stopsLoop() throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        gameLoop.start(emptySesisons, getTestPlayers());

        // should be syncronous, but if this test starts failing
        // it may be best to add 'Thread.sleep'
        gameLoop.interrupt();
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_onIdleTimeout_activeLoop_throwsException() {
        var gameLoop = new GameLoop().withTickDelay(0);
        gameLoop.start(emptySesisons, getTestPlayers());

        assertThrows(Exception.class, () -> {
            gameLoop.onIdleTimeout(() -> {}, 0);
        });
        gameLoop.interrupt();
    }

    @Test
    void test_onIdleTimeout_inactiveLoop_runsActionAfterLoopFinishes() throws Exception {
        // debugging this one sucks

        var gameLoop = new GameLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            synchronized (sharedList) {
                sharedList.add("test item");
                sharedList.notify();
            }
        }, 0);

        gameLoop.start(emptySesisons, getTestPlayers());
        gameLoop.interrupt();
        synchronized (sharedList) {
            sharedList.wait();
            assertEquals(1, sharedList.size());
        }
    }

    @Test
    void test_onIdleTimeout_actionSet_doesNotRunActionBeforeTimeout() throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 10);

        gameLoop.start(emptySesisons, getTestPlayers());
        gameLoop.interrupt();
        assertEquals(0, sharedList.size());
    }

    @Test
    void test_start_whileTimeoutActionWaiting_cancelsAction() throws Exception {
        // this one sucks to debug too

        var gameLoop = new GameLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
            System.out.println("test item added");
        }, 50);

        gameLoop.start(emptySesisons, getTestPlayers());
        // sleeping forces the game thread to start
        // (since this thread isn't doing anything)
        Thread.sleep(10);
        gameLoop.interrupt();
        Thread.sleep(10);  // forces the game thread to stop
        gameLoop.start(emptySesisons, getTestPlayers());

        // gives enough time for the timeout action to execute, but it shouldn't
        Thread.sleep(100);
        assertEquals(0, sharedList.size());
    }

    private Map<String, Player> getTestPlayers() {
        return new HashMap<>(Map.of("", Player.createRandomPlayer()));
    }
}
