package io.github.aggarcia.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.socket.WebSocketSession;

import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.Player;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @Builder.Default
    private final Set<WebSocketSession> sessions = new HashSet<>();

    @Builder.Default
    private final Map<String, Player> players = new HashMap<>();

    @Builder.Default
    private List<GamePlatform> platforms = new ArrayList<>();

    @Builder.Default
    private int tickCount = 0;
}
