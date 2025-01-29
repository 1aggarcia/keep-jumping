import jQuery from "jquery";
import { GAME_HEIGHT, GAME_WIDTH } from "./ui/gameConstants";
import { fitCanvasToWindow, gameElements, getGameContext } from "./ui/dom";
import { handleJoinSubmit, handleKeyDown, handleKeyUp } from "./domHandler";
import { drawMetadata } from "./ui/graphics";
import { enableDevTools } from "./devTools";
import { AppState } from "./types";

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

    fitCanvasToWindow(gameElements.canvas);
    drawMetadata(appState);

    // Event listeners
    addEventListener("resize", () => fitCanvasToWindow(gameElements.canvas));
    addEventListener("keyup", (e) => handleKeyUp(e.code, appState));
    addEventListener("keydown", (e) => handleKeyDown(e.code, appState));

    gameElements.joinForm.on("submit", (e) => handleJoinSubmit(e, appState));
    gameElements.canvas
        .on("mousedown",() => handleKeyDown(JUMP_KEYCODE, appState))
        .on("mouseup", () => handleKeyUp(JUMP_KEYCODE, appState));

    enableDevTools(appState);
});
