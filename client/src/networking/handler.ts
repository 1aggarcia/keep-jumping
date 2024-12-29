import { getServerEndpoint } from "./config";
import { AppState } from "../state/appState";
import { networkElements } from "./elements";
import { clearCanvas, renderGame, renderMetadata } from "../game/renderer";
import { handleServerMessage } from "../game/handler";
import { Button, subscribeButtonsToCursor } from "../canvas/button";
import { JoinEvent } from "../game/types/messages";

const MAX_HISTORY_LEN = 25;

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

function onServerClose(state: AppState) {
    state.connectedStatus = "CLOSED";
    networkElements.messagesBox.empty();
    networkElements.connectedBox.hide();
    networkElements.joinForm
        .trigger("reset")
        .show();

    subscribeButtonsToCursor(state, []);
    if (state.lastPing === null) {
        console.warn("onServerClose called before receiving any pings");
        return;
    }
    renderGame(state, state.lastPing);
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
