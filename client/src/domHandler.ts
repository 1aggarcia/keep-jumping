import { AppState } from "./types";
import { PlayerControl } from "./generated/socketMessage";
import { connectToServer, sendToServer } from "./server";

export function handleKeyDown(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;
    if (state.server === null) return;

    // if no changes made, data should not be sent to the server
    if (state.pressedControls.has(control)) return;

    state.pressedControls.add(control);
    sendToServer(state, {
        controlChangeEvent: {
            pressedControls: Array.from(state.pressedControls),
        },
    });
}

export function handleKeyUp(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;
    if (state.server === null) return;

    state.pressedControls.delete(control);
    sendToServer(state, {
        controlChangeEvent: {
            pressedControls: Array.from(state.pressedControls),
        }
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
