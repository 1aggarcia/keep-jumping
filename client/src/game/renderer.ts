import { Context2D } from "../canvas/types";
import { GameUpdate } from "./types/messages";
import { GamePlatform, PlayerState } from "./types/models";
import { renderButtons } from "../canvas/button";
import { renderLabel } from "../canvas/label";
import { AppState } from "../state/appState";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";

const RED_HEX = "#ff0000";

const PLATFORM_HEIGHT = 30;
const PLATFORM_COLOR = "green";

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
    game.platforms.forEach(platform => renderPlatform(context, platform));
    game.players.forEach(player => renderPlayer(context, player));
    renderLabel(context, {
        text: `Time: ${game.serverAge}`,
        x: 10,
        y: 20,
        font: "30px Arial",

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
    // this wacky syntax is to mimic a switch expression in JS
    const connectionLabel = (() => {
        switch (state.connectedStatus) {
        case "OPEN":
            return "Connection Open";
        case "CONNECTING":
            return "Connecting... (this might take a few seconds)";
        default:
            return "Connection Status: " + state.connectedStatus;
    }})();
    renderLabel(state.context, {
        text: connectionLabel,
        x: GAME_WIDTH / 2,
        y: 20,
        textAlign: "center",
        font: "25px Arial"
    });
}

function renderPlatform(context: Context2D, platform: GamePlatform) {
    const { x, y, width } = platform;
    if (x > GAME_WIDTH || y > GAME_HEIGHT) {
        console.error(`Sprite position out of bounds: (${x}, ${y})`);
        return;
    }
    context.fillStyle = PLATFORM_COLOR;
    context.fillRect(x, y, width, PLATFORM_HEIGHT);
}


function renderPlayer(context: Context2D, player: PlayerState) {
    const { x, y } = player;
    if (x > GAME_WIDTH || y > GAME_HEIGHT) {
        console.error(`Sprite position out of bounds: (${x}, ${y})`);
        return;
    }
    renderLabel(context, {
        text: `${player.name}: ${player.score}`,
        x: player.x + (PLAYER_WIDTH / 2),
        y: player.y - 15,
        color: player.color,
        textAlign: "center",
        font: "bold 22px Arial",
    });
    context.fillStyle = player.color;
    context.fillRect(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
};
