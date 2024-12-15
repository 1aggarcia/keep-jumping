package io.github.aggarcia.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.players.messages.PlayerControlUpdate;
import io.github.aggarcia.players.messages.PlayerJoinUpdate;
import io.github.aggarcia.players.updates.CreatePlayer;
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
    void test_processEvent_playerControlUpdate_callsComputeVelocity()
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
    void test_computeVelocity_noControls_returnsZeroVelocity() throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        var velocity = PlayerEventHandler
            .processControlUpdate("", message, new HashMap<>());
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_rightControl_returnsPositiveX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.RIGHT));
        var velocity = PlayerEventHandler
            .processControlUpdate("", message, new HashMap<>());
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_leftControl_returnsNegativeX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.LEFT));
        var velocity = PlayerEventHandler
            .processControlUpdate("", message, new HashMap<>());
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_downControl_returnsZero() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.DOWN));
        var velocity = PlayerEventHandler
            .processControlUpdate("", message, new HashMap<>());
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_upControl_returnsNegativeY() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.UP));
        var velocity = PlayerEventHandler
            .processControlUpdate("", message, new HashMap<>());
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() < 0);
    }

    @Test
    void
    test_computeVelocity_multipleControls_returnsCorrectVelocity()
    throws Exception {
        var controls =
            List.of(PlayerControl.LEFT, PlayerControl.DOWN, PlayerControl.LEFT);
        var message = controlListAsMessage(controls);
        var velocity = PlayerEventHandler
            .processControlUpdate("", message, new HashMap<>());
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
}
