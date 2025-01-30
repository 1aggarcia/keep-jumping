package io.github.aggarcia.engine;

import static io.github.aggarcia.engine.GameConstants.INIT_PLATFORM_GRAVITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketSession;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.models.GameStore;
import io.github.aggarcia.models.PlayerStore;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@SpringBootTest
public class GameLoopTest {
    @Mock
    private WebSocketSession mockSession;

    @Test
    void test_isRunning_afterConstruction_returnsFalse() {
        assertFalse(testLoop().isRunning());
    }

    @Test
    void test_isRunning_loopStartedWithOnePlayer_returnsTrue()
    throws Exception {
        var gameLoop = testLoop().withTickDelay(0);
        gameLoop.start();
        assertTrue(gameLoop.isRunning());
        gameLoop.forceQuit();
    }

    @Test
    void test_isRunning_afterPlayersCleared_returnsFalse() throws Exception {
        var store = new GameStore();
        store.players().put("", PlayerStore.createRandomPlayer(""));

        var loop = new GameLoop(store).withTickDelay(0);
        loop.start();
        assertTrue(loop.isRunning());
        store.players().clear();

        // gives the loop thread time to notice the change and stop execution
        Thread.sleep(10);
        assertFalse(loop.isRunning());
    }

    @Test
    void test_isRunning_afterSessionsCleared_returnsFalse() throws Exception {
        var store = GameStore.builder()
            .sessions(getSessions())
            .build();
        var gameLoop = new GameLoop(store).withTickDelay(0);
        gameLoop.start();

        store.sessions().clear();
        // gives the loop thread time to notice the change and stop execution
        Thread.sleep(10);
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_forceQuit_activeLoop_stopsLoop() throws Exception {
        var gameLoop = testLoop().withTickDelay(0);
        gameLoop.start();

        gameLoop.forceQuit();
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_onIdleTimeout_activeLoop_throwsException() throws Exception {
        var gameLoop = testLoop().withTickDelay(0);
        gameLoop.start();

        assertThrows(Exception.class, () -> {
            gameLoop.onIdleTimeout(() -> {}, 0);
        });
        gameLoop.forceQuit();
    }

    @Test
    void test_onIdleTimeout_inactiveLoop_runsActionAfterLoopFinishes()
    throws Exception {
        var gameLoop = testLoop();
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            synchronized (sharedList) {
                sharedList.add("test item");
                sharedList.notify();
            }
        }, 0);

        gameLoop.start();
        gameLoop.forceQuit();
        synchronized (sharedList) {
            // give up after 10ms so the test doesn't freeze
            sharedList.wait(10);
            assertEquals(1, sharedList.size());
        }
    }

    @Test
    void test_onIdleTimeout_actionSet_doesNotRunActionBeforeTimeout() throws Exception {
        var gameLoop = testLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 50);

        gameLoop.start();
        gameLoop.forceQuit();
        assertEquals(0, sharedList.size());
    }

    @Test
    void test_start_whileTimeoutActionWaiting_cancelsAction() throws Exception {
        var store = GameStore.builder()
            .players(Map.of("", PlayerStore.createRandomPlayer("")))
            .sessions(Set.of(mockSession))
            .build();

        var gameLoop = new GameLoop(store); 
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 50);

        gameLoop.start();
        gameLoop.forceQuit();

        // need to fill the game store so the thread doesnt immediately stop
        gameLoop.start();

        // gives enough time for the timeout action to execute, but it shouldn't
        Thread.sleep(100);
        assertEquals(0, sharedList.size());
    }

    @Test
    void test_start_whileTimeoutActionWaiting_performsActionAfterLoopCloses()
    throws Exception {
        var gameLoop = testLoop().withTickDelay(0);
        var sharedList = new ArrayList<>();

        gameLoop.onIdleTimeout(() -> {
            sharedList.add("test item");
        }, 50);

        gameLoop.start();
        gameLoop.forceQuit();

        assertEquals(0, sharedList.size());
        gameLoop.start();
        gameLoop.forceQuit();

        Thread.sleep(100);
        assertEquals(1, sharedList.size());
    }

    @Test
    void test_start_afterMaxTime_closesLoop() throws Exception {
        var gameLoop = testLoop()
            .withTickDelay(1)
            .withMaxTime(1);

        gameLoop.start();
        assertTrue(gameLoop.isRunning());
        // even with 50% extra time, watch out for false negatives.
        Thread.sleep(1500);
        assertFalse(gameLoop.isRunning());
    }

    @Test
    void test_start_afterLoopCloses_clearsSessions() throws Exception {
        var store = GameStore.builder()
            .sessions(getSessions())
            .build();
        var gameLoop = new GameLoop(store);

        assertNotEquals(0, store.sessions().size());
        gameLoop.start();
        gameLoop.forceQuit();
        assertEquals(0, store.sessions().size());
    }

    @Test
    void test_start_secondTime_resetsTimeAndPlatformGravity() throws Exception {
        var store = new GameStore();
        var loop = new GameLoop(store);

        loop.start();
        loop.forceQuit();

        // Simulate long running game
        store.tickCount(1 + RandomUtil.getPositiveInt());
        store.gameAgeSeconds(1 + RandomUtil.getPositiveInt());
        store.platformGravity(
            INIT_PLATFORM_GRAVITY + RandomUtil.getPositiveInt());

        loop.start();
        loop.forceQuit();

        assertEquals(0, store.tickCount());
        assertEquals(0, store.gameAgeSeconds());
        assertEquals(INIT_PLATFORM_GRAVITY, store.platformGravity());

    }

    private Set<WebSocketSession> getSessions() {
        var set = new HashSet<WebSocketSession>();
        set.add(mockSession);
        return set;
    }

    private Map<String, PlayerStore> getPlayers() {
        var map = new HashMap<String, PlayerStore>();
        map.put("player1", PlayerStore.createRandomPlayer("player1"));
        map.put("player2", PlayerStore.createRandomPlayer("player2"));
        return map;
    }

    private GameLoop testLoop() {
        var store = GameStore.builder()
            .sessions(getSessions())
            .players(getPlayers())
            .build();
        return new GameLoop(store);
    }
}
