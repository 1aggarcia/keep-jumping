import { getServerEndpoint } from "./config";
import { AppState } from "../state/appState";
import { networkElements } from "./elements";
import {
    clearCanvas,
    renderGame,
    renderGameOver,
    renderMetadata,
    rerender
} from "../game/renderer";
import { Button, subscribeButtonsToCursor } from "../canvas/button";
import {
    serialize,
    deserialize,
    formatBytesString,
} from "./formatters";
import { SocketMessage } from "../generated/socketMessage";

const MAX_NAME_LENGTH = 25;
const MAX_HISTORY_LEN = 25;
const ERROR_DISPLAY_TIME = 5000;

// type to represent SocketMessages with object literals
type SocketMessageObject = Parameters<typeof SocketMessage.fromObject>[0];

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
    renderMetadata(state);
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
        renderMetadata(state);
        networkElements.errorBox.empty();
        networkElements.connectedBox.show();
        networkElements.joinForm.hide();

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
        networkElements.errorBox.append("<p>Connection error</p>");
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
            networkElements.messagesBox.find("pre:last").remove();
        }
        const prettyMessage = JSON.stringify(message.toObject(), undefined, 2);
        networkElements.messagesBox.prepend(`<pre>${prettyMessage}</pre>`);
        renderMessageStats(state);
        handleServerMessage(message, state);
    };
}


function handleServerMessage(message: SocketMessage, state: AppState) {
    if (message.payload === "gamePing") {
        state.lastPing = message.gamePing;
        renderGame(state, message.gamePing);
    } else if (message.payload === "gameOverEvent") {
        renderGameOver(state.context, message.gameOverEvent.reason);
    } else if (message.payload === "errorReply") {
        addErrorNotification(state, message.errorReply.message);
    } else {
        throw new Error(`Unsupported message type: ${message.payload}`);
    }
}

function addErrorNotification(state: AppState, error: string) {
    state.errors.unshift(error);  // enqueue at start
    rerender(state);
    setTimeout(() => {
        state.errors.pop();  // dequeue from end
        rerender(state);
    }, ERROR_DISPLAY_TIME);
}


function onServerClose(state: AppState) {
    state.connectedStatus = "CLOSED";
    networkElements.messagesBox.empty();
    networkElements.connectedBox.hide();
    networkElements.joinForm
        .trigger("reset")
        .show();

    subscribeButtonsToCursor(state, []);
    rerender(state);
}

function disconnectFromServer(state: AppState) {
    const server = state.server;
    if (server === null) {
        throw new ReferenceError("tried to disconnect from null server");
    }
    server.close();
}

function renderMessageStats(state: AppState) {
    const outText = `Sent: ${state.messagesOut}`;
    const inText = `Received: ${state.messagesIn}`;
    const bytesText = `Data in: ${formatBytesString(state.bytesIn)}`;

    const meanPingSize = Math.floor(state.bytesIn / state.messagesIn);
    const meanText = `Mean ping size: ${formatBytesString(meanPingSize)}`;

    networkElements.messagesStats
        .text(outText + " | " + inText + " | " + bytesText + " | " + meanText);
}
