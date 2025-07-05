import jQuery from "jquery";
import { GAME_HEIGHT, GAME_WIDTH } from "./ui/gameConstants";
import {
    buildLeaderboardRows,
    fitCanvasToWindow,
    gameElements,
    getGameContext,
} from "./ui/dom";
import { handleKeyDown, handleKeyUp, onJoinSubmit } from "./domHandler";
import { drawMetadataImpure } from "./ui/graphics";
import { enableDevTools } from "./devTools";
import { AppState } from "./types";
import { updateLeaderboard, checkServerHealth } from "./server";
import { applyEffects } from "./effects";

const JUMP_KEYCODE = "ArrowUp";

jQuery(function main() {
    const appState: AppState = {
        server: null,
        connectedStatus: "CLOSED",
        pressedControls: new Set(),
        lastPing: null,
        errors: [],
        context: getGameContext(),
        buttons: [],
        serverId: null,
        gameOverMessage: null,

        bytesIn: 0,
        messagesIn: 0,
        messagesOut: 0,
    };

    // setting size with CSS distorts the canvas,
    // it must be done with the DOM attributes
    gameElements.canvas
        .attr("width", GAME_WIDTH)
        .attr("height", GAME_HEIGHT);

    gameElements.viteMode
        .text(`Mode: ${import.meta.env.MODE} | v${VERSION}`);

    const leaderboardRows = buildLeaderboardRows();
    gameElements.leaderboardBody.append(leaderboardRows);
    fitCanvasToWindow(gameElements.canvas);
    drawMetadataImpure(appState);

    // Event listeners
    addEventListener("resize", () => fitCanvasToWindow(gameElements.canvas));
    addEventListener("keyup", (e) => handleKeyUp(e.code, appState));
    addEventListener("keydown", (e) => handleKeyDown(e.code, appState));

    gameElements.joinForm.on("submit", (e) => {
        const effects = onJoinSubmit(e, appState);
        applyEffects(effects, appState);
    });
    gameElements.canvas
        .on("mousedown",() => handleKeyDown(JUMP_KEYCODE, appState))
        .on("mouseup", () => handleKeyUp(JUMP_KEYCODE, appState));

    enableDevTools(appState);
    checkServerHealth()
        .then(updateLeaderboard)
        .catch(displayServerUnavailable);
});

function displayServerUnavailable(connectionError: unknown) {
    console.error(connectionError);

    gameElements.joinForm.hide();
    gameElements.leaderboard.hide();
    gameElements.leaderboardStatus.hide();
    gameElements.serverUnavailableBox.show();
}
