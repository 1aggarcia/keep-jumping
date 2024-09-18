package com.example.game.game;

import java.io.IOException;
import java.util.Map;
import java.util.Collection;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.example.game.players.Player;
import com.example.game.sessions.MessageHandler;
import com.example.game.sessions.MessageTypes.GameUpdate;
import com.example.game.sessions.Singletons;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Interface to hide the thread management logic of running the game loop.
 */
public class GameLoop {
    private static final MessageHandler handler = new MessageHandler();

    private Thread loopThread = new Thread();
    private Thread idleThread = new Thread();
    private int tickDelayMs = GameConstants.TICK_DELAY_MS;


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
        this.idleThread = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                action.run();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        });
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
     * @param players - all players
     * @return true if a new loop was started, false if a loop was already
     * running and no new loop was created
     */
    public boolean start(
        Collection<WebSocketSession> sessions,
        Map<String, Player> players
    ) {
        if (this.isRunning()) {
            return false;
        }
        this.loopThread = new Thread(() -> runGameLoop(sessions, players));
        this.loopThread.start();
        return true;
    }

    /**
     * Interrupt the game loop, stopping execution.
     */
    public void interrupt() {
        this.loopThread.interrupt();
    }


    // PRIVATE UTILITIES //

    /**
     * Internal thread function for the game loop.
     * @param sessions - collection of all sessions to broadcast to
     * @param players - reference to player state
     */
    private void runGameLoop(
        Collection<WebSocketSession> sessions,
        Map<String, Player> players
    ) {
        int serverAge = 0;  // in seconds
        int tickCount = 0;

        System.out.println("Starting game loop");
        if (idleThread.isAlive()) {
            idleThread.interrupt();
        }
        while (players.size() > 0) {
            var response = handler.advanceToNextTick(players, tickCount);
            tickCount = response.nextTickCount();
            if (tickCount == 0) {
                serverAge++;
            }
            if (response.isUpdateNeeded()) try {
                var update = GameUpdate.fromGameState(players, serverAge);
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
        var stringMessage = Singletons.objectMapper.writeValueAsString(message);

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
