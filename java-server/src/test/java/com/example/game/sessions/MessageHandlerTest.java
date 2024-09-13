package com.example.game.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;

import com.example.game.sessions.MessageTypes.PlayerControlUpdate;
import com.example.game.game.GameConstants;
import com.example.game.players.Player;
import com.example.game.players.PlayerControl;
import com.example.game.sessions.MessageTypes.SocketMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class MessageHandlerTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int TICKS_PER_SECOND = 1000 / GameConstants.TICK_DELAY_MS;

    private MessageHandler messageHandler;

    @BeforeEach
    void resetState() {
        messageHandler = new MessageHandler();
    }

    @Test
    void test_computeVelocity_noControls_returnsZeroVelocity() throws Exception {
        var message = controlListAsMessage(Collections.emptyList());
        var velocity = messageHandler.computeVelocity(message);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_rightControl_returnsPositiveX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.RIGHT));
        var velocity = messageHandler.computeVelocity(message);
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_leftControl_returnsNegativeX() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.LEFT));
        var velocity = messageHandler.computeVelocity(message);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_computeVelocity_downControl_returnsPositiveY() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.DOWN));
        var velocity = messageHandler.computeVelocity(message);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() > 0);
    }

    @Test
    void test_computeVelocity_upControl_returnsNegativeY() throws Exception {
        var message = controlListAsMessage(List.of(PlayerControl.UP));
        var velocity = messageHandler.computeVelocity(message);
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
        var velocity = messageHandler.computeVelocity(message);
        assertTrue(velocity.xVelocity() < 0);
        assertTrue(velocity.yVelocity() > 0);
    }

    @Test
    void test_advanceToNextTick_tickCountZero_returnsTickCountOne() {
        var response = messageHandler.advanceToNextTick(new HashMap<>(), 0);
        assertEquals(1, response.nextTickCount());
    }

    @Test
    void test_advanceToNextTick_tickCountZero_doesNotRequireUpdate() {
        var response = messageHandler.advanceToNextTick(new HashMap<>(), 0);
        assertFalse(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_maxTickCount_returnsZeroTickCount() {
        var response = messageHandler
            .advanceToNextTick(new HashMap<>(), TICKS_PER_SECOND - 1);
        assertEquals(0, response.nextTickCount());
    }

    @Test
    void test_advanceToNextTick_maxTickCount_requiresUpdate() {
        var response = messageHandler
            .advanceToNextTick(new HashMap<>(), TICKS_PER_SECOND - 1);
        assertTrue(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_motionlessPlayers_doesNotRequireUpdate() {
        var players = Map.of(
            "1", Player.createRandomPlayer(),
            "2", Player.createRandomPlayer()
        );
        var response = messageHandler.advanceToNextTick(players, 0);
        assertFalse(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_motionlessPlayers_doesNotMutatePlayers() {
        var players = Map.of(
            "1", Player.createRandomPlayer(),
            "2", Player.createRandomPlayer()
        );
        var expected1 = players.get("1").clone();
        var expected2 = players.get("2").clone();

        var response = messageHandler.advanceToNextTick(players, 0);
        assertFalse(response.isUpdateNeeded());
        assertEquals(expected1, players.get("1"));
        assertEquals(expected2, players.get("2"));
    }

    @Test
    void test_advanceToNextTick_oneMovingPlayer_requiresUpdate() {
        var players = Map.of(
            "1", Player.createRandomPlayer(),
            "2", new Player("", 0, 0, 0, 1, 0)
        );
        var response = messageHandler.advanceToNextTick(players, 0);
        assertTrue(response.isUpdateNeeded());
    }

    @Test
    void test_advanceToNextTick_oneMovingPlayer_mutatesCorrectPlayer() {
        var players = Map.of(
            "1", Player.createRandomPlayer(),
            "2", new Player("", 0, 0, 0, 1, 0)
        );
        var expected1 = players.get("1").clone();
        var expected2 = new Player("", 0, 1, 0, 1, 0);

        var response = messageHandler.advanceToNextTick(players, 0);
        assertTrue(response.isUpdateNeeded());
        assertEquals(expected1, players.get("1"));
        assertEquals(expected2, players.get("2"));
    }

    private TextMessage controlListAsMessage(List<PlayerControl> pressedControls) {
        try {
            var update = new PlayerControlUpdate(
                SocketMessageType.PLAYER_CONTROL_UPDATE,
                pressedControls
            );
            return new TextMessage(mapper.writeValueAsString(update));
        } catch (JsonProcessingException e) {
            fail(e);
            return null;  // to make the compiler happy
        }
    }
}
