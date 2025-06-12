import { AppState } from "./types";
import { PlayerControl } from "./generated/socketMessage";
import { connectToServer, sendToServer } from "./server";

export function handleKeyDown(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;

    const didStateChange = !state.pressedControls.has(control);
    state.pressedControls.add(control);
    if (!didStateChange || state.server === null) return;

    sendToServer(state, {
        controlChangeEvent: {
            pressedControls: Array.from(state.pressedControls),
        },
    });
}

export function handleKeyUp(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;

    const didStateChange = state.pressedControls.has(control);
    state.pressedControls.delete(control);
    if (!didStateChange || state.server === null) return;

    sendToServer(state, {
        controlChangeEvent: {
            pressedControls: Array.from(state.pressedControls),
        },
    });
}

export function handleJoinSubmit(event: JQuery.SubmitEvent, state: AppState) {
    const name = new FormData(event.target).get("name");
    if (name === null || name.toString().length === 0) {
        return;
    }
    connectToServer(state, String(name));
}

function keyCodeToPlayerControl(code: string): PlayerControl | null {
    switch(code) {
        case "ArrowLeft":
        case "KeyA":
            return PlayerControl.LEFT;
        case "ArrowUp":
        case "KeyW":
            return PlayerControl.UP;
        case "ArrowRight":
        case "KeyD":
            return PlayerControl.RIGHT;
        case "ArrowDown":
        case "KeyS":
            return PlayerControl.DOWN;
        default:
           return null;
    }
}
