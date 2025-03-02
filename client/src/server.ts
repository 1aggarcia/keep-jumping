import { SocketMessage } from "./generated/socketMessage";
import { AppState } from "./types";
import { Button, subscribeButtonsToCursor } from "./ui/button";
import { gameElements, renderMessageStats } from "./ui/dom";
import {
    clearCanvas,
    drawGame,
    drawGameOver,
    drawMetadata,
    redrawGame,
} from "./ui/graphics";

const MAX_NAME_LENGTH = 25;
const MAX_HISTORY_LEN = 25;
const ERROR_DISPLAY_TIME = 5000;
const DEFAULT_SERVER_ENDPOINT = "ws://localhost:8081";

// type to represent SocketMessages with object literals
type SocketMessageObject = Parameters<typeof SocketMessage.fromObject>[0];

/**
 * Try to connect to the server, show an error message if connection fails.
 */
export async function verifyServerHealth() {
    const server = new WebSocket(getServerEndpoint());
    server.onopen = () => server.close();
    server.onerror = () => {
        gameElements.joinForm.hide();
        gameElements.serverUnavaliableBox.show();
    };
}

/**
 * Serializes and sends `message` to the server in state, assuming a connection
 * is open. Counts the message in state for analytics.
 */
export function sendToServer(state: AppState, message: SocketMessageObject) {
    if (state.server === null) {
        throw new ReferenceError(`Message sent to null server: ${message}`);
    }
    const wrappedMessage = SocketMessage.fromObject(message);
    state.server.send(serialize(wrappedMessage));
    state.messagesOut++;
    renderMessageStats(state);
}

export function connectToServer(state: AppState, username: string) {
    if (username.length > MAX_NAME_LENGTH) {
        addErrorNotification(state, "Username is too long");
        return;
    }

    clearCanvas(state.context);
    state.bytesIn = 0;
    state.messagesIn = 0;
    state.messagesOut = 0;
    state.connectedStatus = "CONNECTING";
    drawMetadata(state);
    subscribeButtonsToCursor(state, []);  // to remove any buttons on the screen

    const server = new WebSocket(getServerEndpoint());
    state.server = server;

    server.onopen = () => {
        sendToServer(state, {
            joinEvent: {
                name: username
            }
        });
        clearCanvas(state.context);
        state.connectedStatus = "OPEN";
        drawMetadata(state);
        gameElements.errorBox.empty();
        gameElements.connectedBox.show();
        gameElements.inactiveOverlay.hide();

        const disconnectButton = new Button("Disconnect")
            .positionRight()
            .onClick(() => disconnectFromServer(state));
        subscribeButtonsToCursor(state, [disconnectButton]);
    };
    server.onclose = () => onServerClose(state);
    server.onerror = () => {
        onServerClose(state);
        addErrorNotification(state, "Connection error");
        state.connectedStatus = "ERROR";
        gameElements.errorBox.append("<p>Connection error</p>");
    };
    server.onmessage = async ({ data }) => {
        state.messagesIn++;

        if (!(data instanceof Blob)) {
            throw new TypeError(`unexpected message type: ${data}`);
        }
        state.bytesIn += data.size;
        const buffer = await data.arrayBuffer();
        const message = deserialize(new Uint8Array(buffer));
        if (message === null) {
            throw new SyntaxError(`unable to deserialize: ${data}`);
        }

        // garbage collect old messages
        if (state.messagesIn > MAX_HISTORY_LEN) {
            gameElements.messagesBox.find("pre:last").remove();
        }
        const prettyMessage = JSON.stringify(message.toObject(), undefined, 2);
        gameElements.messagesBox.prepend(`<pre>${prettyMessage}</pre>`);
        renderMessageStats(state);
        handleServerMessage(message, state);
    };
}

function onServerClose(state: AppState) {
    state.serverId = null;
    state.server = null;
    state.connectedStatus = "CLOSED";
    gameElements.messagesBox.empty();
    gameElements.connectedBox.hide();
    gameElements.inactiveOverlay.show();

    subscribeButtonsToCursor(state, []);
    redrawGame(state);
}

function handleServerMessage(message: SocketMessage, state: AppState) {
    if (message.payload === "gamePing") {
        state.lastPing = message.gamePing;
        drawGame(state, message.gamePing);
    }
    else if (message.payload === "gameOverEvent") {
        drawGameOver(state.context, message.gameOverEvent.reason);
    }
    else if (message.payload === "errorReply") {
        addErrorNotification(state, message.errorReply.message);
    }
    else if (message.payload === "joinReply") {
        state.serverId = message.joinReply.serverId;
    }
    else {
        throw new Error(`Unsupported message type: ${message.payload}`);
    }
}

function addErrorNotification(state: AppState, error: string) {
    state.errors.unshift(error);  // enqueue at start
    redrawGame(state);
    setTimeout(() => {
        state.errors.pop();  // dequeue from end
        redrawGame(state);
    }, ERROR_DISPLAY_TIME);
}

function disconnectFromServer(state: AppState) {
    const server = state.server;
    if (server === null) {
        throw new ReferenceError("tried to disconnect from null server");
    }
    server.close();
}

function getServerEndpoint() {
    const { VITE_SERVER_ENDPOINT } = import.meta.env;
    if (VITE_SERVER_ENDPOINT === undefined) {
        console.warn(
            "environment variable 'VITE_SERVER_ENDPOINT' is not set."
            + ` Using default '${DEFAULT_SERVER_ENDPOINT}'`
        );
        return DEFAULT_SERVER_ENDPOINT;
    }
    return VITE_SERVER_ENDPOINT;
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
