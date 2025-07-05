import {
    onServerClose,
    onServerError,
    onServerMessage,
    onServerOpen
} from "./server";
import { AppState } from "./types";

export type Effect =
    | { action: "throwError"; data: string }
    | { action: "clearRect"; data: ClearRectEffect }
    | { action: "updateState"; data: Partial<AppState> }
    | { action: "openServer"; data: OpenServerEffect }
    | { action: "sendMessage"; data: Uint8Array }
    | { action: "addError"; data: string }
    | { action: "removeError"; data: RemoveErrorEffect }
    // TODO: add enough effect types to not need a "generic" action
    | { action: "generic"; data: () => void }

type ClearRectEffect = {
    x: number;
    y: number;
    width: number;
    height: number;
}

type OpenServerEffect = {
    endpoint: string;
    username: string;
}

type RemoveErrorEffect = {
    delayMs: number;
}

export const throwError = (message: string): Effect => ({
    action: "throwError",
    data: message,
});

export const updateState = (updates: Partial<AppState>): Effect => ({
    action: "updateState",
    data: updates
});

export function applyEffects(effects: Effect[], state: AppState) {
    for (const effect of effects) {
        applyEffect(effect, state);
    }
}

function applyEffect({ action, data }: Effect, state: AppState): void {
    switch (action) {
        case "throwError": {
            throw new Error(data);
        }
        case "clearRect": {
            const { x, y, width, height } = data;
            state.context.clearRect(x, y, width, height);
            break;
        }
        case "updateState": {
            Object.assign(state, data);
            break;
        }
        case "openServer": {
            applyOpenServerEffect(data, state);
            break;
        }
        case "sendMessage": {
            state.server?.send(data);
            break;
        }
        case "addError": {
            state.errors.unshift(data);
            break;
        }
        case "removeError": {
            setTimeout(() => state.errors.pop(), data.delayMs);
            break;
        }
        case "generic": {
            data();
            break;
        }
        default: {
            throw new Error(`Cannot handle '${action}'`);
        }
    };
}

function applyOpenServerEffect(effect: OpenServerEffect, state: AppState) {
    const server = new WebSocket(effect.endpoint);
    state.server = server;

    server.onopen = () =>
        applyEffects(onServerOpen(state, effect.username), state);

    server.onerror = () =>
        applyEffects(onServerError(state), state);

    server.onclose = () =>
        applyEffects(onServerClose(state), state);

    server.onmessage = (message) => onServerMessage(message.data, state)
        .then(effects => applyEffects(effects, state));
}
