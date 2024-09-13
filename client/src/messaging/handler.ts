import { sendToServer } from "../connections/handler";
import { AppState } from "../state/appState";
import { messagingElements } from "./elements";

export function handleOpenCloseToggle() {
  const isVisible = messagingElements.devTools.is(":visible");
  messagingElements.openCloseDevTools.text(
    isVisible ? "Show Dev Tools" : "Hide Dev Tools");
  messagingElements.devTools.slideToggle();
}

export function handleMessageFormSubmit(
    event: JQuery.SubmitEvent, state: AppState
) {
    const formData = new FormData(event.target);
    const message = formData.get("message");
    sendToServer(state, String(message));
    event.target.reset();
}
