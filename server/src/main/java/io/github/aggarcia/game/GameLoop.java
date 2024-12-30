package io.github.aggarcia.game;

import java.io.IOException;
import java.util.Collection;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.platforms.GamePlatform;
import static io.github.aggarcia.game.GameEventHandler.advanceToNextTick;


/**
 * Interface to hide the thread management logic of running the game loop.
 */
public class GameLoop {
    private final GameStore gameStore;

     /**
     * Amount of time to wait between each tick.
     */
    private int tickDelayMs = GameConstants.TICK_DELAY_MS;

    private int maxTimeSeconds = GameConstants.MAX_TIME_SECONDS;

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

    public GameLoop(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    // PUBLIC API //

    /**
     * @return reference to the store used by the loop
     */
    public GameStore gameStore() {
        return this.gameStore;
    }

    /**
     * Override the default value for the tick delay.
     * @param tickDelayMs - number of milliseconds to wait between ticks
     * @return reference to the same object
     */
    public GameLoop withTickDelay(int tickDelayMs) {
        if (this.isRunning()) {
            throw new RuntimeException(
                "Cannot change tick delay while loop is running");
        }
        this.tickDelayMs = tickDelayMs;
        return this;
    }

    /**
     * Override the default value for the tick delay.
     * @param maxTimeSeconds - max time the game loop can be active
     * @return reference to the same object
     */
    public GameLoop withMaxTime(int maxTimeSeconds) {
        if (this.isRunning()) {
            throw new RuntimeException(
                "Cannot change max loop time while loop is running");
        }
        this.maxTimeSeconds = maxTimeSeconds;
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
     * Start a new thread running the game loop, if none is currently active.
     * @return true if a new loop was started, false if a loop was already
     * running and no new loop was created
     */
    public boolean start() {
        if (this.isRunning()) {
            return false;
        }
        if (idleThread.isAlive()) {
            idleThread.interrupt();
            try {
                idleThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        this.loopThread = new Thread(this::runGameLoop);
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
     */
    private void runGameLoop() {
        int gameAgeSeconds = 0;
        final var players = gameStore.players();
        final var sessions = gameStore.sessions();
        gameStore.platforms(GameEventHandler.spawnInitPlatforms());

        System.out.println("Starting game loop");
        if (idleThread.isAlive()) {
            idleThread.interrupt();
            try {
                idleThread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        while (
            players.size() > 0
            && sessions.size() > 0
            && gameAgeSeconds < this.maxTimeSeconds
        ) {
            var response = advanceToNextTick(gameStore);
            gameStore.tickCount(response.nextTickCount());
            this.gameStore.platforms(response.nextPlatformsState());
            if (GameEventHandler.shouldSpawnPlatform(gameStore.platforms())) {
                gameStore.platforms().add(GamePlatform.generateAtHeight(0));
            }
            if (gameStore.tickCount() == 0) {
                gameAgeSeconds++;
            }
            if (response.isUpdateNeeded()) try {
                var update = GamePing.fromGameState(gameStore, gameAgeSeconds);
                broadcast(sessions, update);
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
        players.clear();
        for (var session : sessions) {
            try {
                session.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        sessions.clear();
        idleThread = new Thread(idleTimeoutAction);
        idleThread.start();
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
