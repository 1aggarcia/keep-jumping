import { Button, subscribeButtonsToCursor } from "../canvas/button";
import { renderConnectionStatus } from "../game/renderer";
import { AppState } from "../state/appState";
import { connectionElements } from "./elements";
import { connectToServer } from "./handler";

export function setUpNetworking(state: AppState) {
    const connectButton = new Button("Connect")
        .positionRight()
        .onClick(() => connectToServer(state));

    subscribeButtonsToCursor(state, [connectButton]);
    renderConnectionStatus(state);

    connectionElements.viteMode.text(
        `Mode: ${import.meta.env.MODE} | Last Updated + ${LAST_UPDATED}`
    );
}
