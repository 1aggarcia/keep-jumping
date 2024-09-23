import { PlayerControl, PlayerControlUpdate, SocketMessage } from "@lib/types";
import { AppState } from "../state/appState";
import { renderGame, renderGameOver } from "./renderer";
import { sendToServer } from "../networking/handler";
import { GAME_HEIGHT, GAME_WIDTH } from "./constants";

const GAME_ASPECT_RATIO = GAME_WIDTH / GAME_HEIGHT;

export function handleGameUpdate(message: string, state: AppState) {
    const json: SocketMessage = JSON.parse(message);
    if (json.type === "gameUpdate") {
        renderGame(state.context, state.buttons, json);
    }
    if (json.type === "gameJoinUpdate") {
        renderGame(state.context, state.buttons, {
            type: "gameUpdate",
            serverAge: json.serverAge,
            players: json.players
        });
    }
    if (json.type === "gameOverUpdate") {
        renderGameOver(state.context, json.reason);
    }
}

export function handleKeyDown(event: KeyboardEvent, state: AppState) {
    const control = keyCodeToPlayerControl(event.code);
    if (control === null) return;

    // if no changes made, data should not be sent to the server
    if (state.pressedControls.has(control)) return;

    state.pressedControls.add(control);
    const update: PlayerControlUpdate = {
        type: "playerControlUpdate",
        pressedControls: Array.from(state.pressedControls),
    };
    sendToServer(state, JSON.stringify(update));
}

export function handleKeyUp(event: KeyboardEvent, state: AppState) {
    const control = keyCodeToPlayerControl(event.code);
    if (control === null) return;

    state.pressedControls.delete(control);
    const update: PlayerControlUpdate = {
        type: "playerControlUpdate",
        pressedControls: Array.from(state.pressedControls),
    };
    sendToServer(state, JSON.stringify(update));
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


function keyCodeToPlayerControl(code: string): PlayerControl | null {
    switch(code) {
        case "ArrowLeft":
        case "KeyA":
            return "Left";
        case "ArrowUp":
        case "KeyW":
            return "Up";
        case "ArrowRight":
        case "KeyD":
            return "Right";
        case "ArrowDown":
        case "KeyS":
            return "Down";
        default:
           return null;
    }
}
