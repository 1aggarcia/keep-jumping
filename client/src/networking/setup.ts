import { renderMetadata } from "../game/renderer";
import { AppState } from "../state/appState";
import { networkElements } from "./elements";
import { connectToServer } from "./handler";

export function setUpNetworking(state: AppState) {
    renderMetadata(state);
    networkElements.viteMode.text(`Mode: ${import.meta.env.MODE} | v${VERSION}`);
    networkElements.joinForm.on("submit", (event) => {
        const name = new FormData(event.target).get("name");
        if (name === null || name.toString().length === 0) {
            return;
        }
        connectToServer(state, String(name));
    });
}
