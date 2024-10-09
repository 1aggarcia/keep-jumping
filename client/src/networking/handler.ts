import { getServerEndpoint } from "./config";
import { AppState } from "../state/appState";
import { networkElements } from "./elements";
import { clearCanvas, renderMetadata } from "../game/renderer";
import { handleGameUpdate } from "../game/handler";
import { Button, subscribeButtonsToCursor } from "../canvas/button";
import { PlayerJoinUpdate } from "../game/types/messages";

export function sendToServer(state: AppState, message: string) {
    if (state.server == null) {
        throw new ReferenceError(`Message sent to null server: ${message}`);
    }
    state.server.send(message);
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
        const joinUpdate: PlayerJoinUpdate = {
            type: "playerJoinUpdate",
            name: username
        };
        sendToServer(state, JSON.stringify(joinUpdate));
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
        networkElements.messagesBox.prepend(`<pre>${prettyMessage}</pre>`);
        renderMessageStats(state);
        handleGameUpdate(message, state);
    };
}

function onServerClose(state: AppState) {
    clearCanvas(state.context);
    state.connectedStatus = "CLOSED";
    renderMetadata(state);
    networkElements.messagesBox.empty();
    networkElements.connectedBox.hide();
    networkElements.joinForm
        .trigger("reset")
        .show();

    subscribeButtonsToCursor(state, []);
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
