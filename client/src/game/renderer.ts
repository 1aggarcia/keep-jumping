import { renderButtons } from "../canvas/button";
import { renderLabel } from "../canvas/label";
import { AppState } from "../state/appState";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";
import { Context2D, GameUpdate, PlayerState } from "@lib/types";

const RED_HEX = "#ff0000";
const GREY_HEX = "#585858";

/**
 * Does NOT mutate the app state.
 *
 * Renders the game to the canvas based on the current app state
 * and game update passed in.
 * @param state app state including the context to draw to
 * @param game game state received from the server
 */
export function renderGame(state: AppState, game: GameUpdate) {
    const { context, buttons } = state;

    clearCanvas(context);
    game.players.forEach(renderPlayer(context));
    renderLabel(context, {
        text: `Time: ${game.serverAge}`,
        x: 10,
        y: 20,
        font: "15px Arial",
    });
    renderButtons(context, buttons);
    renderMetadata(state);
}

// TODO: change server to send game over update, then change this to use the
// `renderLabel` helper
export function renderGameOver(context: Context2D, reason: string) {
    context.fillStyle = RED_HEX;
    context.font = "bold 30px Arial";
    context.textAlign = "center";
    context.fillText(`GAME OVER: ${reason}`, GAME_WIDTH / 2, GAME_HEIGHT / 2);
}

export function clearCanvas(context: Context2D) {
    context.clearRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
}

export function renderMetadata(state: AppState) {
    renderLabel(state.context, {
        text: `Mode: ${import.meta.env.MODE} | v${VERSION}`,
        x: GAME_WIDTH / 2,
        y: GAME_HEIGHT - 10,
        textAlign: "center",
        font: "10px Arial",
        color: GREY_HEX,
    });
    renderLabel(state.context, {
        text: "Connection Status: " + state.connectedStatus,
        x: GAME_WIDTH / 2,
        y: 10,
        textAlign: "center",
        font: "12px Arial"
    });
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
