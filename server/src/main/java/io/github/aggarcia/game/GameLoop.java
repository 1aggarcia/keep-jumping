package io.github.aggarcia.game;

import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.Player;

/**
 * Interface to hide the thread management logic of running the game loop.
 */
public class GameLoop {
     /**
     * Amount of time to wait between each tick.
     */
    private int tickDelayMs = GameConstants.TICK_DELAY_MS;

    /**
     * Thread for the game loop.
     */
    private Thread loopThread = new Thread();

    /**
     * Thread for the idle action to perform after the loop stops.
     */
    private Thread idleThread = new Thread();

    /**
     * Action to perform after the game loop has been closed.
     */
    private Runnable idleTimeoutAction = () -> {};


    // PUBLIC API //

    /**
     * Override the default value for the tick delay.
     * @param tickDelayMs - number of milliseconds to wait between ticks
     * @return reference to the same object
     */
    public GameLoop withTickDelay(int tickDelayMs) {
        this.tickDelayMs = tickDelayMs;
        return this;
    }

    /**
     * Set an action to run after the game loop is inactive for a period of
     * time. Similar to JavaScripts 'setTimeout' function.
     * Should not be called when the game loop is running.
     * @param action - runnable action to execute
     * @param delayMs - number of milliseconds to wait after the game loop ends
     *  before calling the action.
     * @return a refernce to the same object
     */
    public GameLoop onIdleTimeout(Runnable action, int delayMs) {
        if (this.isRunning() || this.idleThread.isAlive()) {
            throw new IllegalStateException(
                "Cannot set idle action while loop is running");
        }
        this.idleTimeoutAction = () -> {
            try {
                Thread.sleep(delayMs);
                action.run();
            } catch (InterruptedException e) {
                System.out.println("Game loop idle action interrupted");
            }
        };
        return this;
    }

    /**
     * @return True if the game loop is running, false otherwise.
     * Does not describe the state of the idle timeout action.
     */
    public boolean isRunning() {
        return this.loopThread.isAlive() && !this.loopThread.isInterrupted();
    }

    /**
     * Begin the game loop with a reference to the players and open sessions.
     * @param sessions - all open client sessions
     * @return true if a new loop was started, false if a loop was already
     * running and no new loop was created
     */
    public boolean start(Map<WebSocketSession, Player> sessions) {
        if (this.isRunning()) {
            return false;
        }
        this.loopThread = new Thread(() -> runGameLoop(sessions));
        this.loopThread.start();
        return true;
    }

    /**
     * Interrupt the game loop, wait for its thread to stop execution.
     */
    public void forceQuit() throws InterruptedException {
        this.loopThread.interrupt();
        this.loopThread.join();
    }

    // PRIVATE UTILITIES //

    /**
     * Internal thread function for the game loop.
     * @param sessions - collection of all sessions to broadcast to
     */
    private void runGameLoop(Map<WebSocketSession, Player> sessions) {
        final var players = sessions.values();

        int serverAge = 0;  // in seconds
        int tickCount = 0;
        List<GamePlatform> platforms = new ArrayList<>();

        System.out.println("Starting game loop");
        if (idleThread.isAlive()) {
            idleThread.interrupt();
        }
        while (sessions.size() > 0) {
            var response = GameEventHandler
                .advanceToNextTick(players, platforms, tickCount);

            tickCount = response.nextTickCount();
            platforms = response.nextPlatformsState();
            maybeAddPlatform(platforms, tickCount);
            if (tickCount == 0) {
                serverAge++;
            }
            if (response.isUpdateNeeded()) try {
                var update = GameUpdate.fromGameState(
                    players,
                    platforms,
                    serverAge
                );
                broadcast(sessions.keySet(), update);
            } catch (JsonProcessingException e) {
                System.err.println(e);
            }
            try {
                Thread.sleep(tickDelayMs);
            } catch (InterruptedException e) {
                System.err.println(e);
                break;
            }
        }
        System.out.println("Closing game loop");
        idleThread = new Thread(idleTimeoutAction);
        idleThread.start();
    }

    /**
     * Decides randomly to add or not to add
     * a new platform to the passed in list.
     */
    private void maybeAddPlatform(List<GamePlatform> platforms, int tickCount) {
        // so that platforms are not too close to each other
        if (tickCount % 8 != 0) return;
        if (RandomUtil.getPositiveInt() % 3 != 0) return;

        platforms.add(GamePlatform.createRandomPlatform());
    }

    /**
     * Send a message to muliple clients at once.
     * @param message message of a JSON serializable type. Will be stringified
     * with the Jackson object mapper.
     */
    private synchronized <T> void broadcast(
        Collection<WebSocketSession> sessions,
        T message
    ) throws JsonProcessingException {
        var stringMessage = new ObjectMapper().writeValueAsString(message);

        for (WebSocketSession session : sessions) try {
            synchronized (session) {
                if (!session.isOpen()) continue;
                session.sendMessage(new TextMessage(stringMessage));
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
