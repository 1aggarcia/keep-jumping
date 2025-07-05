import { AppState, StateSnapshot } from "./types";
import { PlayerControl } from "./generated/socketMessage";
import {
    addErrorNotification,
    connectToServer,
    sendToServerImpure,
} from "./server";
import { Effect } from "./effects";

const MAX_NAME_LENGTH = 25;

export function handleKeyDown(keyCode: string, state: AppState) {
    const control = keyCodeToPlayerControl(keyCode);
    if (control === null) return;

    const didStateChange = !state.pressedControls.has(control);
    state.pressedControls.add(control);
    if (!didStateChange || state.server === null) return;

    sendToServerImpure(state, {
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

    sendToServerImpure(state, {
        controlChangeEvent: {
            pressedControls: Array.from(state.pressedControls),
        },
    });
}

export function
onJoinSubmit(event: JQuery.SubmitEvent, state: StateSnapshot): Effect[] {
    const name = new FormData(event.target).get("name")?.toString();
    if (name === undefined || name.length === 0) {
        return [];
    }
    if (name.length > MAX_NAME_LENGTH) {
        return addErrorNotification(state, "Username is too long");
    }
    return connectToServer(name, state);
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
