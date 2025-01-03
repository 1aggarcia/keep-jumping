package io.github.aggarcia.players;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.github.aggarcia.game.GameConstants;
import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.generated.SocketMessageOuterClass.ControlChangeEvent;
import io.github.aggarcia.generated.SocketMessageOuterClass.JoinEvent;
import io.github.aggarcia.generated.SocketMessageOuterClass.PlayerControl;
import io.github.aggarcia.generated.SocketMessageOuterClass.SocketMessage;
import io.github.aggarcia.platforms.GamePlatform;
import io.github.aggarcia.players.updates.CreateFirstPlayer;
import io.github.aggarcia.players.updates.CreatePlayer;
import io.github.aggarcia.players.updates.ErrorUpdate;
import io.github.aggarcia.players.updates.UpdateVelocity;

import static io.github.aggarcia.players.PlayerEventHandler.processEvent;
import static io.github.aggarcia.players.PlayerEventHandler.processJoin;
import static io.github.aggarcia.players.PlayerEventHandler.MAX_NAME_LENGTH;
import static io.github.aggarcia.players.PlayerEventHandler.processControlChange;;

public class PlayerEventHandlerTest {
    // players cannot jump if they are falling faster than this speed
    static final int Y_VELOCITY_JUMP_CUTOFF = (2 * GamePlatform.PLATFORM_GRAVITY) - 1;

    @Test
    void test_processEvent_playerJoinUpdate_returnsName() {
        var event = joinEvent("testName");
        var wrappedEvent = SocketMessage.newBuilder()
            .setJoinEvent(event).build();

        CreateFirstPlayer result = (CreateFirstPlayer) processEvent(
            "client1",
            wrappedEvent,
            new GameStore()
        );

        assertTrue(result instanceof CreateFirstPlayer);
        assertEquals("client1", result.client());
        assertEquals("testName", result.player().name());
    }

    @Test
    void test_processEvent_playerControlUpdate_callsProcessControlUpdate() {
        var event = controlChangeEvent();
        var wrappedEvent = SocketMessage.newBuilder()
            .setControlChangeEvent(event).build();
        var store = GameStore.builder()
            .players(Map.of("client1", PlayerStore.createRandomPlayer("")))
            .build();

        assertEquals(
            processControlChange("client1", event, store),
            processEvent("client1", wrappedEvent, store)
        );
    }

    @Test
    void test_processControlChange_missingPlayer_returnsError() {
        var event = controlChangeEvent();
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
        var event = controlChangeEvent();
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_rightControl_returnsPositiveX() {
        var event = controlChangeEvent(PlayerControl.RIGHT);
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() > 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_leftControl_returnsNegativeX() {
        var event = controlChangeEvent(PlayerControl.LEFT);
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_downControl_returnsZero() {
        var event = controlChangeEvent(PlayerControl.DOWN);
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_upControlOnGround_returnsNegativeY() {
        var event = controlChangeEvent(PlayerControl.UP);
        var velocity = processControlWithValidPlayer(event);
        assertEquals(0, velocity.xVelocity());
        assertTrue(velocity.yVelocity() < 0);
    }

    @Test
    void test_processControlChange_upControlFallingSlowly_returnsNegative() {
        var event = controlChangeEvent(PlayerControl.UP);
        PlayerStore player1 = PlayerStore.builder()
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
        var event = controlChangeEvent(PlayerControl.UP);
        PlayerStore player1 = PlayerStore.builder()
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
    test_processControlChange_multipleControls_returnsCorrectVelocity() {
        var event = controlChangeEvent(
            PlayerControl.LEFT,
            PlayerControl.DOWN,
            PlayerControl.LEFT
        );
        var velocity = processControlWithValidPlayer(event);
        assertTrue(velocity.xVelocity() < 0);
        assertEquals(0, velocity.yVelocity());
    }

    @Test
    void test_processControlChange_movingPlayer_maintainsMotion() {
        var event = controlChangeEvent(PlayerControl.LEFT);
        PlayerStore player1 = PlayerStore.builder()
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
    void test_processJoin_firstPlayer_returnsCreateFirstPlayerWithCorrectPlayer() {
        var event = joinEvent("player1");
        var createUpdate =
            (CreateFirstPlayer) processJoin("client1", event, new GameStore());
        assertEquals("client1", createUpdate.client());
        assertEquals("player1", createUpdate.player().name());
    }

    @Test
    void test_processJoin_firstPlayer_returnsMultiplePlatforms() {
        var event = joinEvent("player1");
        var createUpdate =
            (CreateFirstPlayer) processJoin("client1", event, new GameStore());
        assertTrue(createUpdate.platforms().size() > 1);
    }

    @Test
    void test_processJoin_manyPlayers_returnsCreatePlayerWithCorrectName() {
        var event = joinEvent("unique player");

        Map<String, PlayerStore> players = new HashMap<>();
        // create one less than the limit, so there's room for one more player
        for (int i = 0; i < PlayerEventHandler.MAX_PLAYER_COUNT - 1; i++) {
            var stringI = Integer.toString(i);
            players.put(stringI, PlayerStore.createRandomPlayer(stringI));
        }

        var store = GameStore.builder().players(players).build();
        var createUpdate =
            (CreatePlayer) processJoin("unique client", event, store);
        assertEquals("unique client", createUpdate.client());
        assertEquals("unique player", createUpdate.player().name());
    }

    @Test
    void test_processJoin_maxPlayers_returnsError() {
        var event = joinEvent("");

        Map<String, PlayerStore> players = new HashMap<>();
        for (int i = 0; i < PlayerEventHandler.MAX_PLAYER_COUNT; i++) {
            var stringI = Integer.toString(i);
            players.put(stringI, PlayerStore.createRandomPlayer(stringI));
        }

        var store = GameStore.builder().players(players).build();
        var update = processJoin("", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_duplicateClient_returnsError() {
        var event = joinEvent("");
        var player = PlayerStore.createRandomPlayer("duplicate_client");
        var store = testStateWithPlayer(player);

        var update = processJoin("duplicate_client", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_duplicatePlayer_returnsError() {
        var event = joinEvent("duplicate_player");
        var player = PlayerStore.createRandomPlayer("duplicate_player");
        var store = testStateWithPlayer(player);

        var update = processJoin("", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_sameNameCapitalizedDifferently_returnsError() {
        var event = joinEvent("abcd");
        var player = PlayerStore.createRandomPlayer("AbCd");
        var store = testStateWithPlayer(player);

        var update = processJoin("", event, store);
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_emptyName_returnsError() {
        var event = joinEvent("");
        var update = processJoin("client", event, new GameStore());
        assertTrue(update instanceof ErrorUpdate);
    }

    @Test
    void test_processJoin_nameTooLong_returnsError() {
        String longName = "*".repeat(MAX_NAME_LENGTH + 1);
        var event = joinEvent(longName);

        var update = processJoin("", event, new GameStore());
        assertTrue(update instanceof ErrorUpdate);
    }

    void assertAbovePlatform(PlayerStore player, GamePlatform platform) {
        assertEquals(
            platform.y() - PlayerStore.SPAWN_HEIGHT,
            player.yPosition(),
            "Player is above platform"
        );

        int middleOfPlatform = 
            platform.x() + ((platform.width() - PlayerStore.PLAYER_WIDTH) / 2);
        assertEquals(
            middleOfPlatform, player.xPosition(),
            "Player is in the middle of the platform"
        );
    }

    @Test
    void test_processJoin_onePlatform_spawnsPlayerAbovePlatform() {
        var platform = GamePlatform.generateAtHeight(100);
        var store = GameStore.builder()
            .platforms(List.of(platform)).build();
 
        var update = processJoin("", joinEvent("~"), store);
        var player = ((CreateFirstPlayer) update).player();

        assertAbovePlatform(player, platform);
    }

    @Test
    void test_processJoin_manyPlatforms_choosesCentermostPlatform() {
        int middleHeight = GameConstants.HEIGHT / 2;
        int buffer = new Random().nextInt(-50, 50);

        var goalPlatform = GamePlatform.generateAtHeight(middleHeight + buffer);
        var platforms = List.of(
            GamePlatform.generateAtHeight(middleHeight - 223),
            goalPlatform,
            GamePlatform.generateAtHeight(middleHeight + 329)
        );
        var store = new GameStore().platforms(platforms);

        var update = processJoin("", joinEvent("~"), store);
        var player = ((CreateFirstPlayer) update).player();

        assertAbovePlatform(player, goalPlatform);
    }

    private GameStore testStateWithPlayer(PlayerStore player) {
        Map<String, PlayerStore> players = new HashMap<>();
        players.put(player.name(), player);

        return GameStore.builder()
            .players(players)
            .build();
    }

    /** Shorthand */
    private JoinEvent joinEvent(String name) {
        return JoinEvent.newBuilder().setName(name).build();
    }

    /** Shorthand */
    private ControlChangeEvent controlChangeEvent(PlayerControl... controls) {
        var builder = ControlChangeEvent.newBuilder();
        for (var control : controls) {
            builder.addPressedControls(control);
        }
        return builder.build();
    }

    /**
     * Wrapper for calling processControlChange with a valid player session
     */
    private UpdateVelocity
    processControlWithValidPlayer(ControlChangeEvent event) {
        var state = GameStore.builder()
            .players(Map.of("client1", PlayerStore.createRandomPlayer("player1")))
            .build();
        return (UpdateVelocity) processControlChange("client1", event, state);
    }
}
