import { AppState } from "../state/appState";
import { connectionElements } from "./elements";
import { handleConnectBtnClick } from "./handler";

export function setUpConnections(state: AppState) {
    connectionElements.viteMode.text(
        `Mode: ${import.meta.env.MODE} | Last Updated + ${LAST_UPDATED}`
    );
    connectionElements.connectedStatus.text(state.connectedStatus);
    connectionElements.connectBtn.on("click",
        () => handleConnectBtnClick(state)
    );
}
