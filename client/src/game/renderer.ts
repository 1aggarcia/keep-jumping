import { AppState } from "../state/appState";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";
import { Context2D, GameUpdate, PlayerState } from "@lib/types";

const BLACK_HEX = "#000000";
const RED_HEX = "#ff0000";

export function renderGame(context: Context2D, game: GameUpdate) {
    context.clearRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
    game.players.forEach(renderPlayer(context));
    context.fillStyle = BLACK_HEX;
    context.font = "15px Arial";
    context.textAlign = "left";
    context.fillText(`Time: ${game.serverAge}`, 10, 20, GAME_WIDTH);
}

export function renderGameOver(context: Context2D, reason: string) {
    context.fillStyle = RED_HEX;
    context.font = "bold 30px Arial";
    context.textAlign = "center";
    context.fillText(`GAME OVER: ${reason}`, GAME_WIDTH / 2, GAME_HEIGHT / 2);
}

export function clearGame(state: AppState) {
    state?.context?.clearRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
}

// who needs loops when you have currying
const renderPlayer = (context: Context2D) => (player: PlayerState) => {
    const { x, y } = player;
    if (x > GAME_WIDTH || y > GAME_HEIGHT) {
        throw new RangeError(`Sprite position out of bounds: (${x}, ${y})`);
    }
    context.fillStyle = player.color;
    context.fillRect(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
};
