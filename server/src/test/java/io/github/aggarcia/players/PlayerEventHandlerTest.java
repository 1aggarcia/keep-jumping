package io.github.aggarcia.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.players.messages.PlayerControlUpdate;
import io.github.aggarcia.players.messages.PlayerJoinUpdate;
import io.github.aggarcia.shared.SocketMessage;

public class PlayerEventHandlerTest {
    @Test
    void test_processMessage_badMessage_throwsException() {
        assertThrows(
            JsonProcessingException.class,
            () -> PlayerEventHandler.processMessage(new TextMessage(""))
        );
    }

    @Test
    void test_processMessage_missingType_throwsExecption() {
        var message = new TextMessage("{}");
        assertThrows(
            IllegalArgumentException.class,
            () -> PlayerEventHandler.processMessage(message)
        );
    }

    @Test
    void test_processMessage_badType_throwsException() throws Exception {
        var message = new TextMessage("{\"type\":\"bad type\"}");
        assertThrows(
            IllegalArgumentException.class,
            () -> PlayerEventHandler.processMessage(message)
        );
    }

    @Test
    void test_processMessage_playerJoinUpdate_returnsName()
    throws Exception {
        var payload = new PlayerJoinUpdate(SocketMessage.PLAYER_JOIN_UPDATE, "testName");
        var message = new TextMessage(new ObjectMapper().writeValueAsString(payload));
        assertEquals("testName", PlayerEventHandler.processMessage(message));
    }

    @Test
    void test_processMessage_playerControlUpdate_callsComputeVelocity()
    throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        assertEquals(
            PlayerEventHandler.computeVelocity(message),
            PlayerEventHandler.processMessage(message)
        );
    }

    @Test
    void test_computeVelocity_noControls_returnsZeroVelocity() throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        var velocity = PlayerEventHandler.computeVelocity(message);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_rightControl_returnsPositiveX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.RIGHT));
        var velocity = PlayerEventHandler.computeVelocity(message);
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_leftControl_returnsNegativeX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.LEFT));
        var velocity = PlayerEventHandler.computeVelocity(message);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_downControl_returnsPositiveY() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.DOWN));
        var velocity = PlayerEventHandler.computeVelocity(message);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() > 0);
    }

    @Test
    void test_computeVelocity_upControl_returnsNegativeY() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.UP));
        var velocity = PlayerEventHandler.computeVelocity(message);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() < 0);
    }

    @Test
    void
    test_computeVelocity_multipleControls_returnsCorrectVelocity()
    throws Exception {
        var controls =
            List.of(PlayerControl.DOWN, PlayerControl.LEFT, PlayerControl.DOWN);
        var message = controlListAsMessage(controls);
        var velocity = PlayerEventHandler.computeVelocity(message);
        assertTrue(velocity.xVelocity() < 0);
        assertTrue(velocity.yVelocity() > 0);
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
