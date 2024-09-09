import { setUpConnections } from "./connections/setup";
import { setupGame } from "./game/setup";
import { setUpMessaging } from "./messaging/setup";
import { AppState } from "./state/appState";

const appState: AppState = {
    server: null,
    connectedStatus: "CLOSED",
    pressedControls: new Set(),
    context: null,
};

setUpConnections(appState);
setUpMessaging(appState);
setupGame(appState);
