import { Effect } from "./effects";
import { sendToServer } from "./server";
import { StateSnapshot } from "./types";

/**
 * Sends the pressed controls to the server, if needed, on a scheduled interval
 * @returns
 *  effects to send controls to the server and cache the pressed controls
 */
export function onClientTick(state: StateSnapshot): Effect[] {
    if (!shouldSendControlsToServer(state)) {
        return [];
    }
    return [
        { action: "cachePressedControls" },
        ...sendToServer({
            controlChangeEvent: {
                pressedControls: Array.from(state.pressedControls)
            }
        }, state)
    ];
}

/**
 * Determines if there was a change in the pressed controls since the last tick.
 * Checks that the server is open as a precondition.
 */
function shouldSendControlsToServer(state: StateSnapshot): boolean {
    if (state.server === null) {
        return false;
    }
    if (state.cachedControls.size !== state.pressedControls.size) {
        return true;
    }
    for (const control of state.cachedControls) {
        if (!state.pressedControls.has(control)) {
            return true;
        }
    }
    return false;
}
