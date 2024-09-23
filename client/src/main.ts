import { setUpConnections } from "./networking/setup";
import { getGameContext, setupGame } from "./game/setup";
import { setUpMessaging } from "./messaging/setup";
import { AppState } from "./state/appState";

const appState: AppState = {
    server: null,
    connectedStatus: "CLOSED",
    pressedControls: new Set(),
    context: getGameContext(),
    buttons: [],
    messagesIn: 0,
    messagesOut: 0,
};

setupGame(appState);
setUpMessaging(appState);

// needs to happen after game setup to work with the canvas properly.
// this is really tight coupling and should be fixed
setUpConnections(appState);
