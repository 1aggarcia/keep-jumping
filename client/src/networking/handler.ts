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
import { JoinEvent, SocketMessage } from "../game/types/messages";

const MAX_HISTORY_LEN = 25;
const ERROR_DISPLAY_TIME = 5000;

export function sendToServer<T>(state: AppState, message: T) {
    if (state.server === null) {
        throw new ReferenceError(`Message sent to null server: ${message}`);
    }
    let encoded: string;
    if (typeof message === "string") {
        encoded = message;
    } else if (typeof message === "object") {
        encoded = JSON.stringify(message);
    } else {
        throw new TypeError(`Cannot serialize message type: ${message}`);
    }
    state.server.send(encoded);
    state.messagesOut++;
    renderMessageStats(state);
}

export function connectToServer(state: AppState, username: string) {
    clearCanvas(state.context);
    state.messagesIn = 0;
    state.messagesOut = 0;
    state.connectedStatus = "CONNECTING";
    renderMetadata(state);
    subscribeButtonsToCursor(state, []);  // to remove any buttons on the screen

    const server = new WebSocket(getServerEndpoint());
    state.server = server;

    server.onopen = () => {
        sendToServer<JoinEvent>(state, {
            type: "JoinEvent",
            name: username
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
    server.onmessage = (event) => {
        state.messagesIn++;
        const message = event.data;
        const prettyMessage = getPrettyMessage(message);
        // garbage collect old messages
        if (state.messagesIn > MAX_HISTORY_LEN) {
            networkElements.messagesBox.find("pre:last").remove();
        }
        networkElements.messagesBox.prepend(`<pre>${prettyMessage}</pre>`);
        renderMessageStats(state);
        handleServerMessage(message, state);
    };
}

function handleServerMessage(message: string, state: AppState) {
    const json: SocketMessage = JSON.parse(message);
    if (json.type === "GamePing") {
        state.lastPing = json;
        renderGame(state, json);
    } else if (json.type === "GameOverEvent") {
        renderGameOver(state.context, json.reason);
    } else if (json.type === "ErrorReply") {
        addErrorNotification(state, json.message);
    } else {
        throw new Error(`Unknown message type: ${message}`);
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

function getPrettyMessage(message: unknown) {
    if (!(typeof message === "string")) {
        return `${message}`;
    }
    try {
        return JSON.stringify(JSON.parse(message), undefined, 2);
    } catch {
        return message;
    }
}

function renderMessageStats(state: AppState) {
    const inText = `Received: ${state.messagesIn}`;
    const outText = `Sent: ${state.messagesOut}`;
    networkElements.messagesStats.text(inText + " | " + outText);
}
