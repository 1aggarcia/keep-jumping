import { PlayerControl, PlayerControlUpdate, SocketMessage } from "@lib/types";
import { AppState } from "../state/appState";
import { renderGame, renderGameOver } from "./renderer";

export function handleGameUpdate(message: string, state: AppState) {
    if (state.context === null) return;

    const json: SocketMessage = JSON.parse(message);
    if (json.type === "gameUpdate") {
        renderGame(state.context, json);
    }
    if (json.type === "gameJoinUpdate") {
        renderGame(state.context, {
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

    state.pressedControls.add(control);
    const update: PlayerControlUpdate = {
        type: "playerControlUpdate",
        pressedControls: Array.from(state.pressedControls),
    };
    state.server?.send(JSON.stringify(update));
}

export function handleKeyUp(event: KeyboardEvent, state: AppState) {
    const control = keyCodeToPlayerControl(event.code);
    if (control === null) return;

    state.pressedControls.delete(control);
    const update: PlayerControlUpdate = {
        type: "playerControlUpdate",
        pressedControls: Array.from(state.pressedControls),
    };
    state.server?.send(JSON.stringify(update));
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
