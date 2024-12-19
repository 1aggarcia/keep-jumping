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

import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.events.ControlChangeEvent;
import io.github.aggarcia.players.events.JoinEvent;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.ErrorUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;

public class PlayerEventHandlerTest {
    // players cannot jump if they are falling faster than this speed
    static final int Y_VELOCITY_JUMP_CUTOFF = (2 * GamePlatform.PLATFORM_GRAVITY) - 1;

    @Test
    void test_processEvent_badMessage_throwsException() {
        assertThrows(JsonProcessingException.class, () -> {
            PlayerEventHandler
                .processEvent("", new TextMessage(""), Collections.emptyMap());
        });
    }

    @Test
    void test_processEvent_badType_throwsException() throws Exception {
        var message = new TextMessage("{\"type\":\"bad type\"}");
        assertThrows(JsonProcessingException.class, () -> {
            PlayerEventHandler
                .processEvent("", message, Collections.emptyMap());
        });
    }

    @Test
    void test_processEvent_playerJoinUpdate_returnsName()
    throws Exception {
        var payload = new JoinEvent("testName");
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
        var event = new ControlChangeEvent(Collections.emptyList());
        var message = serialize(event);
        assertEquals(
            PlayerEventHandler
                .processControlChange("client1", event, Collections.emptyMap()),
            PlayerEventHandler
                .processEvent("client1", message, Collections.emptyMap())
        );
    }

    @Test
    void test_processControlChange_missingPlayer_returnsError() throws Exception {
        var event = new ControlChangeEvent(Collections.emptyList());
        // client id "test client" doesnt exist in the sessions (empty map)
        var result = (ErrorUpdate) PlayerEventHandler.processControlChange(
            "test client",
            event,
            new HashMap<>()
        );
        assertTrue(result instanceof ErrorUpdate);
    }

    @Test
    void test_processControlChange_noControls_returnsZeroVelocity() throws Exception {
        var event = new ControlChangeEvent(Collections.emptyList());
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_rightControl_returnsPositiveX() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.RIGHT));
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_leftControl_returnsNegativeX() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.LEFT));
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_downControl_returnsZero() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.DOWN));
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_upControlOnGround_returnsNegativeY() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.UP));
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() < 0);
    }

    @Test
    void test_processControlChange_upControlFallingSlowly_returnsNegative() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.UP));
        Player player1 = Player.builder()
            .xPosition(0)
            .xVelocity(0)
            .yPosition(50)
            .yVelocity(Y_VELOCITY_JUMP_CUTOFF)
            .build();
        Map<String, Player> sessions = Map.of("player1", player1);

        var velocity = (UpdateVelocity) PlayerEventHandler
            .processControlChange("player1", event, sessions);

        assertEquals(-PlayerEventHandler.PLAYER_JUMP_SPEED, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_upControlFallingQuickly_returnsSameY() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.UP));
        Player player1 = Player.builder()
            .xPosition(0)
            .xVelocity(0)
            .yPosition(50)
            .yVelocity(Y_VELOCITY_JUMP_CUTOFF + 1)
            .build();
        Map<String, Player> sessions = Map.of("player1", player1);

        var velocity = (UpdateVelocity) PlayerEventHandler
            .processControlChange("player1", event, sessions);

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
    void test_processControlChange_movingPlayer_maintainsMotion() throws Exception {
        var event = new ControlChangeEvent(List.of(PlayerControl.LEFT));
        Player player1 = Player.builder()
            .xPosition(0)
            .xVelocity(0)
            .yPosition(50)
            .yVelocity(1000)  // falling very quickly
            .build();
        Map<String, Player> sessions = Map.of("player1", player1);
        var velocity = (UpdateVelocity) PlayerEventHandler
            .processControlChange("player1", event, sessions);
        assertEquals("player1", velocity.clientId());
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(1000, velocity.yVelocity());
    }

    // TODO: tests for processJoin

    private <T> TextMessage serialize(T data) {
        try {
            return new TextMessage(new ObjectMapper().writeValueAsString(data));
        } catch (JsonProcessingException e) {
            fail(e);
            return null;  // to make the compiler happy
        }
    }

    /**
     * Wrapper for calling processControlChange with a valid player session
     */
    private UpdateVelocity
    processControlWithValidPlayer(ControlChangeEvent event)throws Exception {
        var players = Map.of("client1", Player.createRandomPlayer("player1"));
        return (UpdateVelocity) PlayerEventHandler
            .processControlChange("client1", event, players);
    }
}
