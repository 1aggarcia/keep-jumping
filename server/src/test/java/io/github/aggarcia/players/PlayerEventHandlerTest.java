package io.github.aggarcia.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.game.GameStoreTest;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.events.ControlChangeEvent;
import io.github.aggarcia.players.events.JoinEvent;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.ErrorUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;

import static io.github.aggarcia.players.PlayerEventHandler.processEvent;
import static io.github.aggarcia.players.PlayerEventHandler.processJoin;
import static io.github.aggarcia.players.PlayerEventHandler.processControlChange;;

public class PlayerEventHandlerTest {
    // players cannot jump if they are falling faster than this speed
    static final int Y_VELOCITY_JUMP_CUTOFF = (2 * GamePlatform.PLATFORM_GRAVITY) - 1;

    @Test
    void test_processEvent_badMessage_throwsException() {
        assertThrows(JsonProcessingException.class, () -> {
            processEvent("", new TextMessage(""), new GameStore());
        });
    }

    @Test
    void test_processEvent_badType_throwsException() throws Exception {
        var message = new TextMessage("{\"type\":\"bad type\"}");
        assertThrows(JsonProcessingException.class, () -> {
            processEvent("", message, new GameStore());
        });
    }

    @Test
    void test_processEvent_playerJoinUpdate_returnsName()
    throws Exception {
        var payload = new JoinEvent("testName");
        var message = new TextMessage(new ObjectMapper().writeValueAsString(payload));

        CreatePlayer result =
            (CreatePlayer) processEvent("client1", message, new GameStore());

        assertTrue(result instanceof CreatePlayer);
        assertEquals("client1", result.client());
        assertEquals("testName", result.player().name());
    }

    @Test
    void test_processEvent_playerControlUpdate_callsProcessControlUpdate()
    throws Exception {
        var event = new ControlChangeEvent(Collections.emptyList());
        var message = serialize(event);
        assertEquals(
            processControlChange("client1", event, new GameStore()),
            processEvent("client1", message, new GameStore())
        );
    }

    @Test
    void test_processControlChange_missingPlayer_returnsError() {
        var event = new ControlChangeEvent(Collections.emptyList());
        // client id "test client" doesnt exist in the sessions (empty map)
        var result = (ErrorUpdate) processControlChange(
            "test client",
            event,
            new GameStore()
        );
        assertTrue(result instanceof ErrorUpdate);
    }

    @Test
    void test_processControlChange_noControls_returnsZeroVelocity() {
        var event = new ControlChangeEvent(Collections.emptyList());
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_rightControl_returnsPositiveX() {
        var event = new ControlChangeEvent(List.of(PlayerControl.RIGHT));
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_leftControl_returnsNegativeX() {
        var event = new ControlChangeEvent(List.of(PlayerControl.LEFT));
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_downControl_returnsZero() {
        var event = new ControlChangeEvent(List.of(PlayerControl.DOWN));
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_upControlOnGround_returnsNegativeY() {
        var event = new ControlChangeEvent(List.of(PlayerControl.UP));
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() < 0);
    }

    @Test
    void test_processControlChange_upControlFallingSlowly_returnsNegative() {
        var event = new ControlChangeEvent(List.of(PlayerControl.UP));
        Player player1 = Player.builder()
            .xPosition(0)
            .xVelocity(0)
            .yPosition(50)
            .yVelocity(Y_VELOCITY_JUMP_CUTOFF)
            .name("player1")
            .build();
        
        var velocity = (UpdateVelocity) processControlChange(
            "player1",
            event,
            testStateWithPlayer(player1)
        );

        assertEquals(-PlayerEventHandler.PLAYER_JUMP_SPEED, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_upControlFallingQuickly_returnsSameY() {
        var event = new ControlChangeEvent(List.of(PlayerControl.UP));
        Player player1 = Player.builder()
            .xPosition(0)
            .xVelocity(0)
            .yPosition(50)
            .yVelocity(Y_VELOCITY_JUMP_CUTOFF + 1)
            .name("player1")
            .build();

        var velocity = (UpdateVelocity) processControlChange(
            "player1",
            event,
            testStateWithPlayer(player1)
        );

        assertEquals(Y_VELOCITY_JUMP_CUTOFF + 1, velocity.yVelocity());
    }

    @Test
    void
    test_processControlChange_multipleControls_returnsCorrectVelocity()
    throws Exception {
        var controls =
            List.of(PlayerControl.LEFT, PlayerControl.DOWN, PlayerControl.LEFT);
        var event = new ControlChangeEvent(controls);
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_movingPlayer_maintainsMotion() {
        var event = new ControlChangeEvent(List.of(PlayerControl.LEFT));
        Player player1 = Player.builder()
            .xPosition(0)
            .xVelocity(0)
            .yPosition(50)
            .yVelocity(1000)  // falling very quickly
            .name("player1")
            .build();

        var velocity = (UpdateVelocity) processControlChange(
            "player1",
            event,
            testStateWithPlayer(player1)
        );
        assertEquals("player1", velocity.clientId());
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(1000, velocity.yVelocity());
    }

    @Test
    void test_processJoin_firstPlayer_returnsIsFirstPlayerTrue() {
        var event = new JoinEvent("");
        var createUpdate =
            (CreatePlayer) processJoin("", event, new GameStore());
        assertTrue(createUpdate.isFirstPlayer());
    }

    @Test
    void test_processJoin_manyPlayers_returnsIsFirstPlayerFalse() {
        var event = new JoinEvent("");

        Map<String, Player> players = new HashMap<>();
        // create one less than the limit, so there's room for one more player
        for (int i = 0; i < PlayerEventHandler.MAX_PLAYER_COUNT - 1; i++) {
            var stringI = Integer.toString(i);
            players.put(stringI, Player.createRandomPlayer(stringI));
        }

        var store = GameStore.builder().players(players).build();
        var createUpdate = (CreatePlayer) processJoin("", event, store);
        assertFalse(createUpdate.isFirstPlayer());
    }

    @Test
    void test_processJoin_maxPlayers_returnsError() {
        var event = new JoinEvent("");

        Map<String, Player> players = new HashMap<>();
        for (int i = 0; i < PlayerEventHandler.MAX_PLAYER_COUNT; i++) {
            var stringI = Integer.toString(i);
            players.put(stringI, Player.createRandomPlayer(stringI));
        }

        var store = GameStore.builder().players(players).build();
        var update = processJoin("", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_uniquePlayer_returnsPlayerWithCorrectName() {
        var event = new JoinEvent("player1");
        var createUpdate =
            (CreatePlayer) processJoin("client1", event, new GameStore());
        assertEquals("client1", createUpdate.client());
        assertEquals("player1", createUpdate.player().name());
    }

    @Test
    void test_processJoin_duplicateClient_returnsError() {
        var event = new JoinEvent("");
        var player = Player.createRandomPlayer("duplicate_client");
        var store = testStateWithPlayer(player);

        var update = processJoin("duplicate_client", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_duplicatePlayer_returnsError() {
        var event = new JoinEvent("duplicate_player");
        var player = Player.createRandomPlayer("duplicate_player");
        var store = testStateWithPlayer(player);

        var update = processJoin("", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_sameNameCapitalizedDifferently_returnsError() {
        var event = new JoinEvent("abcd");
        var player = Player.createRandomPlayer("AbCd");
        var store = testStateWithPlayer(player);

        var update = processJoin("", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    private <T> TextMessage serialize(T data) {
        try {
            return new TextMessage(new ObjectMapper().writeValueAsString(data));
        } catch (JsonProcessingException e) {
            fail(e);
            return null;  // to make the compiler happy
        }
    }

    private GameStore testStateWithPlayer(Player player) {
        Map<String, Player> players = new HashMap<>();
        players.put(player.name(), player);

        return GameStore.builder()
            .players(players)
            .build();
    }

    /**
     * Wrapper for calling processControlChange with a valid player session
     */
    private UpdateVelocity
    processControlWithValidPlayer(ControlChangeEvent event) {
        var state = GameStore.builder()
            .players(Map.of("client1", Player.createRandomPlayer("player1")))
            .build();
        return (UpdateVelocity) processControlChange("client1", event, state);
    }
}
