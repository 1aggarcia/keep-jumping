import { AppState } from "../state/appState";
import { messagingElements } from "./elements";
import { handleMessageFormSubmit, handleOpenCloseToggle } from "./handler";

export function setUpMessaging(state: AppState) {
    messagingElements.openCloseDevTools.on("click", handleOpenCloseToggle);
    messagingElements.messageForm.on("submit",
        (event) => handleMessageFormSubmit(event, state)
    );
}
