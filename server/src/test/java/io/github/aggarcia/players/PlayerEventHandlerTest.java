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

import io.github.aggarcia.players.events.PlayerControlUpdate;
import io.github.aggarcia.players.events.PlayerJoinUpdate;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.ErrorUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;
import io.github.aggarcia.shared.SocketMessage;

public class PlayerEventHandlerTest {
    @Test
    void test_processEvent_badMessage_throwsException() {
        assertThrows(JsonProcessingException.class, () -> {
            PlayerEventHandler
                .processEvent("", new TextMessage(""), Collections.emptyMap());
        });
    }

    @Test
    void test_processEvent_missingType_throwsExecption() {
        var message = new TextMessage("{}");
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerEventHandler
                .processEvent("", message, Collections.emptyMap());
        });
    }

    @Test
    void test_processEvent_badType_throwsException() throws Exception {
        var message = new TextMessage("{\"type\":\"bad type\"}");
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerEventHandler
                .processEvent("", message, Collections.emptyMap());
        });
    }

    @Test
    void test_processEvent_playerJoinUpdate_returnsName()
    throws Exception {
        var payload = new PlayerJoinUpdate(SocketMessage.PLAYER_JOIN_UPDATE, "testName");
        var message = new TextMessage(new ObjectMapper().writeValueAsString(payload));

        CreatePlayer result = (CreatePlayer) PlayerEventHandler
            .processEvent("client1", message, Collections.emptyMap());

        assertTrue(result instanceof CreatePlayer);
        assertFalse(result.isError());
        assertEquals("client1", result.client());
        assertEquals("testName", result.player().name());
    }

    @Test
    void test_processEvent_playerControlUpdate_callsProcessControlUpdate()
    throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        assertEquals(
            PlayerEventHandler
                .processControlUpdate("client1", message, Collections.emptyMap()),
            PlayerEventHandler
                .processEvent("client1", message, Collections.emptyMap())
        );
    }

    @Test
    void test_processControlUpdate_missingPlayer_returnsError() throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        // client id "test client" doesnt exist in the sessions (empty map)
        var result = (ErrorUpdate) PlayerEventHandler.processControlUpdate(
            "test client",
            message,
            new HashMap<>()
        );
        assertTrue(result instanceof ErrorUpdate);
    }

    @Test
    void test_processControlUpdate_noControls_returnsZeroVelocity() throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        var velocity = processControlWithValidPlayer(message);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlUpdate_rightControl_returnsPositiveX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.RIGHT));
        var velocity = processControlWithValidPlayer(message);
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlUpdate_leftControl_returnsNegativeX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.LEFT));
        var velocity = processControlWithValidPlayer(message);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlUpdate_downControl_returnsZero() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.DOWN));
        var velocity = processControlWithValidPlayer(message);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlUpdate_upControl_returnsNegativeY() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.UP));
        var velocity = processControlWithValidPlayer(message);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() < 0);
    }

    @Test
    void
    test_processControlUpdate_multipleControls_returnsCorrectVelocity()
    throws Exception {
        var controls =
            List.of(PlayerControl.LEFT, PlayerControl.DOWN, PlayerControl.LEFT);
        var message = controlListAsMessage(controls);
        var velocity = processControlWithValidPlayer(message);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    private TextMessage controlListAsMessage(List<PlayerControl> pressedControls) {
        try {
            var update = new PlayerControlUpdate(
                SocketMessage.PLAYER_CONTROL_UPDATE,
                pressedControls
            );
            return new TextMessage(new ObjectMapper().writeValueAsString(update));
        } catch (JsonProcessingException e) {
            fail(e);
            return null;  // to make the compiler happy
        }
    }

    /**
     * Wrapper for calling processControlUpdate with a valid player session
     */
    private UpdateVelocity
    processControlWithValidPlayer(TextMessage event)throws Exception {
        var players = Map.of("client1", Player.createRandomPlayer("player1"));
        return (UpdateVelocity) PlayerEventHandler
            .processControlUpdate("client1", event, players);
    }
}
