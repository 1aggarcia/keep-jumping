import { getServerEndpoint } from "./config";
import { AppState } from "../state/appState";
import { connectionElements } from "./elements";
import { clearGame } from "../game/renderer";
import { handleGameUpdate } from "../game/handler";

export function handleConnectBtnClick(state: AppState) {
    console.log("Dispatcing connect btn click: ", state);
    switch (state.connectedStatus) {
        case "CLOSED":
            connectToServer(state);
            break;
        case "OPEN":
            disconnectFromServer(state);
            break;
        default:
            console.warn(
                "connectBtnClick handler called with bad state:"
                + state.connectedStatus
            );
            return;
    }
}

export function sendToServer(state: AppState, message: string) {
    if (state.server == null) {
        throw new ReferenceError(`Message sent to null server: ${message}`);
    }
    state.server.send(message);
    state.messagesOut++;
    renderMessageStats(state);
}

function connectToServer(state: AppState) {
    state.connectedStatus = "CONNECTING";
    state.messagesIn = 0;
    state.messagesOut = 0;
    connectionElements.connectedStatus.text(state.connectedStatus);
    connectionElements.connectBtn.prop("disabled", true);

    const server = new WebSocket(getServerEndpoint());
    state.server = server;

    server.onopen = () => {
        state.connectedStatus = "OPEN";
        connectionElements.connectedStatus.text(state.connectedStatus);
        connectionElements.errorBox.empty();
        connectionElements.connectedBox.show();
        connectionElements.connectBtn
            .text("Disconnect")
            .prop("disabled", false);
    };
    server.onerror = () => {
        state.connectedStatus = "ERROR";
        connectionElements.connectedStatus.text(state.connectedStatus);
        connectionElements.errorBox.append("<p>Connection error</p>");
        connectionElements.connectBtn.prop("disabled", false);
        connectionElements.connectedBox.hide();
    };
    server.onclose = () => {
        state.connectedStatus = "CLOSED";
        connectionElements.connectedStatus.text(state.connectedStatus);
        connectionElements.connectBtn.text("Connect");
        connectionElements.messagesBox.empty();
        connectionElements.connectedBox.hide();
        clearGame(state);
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
