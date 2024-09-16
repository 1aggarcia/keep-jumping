import t from "tap";
import { LocalGameRepository } from "./localGameRepository";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";
import { playerToPlayerState } from "./player";

t.test("localGameRepository starts with empty state", t => {
    const game = new LocalGameRepository();
    t.strictSame(game.getAllPlayersState(), []);
    t.end();
});

t.test("createPlayer creates player entry", t => {
    const game = new LocalGameRepository();
    const player = game.createPlayer("");
    t.matchStrict(game.getAllPlayers(), [player]);
    t.end();
});

t.test("createPlayer creates player with position in game bounds", t => {
    const player = new LocalGameRepository().createPlayer("");
    t.equal(true, player.position[0] >= 0);
    t.equal(true, player.position[1] >= 0);
    t.equal(true, player.position[0] + PLAYER_WIDTH <= GAME_WIDTH);
    t.equal(true, player.position[1] + PLAYER_HEIGHT <= GAME_HEIGHT);
    t.end();
});

t.test("createPlayer creates player with zeroed stats", t => {
    const player = new LocalGameRepository().createPlayer("");
    t.equal(player.age, 0);
    t.strictSame(player.velocity, [0, 0]);
    t.end();
});

t.test("createPlayer throws error when same ID is used twice", t => {
    const game = new LocalGameRepository();
    game.createPlayer("test");
    t.throws(() => game.createPlayer("test"));
    t.end();
});

t.test("deletePlayer deletes player entry", t => {
    const game = new LocalGameRepository();
    game.createPlayer("");
    t.equal(game.deletePlayer(""), true);
    t.strictSame(game.getAllPlayersState(), []);
    t.end();
});

t.test("getPlayer returns null for invalid player", t => {
    t.equal(new LocalGameRepository().getPlayer("non existent"), null);
    t.end();
});

t.test("getPlayer retrives player based on id", t => {
    const game = new LocalGameRepository();
    const player = game.createPlayer("testId");
    t.strictSame(game.getPlayer("testId"), player);
    t.end();
});

t.test("repo starts with correct stats", t => {
    const game = new LocalGameRepository();
    t.equal(game.age, 0);
    t.equal(game.isRunning, false);
    t.end();
});

t.test("getAllPlayerState returns list of players without velocity", t => {
    const game = new LocalGameRepository();
    const player1 = playerToPlayerState(game.createPlayer("player1"));
    const player2 = playerToPlayerState(game.createPlayer("player2"));

    const result = game.getAllPlayersState();
    t.strictSame(result, [player1, player2]);
    for (const playerState of result) {
        t.equal("velocity" in playerState, false);
    }
    t.end();
});

t.test("getAllPlayers returns list of players", t => {
    const game = new LocalGameRepository();
    const player1 = game.createPlayer("player1");
    const player2 = game.createPlayer("player2");
    t.strictSame(game.getAllPlayers(), [player1, player2]);
    t.end();
});

t.test("getAllPlayers returns mutable player references", t => {
    const game = new LocalGameRepository();
    const player = game.createPlayer("");
    t.equal(player.age, 0);
    game.getAllPlayers().forEach(player => {
        player.age = 55;
    });
    t.equal(player.age, 55);
    t.end();
});

t.test("getUpdate returns correct state", t => {
    const game = new LocalGameRepository();
    const player1 = playerToPlayerState(game.createPlayer("player1"));
    const player2 = playerToPlayerState(game.createPlayer("player2"));
    t.strictSame(game.getUpdate(), {
        type: "gameUpdate",
        serverAge: 0,
        players: [player1, player2]
    });
    t.end();
});
