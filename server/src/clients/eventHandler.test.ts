import t from "tap";
import {
    handleClientDisconnect,
    handleClientMessage,
    playerUpdateResponse
} from "./eventHandler";
import { LocalGameRepository } from "../game/localGameRepository";
import { PlayerControlUpdate } from "@lib/types";
import { playerToPlayerState } from "../game/player";

// shorthand
function newRepo() {
    const game = new LocalGameRepository();
    game.isRunning = true;
    return game;
};

t.test("clientMessageBrodcastResponse returns error for unknown type", t => {
    const response = handleClientMessage(
        "", Buffer.from("{}"), newRepo());
    t.strictSame(response, {
        type: "serverError",
        message: "Unsupported message type: undefined",
    });
    t.end();
});

t.test("clientMessageBrodcastResponse returns error for bad message", t => {
    const response = handleClientMessage(
        "", Buffer.from("just some string"), newRepo());
    t.equal(response.type, "serverError");
    t.end();
});

t.test("playerUpdateResponse returns game update", t => {
    const game = newRepo();
    const player = playerToPlayerState(game.createPlayer("testId"));
    const update: PlayerControlUpdate = {
        type: "playerControlUpdate",
        pressedControls: []
    };

    t.strictSame(playerUpdateResponse("testId", update, game), {
        type: "gameUpdate",
        serverAge: game.age,
        players: [player],
    });
    t.end();
});

t.test("playerUpdateResponse returns early if game is not running", t => {
    const game = newRepo();
    const update: PlayerControlUpdate = {
        type: "playerControlUpdate",
        pressedControls: []
    };
    game.isRunning = false;
    const response = playerUpdateResponse("", update, game);
    t.strictSame(response.type, "serverError");
    t.end();
});

t.test("handleClientDisconnect deletes player from game repo", t => {
    const game = newRepo();
    game.createPlayer("testId");
    t.not(game.getPlayer("testId"), null);
    handleClientDisconnect("testId", game);
    t.equal(game.getPlayer("testId"), null);
    t.end();
});

t.test("handleClientDisconnect returns updated game without player", t => {
    const game = newRepo();
    game.createPlayer("testId");
    const response = handleClientDisconnect("testId", game);
    t.strictSame(response, {
        type: "gameUpdate",
        serverAge: game.age,
        players: []
    });
    t.end();
});

t.test(
    "handleClientDisconnect changes game loop state when last player leaves",
    t => {
        const game = newRepo();
        game.createPlayer("test");
        game.isRunning = true;
        handleClientDisconnect("test", game);
        t.equal(game.isRunning, false);
        t.end();
    }
);
