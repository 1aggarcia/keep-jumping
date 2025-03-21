package io.github.aggarcia.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.web.socket.WebSocketSession;

import ch.qos.logback.core.testUtil.RandomUtil;
import io.github.aggarcia.engine.GameConstants;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Data container for game state.
 */
@Data
@Builder
@NoArgsConstructor  // for convenience
@AllArgsConstructor  // for the builder
@Accessors(fluent = true)
public class GameStore {
    /** Randomly generated, does not change after construction. */
    private final int instanceId = RandomUtil.getPositiveInt() % 999;

    /**
     * Players that are no longer in the game whose scores need to be saved.
     */
    private final BlockingQueue<PlayerStore> unprocessedLosers =
        new LinkedBlockingQueue<>();

    @Builder.Default
    private final Set<WebSocketSession> sessions =
        Collections.synchronizedSet(new HashSet<>());

    @Builder.Default
    private final Map<String, PlayerStore> players =
        Collections.synchronizedMap(new HashMap<>());

    @Builder.Default
    private List<GamePlatform> platforms =
        Collections.synchronizedList(new ArrayList<>());

    @Builder.Default
    private int gameAgeSeconds = 0;

    @Builder.Default
    private int tickCount = 0;

    @Builder.Default
    private int platformGravity = GameConstants.INIT_PLATFORM_GRAVITY;

    /**
     * Callback function to run when the game is started.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Runnable startAction = () -> {};

    /**
     * Notifies the subscribed event listener of the game start event.
     */
    public void tiggerStartEvent() {
        startAction.run();
    }

    /**
     * Subscribe to {@link #tiggerStartEvent()} by attaching a callback
     * function to the event. Only one action can be subscribed to the event
     * at a time
     * @param action
     */
    public void onStartEvent(Runnable action) {
        startAction = action;
    }
}
