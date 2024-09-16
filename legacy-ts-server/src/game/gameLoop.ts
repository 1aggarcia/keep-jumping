import { GameRepository } from "./gameRepository";
import { movePlayer } from "./player";
import { SERVER_TIME_LIMIT, TICK_DELAY_MS } from "./constants";
import { ClientBridge } from "../clients/clientBridge";
import { sleep } from "../util/time";
import { GameOverUpdate } from "@lib/types";

const TICKS_PER_SECOND = 1000 / TICK_DELAY_MS;

/**
 * Async function starting the game loop, and sending state updates to clients
 * whenever it is necessary. The loop continues running until
 * `game.isRunning = false`
 * @param game should have state `game.isRunning = false`
 * @param clientBridge
 * @returns Promise resolving once the game loop is finished
 */
export async function runGameLoop(
    game: GameRepository, clientBridge: ClientBridge
) {
    let tickCount = 0;
    game.age = 0;
    game.isRunning = true;
    console.log("starting game loop");

    while (game.isRunning && game.age < SERVER_TIME_LIMIT) {
        const isUpdatedNeeded = nextGameTick(game, tickCount);
        if (isUpdatedNeeded) {
            clientBridge.broadcast(game.getUpdate());
        }
        tickCount++;
        if (tickCount >= TICKS_PER_SECOND) {
            tickCount = 0;
        }
        await sleep(TICK_DELAY_MS);
    }
    game.isRunning = false;
    clientBridge.broadcast<GameOverUpdate>({
        type: "gameOverUpdate",
        reason: "Time Limit Reached"
    });
    console.log("game loop closed");
}

/**
 * Function which runs once per game tick on the game state. Moves all players
 * and increments the game age, if needed.
 * @param game state of game
 * @param tickCount number of ticks since the last second has passed
 * @returns `true` if a new updates needs to be sent to clients, `false` o/w
 */
export function nextGameTick(game: GameRepository, tickCount: number): boolean {
    if (tickCount + 1 >= TICKS_PER_SECOND) {
        game.age++;
    }
    game.getAllPlayers().forEach(player => {
        movePlayer(player);
    });
    return true;
}
