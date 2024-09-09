import t from "tap";
import { LocalGameRepository } from "./localGameRepository";
import { nextGameTick, runGameLoop } from "./gameLoop";
import { SERVER_TIME_LIMIT, TICK_DELAY_MS } from "./constants";
import { randomInt } from "../util/random";
import { ClientBridge, clientBridge } from "../clients/clientBridge";

const TICKS_PER_SECOND = 1000 / TICK_DELAY_MS;

t.test("runGameLoop resolves once isRunning = false", t => {
    const game = new LocalGameRepository();
    runGameLoop(game, clientBridge).then(() => {
       t.equal(game.age, 0);
       t.end();
    });
    game.isRunning = false;
});

t.test("runGameLoop resolves if game age is at limit", t => {
    const game = new LocalGameRepository();
    runGameLoop(game, clientBridge).then(() => {
        t.equal(game.isRunning, false);
        t.end();
    });
    game.age = SERVER_TIME_LIMIT;
});

t.test("runGameLoop resets game age on start", t => {
    const game = new LocalGameRepository();
    game.age = 10;
    runGameLoop(game, clientBridge).then(() => {
        t.equal(game.age, 0);
        t.end();
    });
    game.isRunning = false;
});

t.test("runGameLoop broadcasts game over to clients", t => {
    let message: unknown;
    const mockClientBridge: ClientBridge = {
        ...clientBridge,
        broadcast: (jsonMessage) => {
            message = jsonMessage;
        }
    };
    const game = new LocalGameRepository();
    runGameLoop(game, mockClientBridge).then(() => {
        t.strictSame(message, {
            type: "gameOverUpdate",
            reason: "Time Limit Reached",
        });
        t.end();
    });
    game.isRunning = false;
});

t.test("nextGameTick increments age when tickCount is correct factor", t => {
    const game = new LocalGameRepository();
    nextGameTick(game, TICKS_PER_SECOND - 1);
    t.equal(game.age, 1);
    nextGameTick(game, TICKS_PER_SECOND - 1);
    t.equal(game.age, 2);
    t.end();
});

t.test(
    "nextGameTick doesn't change age when tickCount is incorrect factor",
    t => {
        const game = new LocalGameRepository();
        nextGameTick(game, 0);
        t.equal(game.age, 0);
        nextGameTick(game, TICKS_PER_SECOND - 2);
        t.equal(game.age, 0);
        t.end();
    }
);

t.test("nextGameTick moves players", t => {
    const game = new LocalGameRepository();
    const player1 = game.createPlayer("test1");
    const player2 = game.createPlayer("test2");

    player1.position = [50, 50];
    player2.position = [50, 50];

    player1.velocity = [randomInt(10), randomInt(10)];
    player2.velocity = [randomInt(10), randomInt(10)];

    nextGameTick(game, 0);

    t.strictSame(player1.position, [
        50 + player1.velocity[0],
        50 + player1.velocity[1]
    ]);
    t.strictSame(player2.position, [
        50 + player2.velocity[0],
        50 + player2.velocity[1]
    ]);

    t.end();
});
