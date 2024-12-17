import { AppState } from "../state/appState";
import { GAME_HEIGHT, GAME_WIDTH } from "./constants";
import { gameElements } from "./elements";
import { fitCanvasToWindow, handleKeyDown, handleKeyUp } from "./handler";

const JUMP_KEYCODE = "ArrowUp";
const gameContext = gameElements.canvas[0].getContext("2d");

export function getGameContext() {
    if (gameContext === null) {
        throw new ReferenceError("Canvas context is null");
    }
    return gameContext;
}

export function setupGame(state: AppState) {
    // setting size with CSS distorts the canvas,
    // it must be done with the DOM attributes
    gameElements.canvas
        .attr("width", GAME_WIDTH)
        .attr("height", GAME_HEIGHT);

    fitCanvasToWindow(gameElements.canvas);
    addEventListener("resize", () => fitCanvasToWindow(gameElements.canvas));

    addEventListener("keydown", (e) => handleKeyDown(e.code, state));
    addEventListener("keyup", (e) => handleKeyUp(e.code, state));

    gameElements.canvas
        .on("mousedown",() => handleKeyDown(JUMP_KEYCODE, state))
        .on("mouseup", () => handleKeyUp(JUMP_KEYCODE, state));
}
