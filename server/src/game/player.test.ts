import t from "tap";
import {
    getPlayerVelocity,
    movePlayer,
    Player,
    PLAYER_SPEED,
    playerToPlayerState
} from "./player";
import { GAME_HEIGHT, GAME_WIDTH } from "./constants";
import { randomInt } from "../util/random";

t.test("getPlayerVelocity finds correct velocity for one input", t => {
    t.strictSame(getPlayerVelocity(["Right"]), [PLAYER_SPEED, 0]);
    t.strictSame(getPlayerVelocity(["Left"]), [-PLAYER_SPEED, 0]);
    t.strictSame(getPlayerVelocity(["Down"]), [0, PLAYER_SPEED]);
    t.strictSame(getPlayerVelocity(["Up"]), [0, -PLAYER_SPEED]);
    t.end();
});

t.test("getPlayerVelocity finds correct velocity for two inputs", t => {
    t.strictSame(
        getPlayerVelocity(["Right", "Up"]), [PLAYER_SPEED, -PLAYER_SPEED]);
    t.strictSame(
        getPlayerVelocity(["Left", "Down"]), [-PLAYER_SPEED, PLAYER_SPEED]);
    t.end();
});

t.test("getPlayerVelocity ignores duplicates", t => {
    t.strictSame(getPlayerVelocity(
        ["Up", "Up", "Left"]),
        [-PLAYER_SPEED, -PLAYER_SPEED]
    );
    t.end();
});

t.test("movePlayer makes correct changes to position", t => {
    const testPlayer: Player = {
        id: "",
        color: "",
        age: 0,
        position: [15, 99],
        velocity: [2, -4],
    };
    movePlayer(testPlayer);
    t.strictSame(testPlayer.position, [17, 95]);
    t.end();
});

t.test("movePlayer ignores changes to position less than 0", t => {
    const testPlayer: Player = {
        id: "",
        color: "",
        age: 0,
        position: [0, 0],
        velocity: [-1, 1],
    };
    movePlayer(testPlayer);
    t.strictSame(testPlayer.position, [0, 1]);

    testPlayer.position = [0, 0];
    testPlayer.velocity = [1, -1];
    movePlayer(testPlayer);
    t.strictSame(testPlayer.position, [1, 0]);
    t.end();
});

t.test("movePlayer ignores changes to position above game boundary", t => {
    const testPlayer: Player = {
        id: "",
        color: "",
        age: 0,
        position: [GAME_WIDTH, 0],
        velocity: [1, 1],
    };
    movePlayer(testPlayer);
    t.strictSame(testPlayer.position, [GAME_WIDTH, 1]);

    testPlayer.position = [0, GAME_HEIGHT];
    movePlayer(testPlayer);
    t.strictSame(testPlayer.position, [1, GAME_HEIGHT]);
    t.end();
});

t.test("playerToPlayerState removes velocity param", t => {
    const player: Player = {
        id: "testPlayer",
        color: "#123456",
        position: [randomInt(100), randomInt(100)],
        velocity: [randomInt(100), randomInt(100)],
        age: randomInt(100)
    };
    t.strictSame(playerToPlayerState(player), {
        id: "testPlayer",
        color: "#123456",
        position: player.position,
        age: player.age,
    });
    t.end();
});
