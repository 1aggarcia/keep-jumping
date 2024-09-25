import { Button, subscribeButtonsToCursor } from "../canvas/button";
import { renderMetadata } from "../game/renderer";
import { AppState } from "../state/appState";
import { connectToServer } from "./handler";

export function setUpNetworking(state: AppState) {
    const connectButton = new Button("Connect")
        .positionRight()
        .onClick(() => connectToServer(state));

    subscribeButtonsToCursor(state, [connectButton]);
    renderMetadata(state);
}
