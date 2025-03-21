import $ from "jquery";
import { GAME_HEIGHT, GAME_WIDTH } from "./gameConstants";
import { AppState } from "../types";
import { formatBytesString } from "./formatters";

const GAME_ASPECT_RATIO = GAME_WIDTH / GAME_HEIGHT;

export const gameElements = {
    canvas: $<HTMLCanvasElement>("#game-box"),
    connectedBox: $("#connected-box"),
    errorBox: $("#error-box"),
    messagesStats: $("#messages-stats"),
    messagesBox: $("#messages-box"),
    viteMode: $("#vite-mode"),
    joinForm: $<HTMLFormElement>("#join-form"),
    inactiveOverlay: $(".inactive-overlay"),
    serverUnavaliableBox: $("#server-unavaliable-box"),
    leaderboard: $("#leaderboard"),
};

const gameContext = gameElements.canvas[0].getContext("2d");

export function getGameContext() {
    if (gameContext === null) {
        throw new ReferenceError("Canvas context is null");
    }
    return gameContext;
}

/**
 * Scales the canvas passed in to the size of the screen, while keeping the
 * aspect ratio of the game
 * @param canvas should have the `width` and `height` attributes set to the
 *  game size
 */
export function fitCanvasToWindow(canvas: JQuery<HTMLCanvasElement>) {
    const screenAspectRatio = window.innerWidth / window.innerHeight;
    const isGameShorterThanScreen = GAME_ASPECT_RATIO > screenAspectRatio;

    let scaleFactor = 1;
    if (isGameShorterThanScreen) {
        scaleFactor = window.innerWidth / GAME_WIDTH;
    } else {
        scaleFactor = window.innerHeight / GAME_HEIGHT;
    }

    canvas
        .css("width", GAME_WIDTH * scaleFactor)
        .css("height", GAME_HEIGHT * scaleFactor);
}

export function renderMessageStats(state: AppState) {
    const outText = `Sent: ${state.messagesOut}`;
    const inText = `Received: ${state.messagesIn}`;
    const bytesText = `Data in: ${formatBytesString(state.bytesIn)}`;

    const meanPingSize = Math.floor(state.bytesIn / state.messagesIn);
    const meanText = `Mean ping size: ${formatBytesString(meanPingSize)}`;

    gameElements.messagesStats
        .text(outText + " | " + inText + " | " + bytesText + " | " + meanText);
}

/**
 * Enable or disable demo features depending on the boolean flag passed in
 */
export function enableDemoFeatures(args: { shouldEnable: boolean }) {
    if (args.shouldEnable) {
        gameElements.leaderboard.show();
    } else {
        gameElements.leaderboard.remove();
    }
}
