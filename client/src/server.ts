import { z } from "zod";

import { SocketMessage } from "./generated/socketMessage";
import { AppState, LeaderboardEntryParser, StateSnapshot } from "./types";
import { Button, subscribeButtonsToCursor } from "./ui/button";
import { fillLeaderboard, gameElements, renderMessageStats } from "./ui/dom";
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
    updateState,
} from "./effects";

const MAX_HISTORY_LEN = 25;
const ERROR_DISPLAY_TIME = 5000;
const DEFAULT_SERVER = "localhost:8081";

// type to represent SocketMessages with object literals
export type SocketMessageObject =
    Parameters<typeof SocketMessage.fromObject>[0];

/**
 * Try to connect to the server. Resolve the promise if the connection succeeds,
 * reject otherwise.
 */
export const checkServerHealth = () => new Promise<void>((resolve, reject) => {
    fetch(getHttpEndpoint() + "/api/health")
        .then(() => resolve())
        .catch(err => reject(err));
});

export async function updateLeaderboard() {
    gameElements.leaderboardStatus.text("Updating leaderboard...");
    try {
        const response = await fetch(getHttpEndpoint() + "/api/leaderboard");
        if (!response.ok) {
            throw new Error(await response.text());
        }

        const entries: unknown = await response.json();
        const parsedEntries = z.array(
            LeaderboardEntryParser).safeParse(entries);

        if (!parsedEntries.success) {
            console.error(parsedEntries.error);
            gameElements.leaderboardStatus
                .text("Bad leaderboard data received from server");
            return;
        }

        fillLeaderboard(parsedEntries.data);
        gameElements.leaderboardStatus.text("");
    } catch (err) {
        console.error(err);
        gameElements.leaderboardStatus.text("Failed to update leaderboard");
    }
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
        {
            action: "generic",
            data: () => renderMessageStats(state),
        }
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
        {
            action: "generic",
            data: () => {
                gameElements.errorBox.empty();
                gameElements.connectedBox.show();
                gameElements.inactiveOverlay.hide();
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
    effects.push(updateState({
        messagesIn: state.messagesIn + 1,
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

    effects.push({
        action: "generic",
        data: () => {
            // garbage collect old messages
            if (state.messagesIn > MAX_HISTORY_LEN) {
                gameElements.messagesBox.find("pre:last").remove();
            }
            const prettyMessage = JSON.stringify(message.toObject(), null, 2);
            gameElements.messagesBox.prepend(`<pre>${prettyMessage}</pre>`);
            renderMessageStats(state);
        }
    });
    effects.push(...handleServerMessage(message, state));
    return effects;
}

export function onServerClose(state: AppState): Effect[] {
    return [
        updateState({
            serverId: null,
            server: null,
            connectedStatus: "CLOSED"
        }),
        {
            action: "generic",
            data: () => {
                gameElements.messagesBox.empty();
                gameElements.connectedBox.hide();
                gameElements.inactiveOverlay.show();
                if (state.gameOverMessage) {
                    gameElements.gameOverMessage.show();
                    gameElements.gameOverMessage.text(state.gameOverMessage);
                }
                subscribeButtonsToCursor(state, []);
                redrawGame(state);
                updateLeaderboard();
            }
        }
    ];
}

export function onServerError(state: StateSnapshot): Effect[] {
    return [
        ...onServerClose(state),
        ...addErrorNotification(state, "Connection error"),
        updateState({ connectedStatus: "ERROR" }),
        {
            action: "generic",
            data: () =>
                gameElements.errorBox.append("<p>Connection error</p>"),
        }
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
        effects.push({
            action: "generic",
            data: () => state.server?.close(),
        });
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
