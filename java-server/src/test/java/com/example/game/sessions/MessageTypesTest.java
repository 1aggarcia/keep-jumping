package com.example.game.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.game.players.Player;
import com.example.game.players.PlayerState;
import com.example.game.sessions.MessageTypes.GameUpdate;
import com.example.game.sessions.MessageTypes.SocketMessageType;

@SpringBootTest
public class MessageTypesTest {
    @Test
    void test_GameUpdatefromPlayerState_multiplePlayers_makesCorrectMessage() {
        Player player1 = Player.createRandomPlayer();
        Player player2 = Player.createRandomPlayer();
        Map<String, Player> players = Map.of(
            "1", player1,
            "2", player2
        );

        GameUpdate update = GameUpdate.fromPlayerState(players);
        assertEquals(SocketMessageType.GAME_UPDATE, update.type());
        assertEquals(update.players().size(), 2);

        PlayerState playerStateA = update.players().get(0);
        PlayerState playerStateB = update.players().get(1);

        // using XOR ^ to assert that exactly one condition is true, not both
        assertTrue(
            player1.color() == playerStateA.color()
            ^ player1.color() == playerStateB.color()
        );
        assertTrue(
            player2.color() == playerStateA.color()
            ^ player2.color() == playerStateB.color()
        );
    }
}
