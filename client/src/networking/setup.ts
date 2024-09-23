import { Button, subscribeButtonsToCursor } from "../game/button";
import { AppState } from "../state/appState";
import { connectionElements } from "./elements";
import { connectToServer } from "./handler";

export function setUpConnections(state: AppState) {
    const connectButton = new Button("Connect")
        .positionRight()
        .onClick(() => connectToServer(state));

    subscribeButtonsToCursor(state, [connectButton]);

    connectionElements.viteMode.text(
        `Mode: ${import.meta.env.MODE} | Last Updated + ${LAST_UPDATED}`
    );
    connectionElements.connectedStatus.text(state.connectedStatus);
}
