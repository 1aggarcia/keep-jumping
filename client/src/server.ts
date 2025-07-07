import { z } from "zod";

import { SocketMessage } from "./generated/socketMessage";
import { AppState, LeaderboardEntryParser, StateSnapshot } from "./types";
import { Button, subscribeButtonsToCursor } from "./ui/button";
import {
    displayServerUnavailable,
    fillLeaderboard,
    gameElements,
    renderMessageStats,
} from "./ui/dom";
import {
    clearCanvas,
    drawGame,
    drawMetadata,
    redrawGame,
} from "./ui/graphics";
import {
    applyEffects,
    Effect,
    throwError,
    updateElement,
    updateState,
} from "./effects";

const MAX_HISTORY_LEN = 25;
const ERROR_DISPLAY_TIME = 5000;
const DEFAULT_SERVER = "localhost:8081";

// type to represent SocketMessages with object literals
export type SocketMessageObject =
    Parameters<typeof SocketMessage.fromObject>[0];

/**
 * Check that the server is alive. Fetch the leaderboard if it is,
 * display an error message if health check fails.
 */
export const checkServerHealth = () => ({
    action: "fetch",
    data: {
        endpoint: getHttpEndpoint() + "/api/health",
        onSuccess: async () => [updateLeaderboard()],
        onError: displayServerUnavailable,
    }
} satisfies Effect);

export const updateLeaderboard = () => ({
    action: "fetch",
    data: {
        endpoint: getHttpEndpoint() + "/api/leaderboard",
        onSuccess: handleLeaderboardResponse,
        onError: handleLeaderboardError
    }
} satisfies Effect);

async function
handleLeaderboardResponse(response: Response): Promise<Effect[]> {
    if (!response.ok) {
        const errorText = await response.text();
        return [throwError(errorText)];
    }
    const entries: unknown = await response.json();
    const parsedEntries = z.array(
        LeaderboardEntryParser).safeParse(entries);

    if (!parsedEntries.success) {
        return [updateElement(
            gameElements.leaderboardStatus,
            e => e.text("Bad leaderboard data received from server")
        )];
    }
    return [
        {
            action: "generic",
            data: () => fillLeaderboard(parsedEntries.data),
        },
        updateElement(gameElements.leaderboardStatus, e => e.text(""))
    ];
}

function handleLeaderboardError(error: unknown): Effect[] {
    return [
        {
            action: "generic",
            data: () => console.error(error),
        },
        updateElement(
            gameElements.leaderboardStatus,
            e => e.text("Failed to update leaderboard")
        ),
    ];
}

/**
 * Serializes and sends `message` to the server in state, assuming a connection
 * is open. Counts the message in state for analytics.
 */
function sendToServer(
    message: SocketMessageObject, state: StateSnapshot
): Effect[] {
    if (state.server === null) {
        return [throwError(`Message sent to null server: ${message}`)];
    }
    const wrappedMessage = SocketMessage.fromObject(message);
    return [
        updateState({ messagesOut: state.messagesOut + 1 }),
        {
            action: "sendMessage",
            data: serialize(wrappedMessage)
        },
        renderMessageStats(state),
    ];
}

export function sendToServerImpure(
    state: AppState, message: SocketMessageObject
) {
    applyEffects(sendToServer(message, state), state);
}

export function connectToServer(name: string, state: StateSnapshot) {
    const effects: Effect[] = [];
    effects.push(clearCanvas());
    effects.push(updateState({
        bytesIn: 0,
        messagesIn: 0,
        messagesOut: 0,
        connectedStatus: "CONNECTING",
    }));
    effects.push(drawMetadata(state));
    effects.push({
        action: "generic",
        data: () => subscribeButtonsToCursor(state, [])
    });
    effects.push({
        action: "openServer",
        data: {
            endpoint: getWebsocketEndpoint(),
            username: name,
        }
    });
    return effects;
}

export function
onServerOpen(state: StateSnapshot, username: string): Effect[] {
    return [
        ...sendToServer({
            joinEvent: { name: username }
        }, state),
        clearCanvas(),
        updateState({ connectedStatus: "OPEN" }),
        drawMetadata(state),
        updateElement(gameElements.errorBox, e => e.empty()),
        updateElement(gameElements.connectedBox, e => e.show()),
        updateElement(gameElements.inactiveOverlay, e => e.hide()),
        {
            action: "generic",
            data: () => {
                const disconnectButton = new Button("Disconnect")
                    .positionRight()
                    .onClick(() => disconnectFromServer(state));
                subscribeButtonsToCursor(state, [disconnectButton]);
            }
        }
    ];
}

export async function onServerMessage(
    data: unknown,
    state: StateSnapshot
) {
    const effects: Effect[] = [];
    const newMessagesIn = state.messagesIn + 1;
    effects.push(updateState({
        messagesIn: newMessagesIn,
    }));
    if (!(data instanceof Blob)) {
        return [...effects, throwError(`unexpected message type: ${data}`)];
    }
    effects.push(updateState({
            bytesIn: state.bytesIn + data.size,
    }));
    const buffer = await data.arrayBuffer();
    const message = deserialize(new Uint8Array(buffer));
    if (message === null) {
        return [...effects, throwError(`unable to deserialize: ${data}`)];
    }

    if (newMessagesIn > MAX_HISTORY_LEN) {
        // garbage collect old messages
        effects.push(updateElement(
            gameElements.messagesBox,
            e => e.find("pre:last").remove()
        ));
    }
    const prettyMessage = JSON.stringify(message.toObject(), null, 2);
    effects.push(updateElement(
        gameElements.messagesBox,
        e => e.prepend(`<pre>${prettyMessage}</pre>`)
    ));

    effects.push(renderMessageStats(state));
    effects.push(...handleServerMessage(message, state));
    return effects;
}

export function onServerClose(state: AppState): Effect[] {
    const effects: Effect[] = [
        updateState({
            serverId: null,
            server: null,
            connectedStatus: "CLOSED"
        }),
        updateElement(gameElements.messagesBox, e => e.empty()),
        updateElement(gameElements.connectedBox, e => e.hide()),
        updateElement(gameElements.inactiveOverlay, e => e.show()),
        {
            action: "generic",
            data: () => {
                subscribeButtonsToCursor(state, []);
                redrawGame(state);
            }
        },
        updateLeaderboard(),
    ];
    if (state.gameOverMessage !== null) {
        const message = state.gameOverMessage;
        effects.push(updateElement(
            gameElements.gameOverMessage,
            e => e.show().text(message)
        ));
    }
    return effects;
}

export function onServerError(state: StateSnapshot): Effect[] {
    return [
        ...onServerClose(state),
        ...addErrorNotification(state, "Connection error"),
        updateState({ connectedStatus: "ERROR" }),
        updateElement(
            gameElements.errorBox,
            e => e.append("<p>Connection error</p>")
        ),
    ];
}

function handleServerMessage(
    message: SocketMessage, state: StateSnapshot
): Effect[] {
    const effects: Effect[] = [];
    if (message.payload === "gamePing") {
        effects.push(updateState({ lastPing: message.gamePing }));
        effects.push({
            action: "generic",
            data: () => drawGame(state, message.gamePing),
        });
    }
    else if (message.payload === "gameOverEvent") {
        effects.push(updateState({
            gameOverMessage: message.gameOverEvent.reason
        }));
        effects.push({ action: "closeServer" });
    }
    else if (message.payload === "errorReply") {
        const error = message.errorReply.message;
        effects.push(...addErrorNotification(state, error));
    }
    else if (message.payload === "joinReply") {
        effects.push(updateState({
            serverId: message.joinReply.serverId,
        }));
    }
    else {
        const error = `Unsupported message type: ${message.payload}`;
        effects.push(throwError(error));
    }
    return effects;
}

export function addErrorNotification(state: AppState, error: string): Effect[] {
    return [
        {
            action: "addError",
            data: error
        },
        {
            action: "generic",
            data: () => redrawGame(state)
        },
        {
            action: "removeError",
            data: { delayMs: ERROR_DISPLAY_TIME }
        }
    ];
}

function disconnectFromServer(state: AppState) {
    const server = state.server;
    if (server === null) {
        throw new ReferenceError("tried to disconnect from null server");
    }
    server.close();
}

function getWebsocketEndpoint() {
    const { VITE_WEBSOCKET_ENDPOINT } = import.meta.env;
    if (VITE_WEBSOCKET_ENDPOINT === undefined) {
        console.warn(
            "environment variable 'VITE_WEBSOCKET_ENDPOINT' is not set."
            + ` Using default '${DEFAULT_SERVER}'`
        );
        return "ws://" + DEFAULT_SERVER;
    }
    return VITE_WEBSOCKET_ENDPOINT;
}

function getHttpEndpoint() {
    const { VITE_HTTP_ENDPOINT } = import.meta.env;
    if (VITE_HTTP_ENDPOINT === undefined) {
        console.warn(
            "environment variable 'VITE_HTTP_ENDPOINT' is not set."
            + ` Using default '${DEFAULT_SERVER}'`
        );
        return "http://" + DEFAULT_SERVER;
    }
    return VITE_HTTP_ENDPOINT;
}

/**
 * Convert a protobuf message to a binary array
 */
function serialize(message: SocketMessage): Uint8Array {
    return message.serialize();
}

/**
 * Convert a binary array to a protobuf message
 * @returns `PingPong` instance if the data can by deserialized, null otherwise
 */
function deserialize(bytes: Uint8Array): SocketMessage | null {
    try {
        return SocketMessage.deserialize(bytes);
    } catch (e) {
        console.error(e);
        return null;
    }
}
