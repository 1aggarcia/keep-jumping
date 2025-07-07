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
    | { action: "fetch"; data: FetchEffect }
    | { action: "openServer"; data: OpenServerEffect }
    | { action: "closeServer"; data?: never }
    | { action: "sendMessage"; data: Uint8Array }
    | { action: "addError"; data: string }
    | { action: "removeError"; data: RemoveErrorEffect }
    | { action: "updateElement"; data: UpdateElementEffect }
    // TODO: add enough effect types to not need a "generic" action
    | { action: "generic"; data: () => void }

type ClearRectEffect = {
    x: number;
    y: number;
    width: number;
    height: number;
}

export type FetchEffect = {
    endpoint: string;
    onSuccess: (response: Response) => Promise<Effect[]>
    onError: (error: unknown) => Effect[];
}

type OpenServerEffect = {
    endpoint: string;
    username: string;
}

type RemoveErrorEffect = {
    delayMs: number;
}

type UpdateElementEffect = {
    element: JQuery;
    operation: (element: JQuery) => JQuery;
};

export const throwError = (message: string): Effect => ({
    action: "throwError",
    data: message,
});

export const updateState = (updates: Partial<AppState>): Effect => ({
    action: "updateState",
    data: updates
});

export const updateElement = (
    element: JQuery,
    operation: (element: JQuery) => JQuery
): Effect => ({
    action: "updateElement",
    data: { element, operation }
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
        case "fetch": {
            applyFetchEffect(data, state);
            break;
        }
        case "openServer": {
            applyOpenServerEffect(data, state);
            break;
        }
        case "closeServer": {
            state.server?.close();
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
        case "updateElement": {
            data.operation(data.element);
            break;
        }
        case "generic": {
            data();
            break;
        }
        default: {
            // this code should never be executed
            // the `never` type enforces that all cases are covered above
            const uncoveredAction: never = action;
            throw new Error(
                `Missing effect handler for action type: ${uncoveredAction}`);
        }
    };
}

async function applyFetchEffect(effect: FetchEffect, state: AppState) {
    try {
        const response = await fetch(effect.endpoint);
        const effects = await effect.onSuccess(response);
        applyEffects(effects, state);
    } catch (error) {
        const effects = effect.onError(error);
        applyEffects(effects, state);
    }
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
