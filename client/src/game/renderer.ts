import { Context2D } from "../canvas/types";
import { BUTTON_HEIGHT, renderButtons } from "../canvas/button";
import { renderLabel } from "../canvas/label";
import { AppState } from "../state/appState";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";
import { GamePing, Platform, Player } from "../generated/socketMessage";

const RED_HEX = "#ff0000";

const PLATFORM_HEIGHT = 30;
const PLATFORM_COLOR = "green";
const LEADERBOARD_LINE_HEIGHT = 35;
const OFF_SCREEN_PLAYER_HEIGHT = 5;

// give players a smaller width as they go further off-screen
const offScreenWidth =
    ({ y }: Player) => Math.max(0, PLAYER_WIDTH - (Math.abs(y) / 7));

/**
 * Does NOT mutate the app state.
 *
 * Renders the game to the canvas based on the current app state
 * and game update passed in.
 * @param state app state including the context to draw to
 * @param ping game state received from the server
 */
export function renderGame(state: AppState, ping: GamePing) {
    const { context, buttons } = state;

    clearCanvas(context);
    ping.platforms.forEach(platform => renderPlatform(context, platform));
    ping.players.forEach(player => renderPlayer(context, player));
    renderLabel(context, {
        text: `Time: ${ping.serverAge}`,
        x: 10,
        y: GAME_HEIGHT - 20,
        font: "27px Arial",
    });
    renderLeaderboard(context, ping.players);
    renderButtons(context, buttons);
    renderMetadata(state);
}

export function rerender(state: AppState) {
    if (state.lastPing !== null) {
        renderGame(state, state.lastPing);
        return;
    }
    clearCanvas(state.context);
    renderButtons(state.context, state.buttons);
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
    if (state.serverId === null) {
        console.error("called renderMetadata without serverId");
    }
    // this wacky syntax is to mimic a switch expression in JS
    const connectionLabel = (() => {
        switch (state.connectedStatus) {
        case "OPEN":
            return state.serverId
                ? `Connected to server ${state.serverId}`
                : "Connected (server ID unknown)";
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

    const disconnectButtonOffset = BUTTON_HEIGHT + 10;
    state.errors.forEach((error, index) => {
        renderLabel(state.context, {
            text: error,
            x: GAME_WIDTH - 10,
            y: disconnectButtonOffset + (index * 40),
            textAlign: "right",
            textBaseline: "top",
            color: RED_HEX,
            font: "25px Arial"
        });
    });
}

function renderLeaderboard(context: Context2D, players: Player[]) {
    const sortedPlayers = [...players].sort((a, b) => b.score - a.score);

    renderLabel(context, {
        text: "Leaderboard",
        x: 10,
        y: LEADERBOARD_LINE_HEIGHT,
        font: "25px Arial",
    });
    for (let i = 1; i <= sortedPlayers.length; i++) {
        const player = sortedPlayers[i - 1];
        renderLabel(context, {
            text: `${i}. ${player.name} | ${player.score}`,
            x: 10,
            y: LEADERBOARD_LINE_HEIGHT + (i * LEADERBOARD_LINE_HEIGHT),
            font: "bold 27px Arial",
            color: player.color,
        });
    }
}

function renderPlatform(context: Context2D, platform: Platform) {
    const { x, y, width } = platform;
    if (x > GAME_WIDTH || y > GAME_HEIGHT) {
        console.error(`Sprite position out of bounds: (${x}, ${y})`);
        return;
    }
    context.fillStyle = PLATFORM_COLOR;
    context.fillRect(x, y, width, PLATFORM_HEIGHT);
}


function renderPlayer(context: Context2D, player: Player) {
    const { x, y } = player;
    if (x > GAME_WIDTH || y > GAME_HEIGHT) {
        console.error(`Sprite position out of bounds: (${x}, ${y})`);
        return;
    }
    const isPlayerVisible = player.y > 0;

    renderLabel(context, {
        text: player.name,
        x: player.x + (PLAYER_WIDTH / 2),
        y: isPlayerVisible ? player.y - 15 : 40,
        color: player.color,
        textAlign: "center",
        font: "bold 22px Arial",
    });
    context.fillStyle = player.color;
    if (isPlayerVisible) {
        context.fillRect(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
    } else {
        const playerWidth = offScreenWidth(player);
        const offsetX = x + (PLAYER_WIDTH - playerWidth) / 2;
        context.fillRect(
            offsetX,
            OFF_SCREEN_PLAYER_HEIGHT,
            playerWidth,
            OFF_SCREEN_PLAYER_HEIGHT
        );
    }
};
