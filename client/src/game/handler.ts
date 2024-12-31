import { ControlChangeEvent } from "./types/messages";
import { AppState } from "../state/appState";
import { sendToServer } from "../networking/handler";
import { GAME_HEIGHT, GAME_WIDTH } from "./constants";
import { PlayerControl } from "./types/models";

const GAME_ASPECT_RATIO = GAME_WIDTH / GAME_HEIGHT;

export function handleKeyDown(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;
    if (state.server === null) return;

    // if no changes made, data should not be sent to the server
    if (state.pressedControls.has(control)) return;

    state.pressedControls.add(control);
    sendToServer<ControlChangeEvent>(state, {
        type: "ControlChangeEvent",
        pressedControls: Array.from(state.pressedControls),
    });
}

export function handleKeyUp(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;
    if (state.server === null) return;

    state.pressedControls.delete(control);
    sendToServer<ControlChangeEvent>(state, {
        type: "ControlChangeEvent",
        pressedControls: Array.from(state.pressedControls),
    });
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
