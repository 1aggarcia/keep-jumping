import $ from "jquery";
import { GAME_HEIGHT, GAME_WIDTH } from "./gameConstants";
import { AppState, LeaderboardEntry } from "../types";
import { formatBytesString } from "./formatters";

const GAME_ASPECT_RATIO = GAME_WIDTH / GAME_HEIGHT;
const LEADERBOARD_ROWS = 10;

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
    leaderboardBody: $("#leaderboard tbody"),
    leaderboardStatus: $("#leaderboard-status"),
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

export function buildLeaderboardRows() {
    gameElements.leaderboardBody.empty();

    for (let i = 1; i <= LEADERBOARD_ROWS; i++) {
        const row = $("<tr>");
        const rank = $("<td>").text(i);
        row.append(rank)
            .append("<td>")
            .append("<td>")
            .append("<td>");

        gameElements.leaderboardBody.append(row);
    }
}

export function fillLeaderboard(entries: LeaderboardEntry[]) {
    const entryIterator = entries.values();

    for (const row of gameElements.leaderboardBody.children("tr")) {
        const [_,
            playerCell, scoreCell, timestampCell] = row.querySelectorAll("td");

        const nextEntry = entryIterator.next().value;
        if (nextEntry !== undefined) {
            playerCell.textContent = nextEntry.player;
            scoreCell.textContent = String(nextEntry.score);
            timestampCell.textContent =
                new Date(nextEntry.timestamp).toLocaleDateString();
        } else {
            playerCell.textContent = "";
            scoreCell.textContent = "";
            timestampCell.textContent = "";
        }
    }
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
