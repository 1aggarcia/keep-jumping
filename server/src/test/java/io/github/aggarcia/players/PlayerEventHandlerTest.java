package io.github.aggarcia.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.players.PlayerControl;
import io.github.aggarcia.players.PlayerControlUpdate;
import io.github.aggarcia.players.PlayerEventHandler;
import io.github.aggarcia.shared.SocketMessage;

public class PlayerEventHandlerTest {
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
