import { getServerEndpoint } from "./config";
import { AppState } from "../state/appState";
import { connectionElements } from "./elements";
import { clearGame } from "../game/renderer";
import { handleGameUpdate } from "../game/handler";
import { Button, subscribeButtonsToCursor } from "../game/button";

export function sendToServer(state: AppState, message: string) {
    if (state.server == null) {
        throw new ReferenceError(`Message sent to null server: ${message}`);
    }
    state.server.send(message);
    state.messagesOut++;
    renderMessageStats(state);
}

export function connectToServer(state: AppState) {
    state.connectedStatus = "CONNECTING";
    state.messagesIn = 0;
    state.messagesOut = 0;
    connectionElements.connectedStatus.text(state.connectedStatus);
    subscribeButtonsToCursor(state, []);  // to remove any buttons on the screen

    const server = new WebSocket(getServerEndpoint());
    state.server = server;

    server.onopen = () => {
        state.connectedStatus = "OPEN";
        connectionElements.connectedStatus.text(state.connectedStatus);
        connectionElements.errorBox.empty();
        connectionElements.connectedBox.show();

        const disconnectButton = new Button("Disconnect")
            .positionRight()
            .onClick(() => disconnectFromServer(state));
        subscribeButtonsToCursor(state, [disconnectButton]);
    };
    server.onclose = () => onServerClose(state);
    server.onerror = () => {
        onServerClose(state);
        state.connectedStatus = "ERROR";
        connectionElements.errorBox.append("<p>Connection error</p>");
    };
    server.onmessage = (event) => {
        state.messagesIn++;
        const message = event.data;
        const prettyMessage = getPrettyMessage(message);
        connectionElements.messagesBox.prepend(`<pre>${prettyMessage}</pre>`);
        renderMessageStats(state);
        handleGameUpdate(message, state);
    };
}

function onServerClose(state: AppState) {
    state.connectedStatus = "CLOSED";
    connectionElements.connectedStatus.text(state.connectedStatus);
    connectionElements.messagesBox.empty();
    connectionElements.connectedBox.hide();
    clearGame(state);

    const connectButton = new Button("Connect")
        .positionRight()
        .onClick(() => connectToServer(state));
    subscribeButtonsToCursor(state, [connectButton]);
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
    connectionElements.messagesStats.text(inText + " | " + outText);
}
