package io.github.aggarcia.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import io.github.aggarcia.players.Player;

import java.util.List;
import java.util.ArrayList;

@SpringBootTest
public class GameLoopTest {
    @Mock
    private WebSocketSession mockSession;

    @Test
    void test_isRunning_afterConstruction_returnsFalse() {
        assertFalse(new GameLoop().isRunning());
    }

    @Test
    void test_isRunning_loopStartedWithOnePlayer_returnsTrue()
    throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        gameLoop.start(getSessions());
        assertTrue(gameLoop.isRunning());
        gameLoop.forceQuit();
    }

    // TODO: test this after implementing dependency injection for GameStore
    // void test_isRunning_afterPlayersCleared_returnsFalse() throws Exception  {
    // }

    @Test
    void test_isRunning_afterSessionsCleared_returnsFalse() throws Exception  {
        var gameLoop = new GameLoop().withTickDelay(0);
        var sessions = getSessions();
        gameLoop.start(sessions);

        sessions.clear();
        // gives the loop thread time to notice the change and stop execution
        Thread.sleep(10);
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_forceQuit_activeLoop_stopsLoop() throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        gameLoop.start(getSessions());

        gameLoop.forceQuit();
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_onIdleTimeout_activeLoop_throwsException() throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        gameLoop.start(getSessions());

        assertThrows(Exception.class, () -> {
            gameLoop.onIdleTimeout(() -> {}, 0);
        });
        gameLoop.forceQuit();
    }

    @Test
    void test_onIdleTimeout_inactiveLoop_runsActionAfterLoopFinishes()
    throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            synchronized (sharedList) {
                sharedList.add("test item");
                sharedList.notify();
            }
        }, 0);

        gameLoop.start(getSessions());
        gameLoop.forceQuit();
        synchronized (sharedList) {
            // give up after 10ms so the test doesn't freeze
            sharedList.wait(10);
            assertEquals(1, sharedList.size());
        }
    }

    @Test
    void test_onIdleTimeout_actionSet_doesNotRunActionBeforeTimeout() throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 50);

        gameLoop.start(getSessions());
        gameLoop.forceQuit();
        assertEquals(0, sharedList.size());
    }

    @Test
    void test_start_whileTimeoutActionWaiting_cancelsAction() throws Exception {
        var gameLoop = new GameLoop();
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 50);

        gameLoop.start(getSessions());
        gameLoop.forceQuit();

        // need to fill the game store so the thread doesnt immediately stop
        gameLoop.gameStore().players().put("", Player.createRandomPlayer(""));
        gameLoop.gameStore().sessions().add(mockSession);
        gameLoop.start(getSessions());

        // gives enough time for the timeout action to execute, but it shouldn't
        Thread.sleep(100);
        assertEquals(0, sharedList.size());
    }

    @Test
    void test_start_whileTimeoutActionWaiting_performsActionAfterLoopCloses()
    throws Exception {
        var gameLoop = new GameLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 50);

        gameLoop.start(getSessions());
        gameLoop.forceQuit();

        assertEquals(0, sharedList.size());
        gameLoop.start(getSessions());
        gameLoop.forceQuit();

        Thread.sleep(100);
        assertEquals(1, sharedList.size());
    }

    @Test
    void test_start_afterMaxTime_closesLoop() throws Exception {
        var gameLoop = new GameLoop()
            .withTickDelay(1)
            .withMaxTime(1);

        gameLoop.start(getSessions());
        assertTrue(gameLoop.isRunning());
        // even with 50% extra time, watch out for false negatives.
        Thread.sleep(1500);
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_start_afterLoopCloses_clearsSessions() throws Exception {
        // TODO: test player clearing after implementing dependency injection
        var gameLoop = new GameLoop();
        var sessions = getSessions();

        assertNotEquals(0, sessions.size());
        gameLoop.start(sessions);
        gameLoop.forceQuit();
        assertEquals(0, sessions.size());
    }

    private List<WebSocketSession> getSessions() {
        var list = new ArrayList<WebSocketSession>();
        list.add(mockSession);
        return list;
    }
}
