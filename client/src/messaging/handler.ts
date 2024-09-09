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
    if (state.server === null) {
      throw new ReferenceError("Stored server is null");
    }
    const formData = new FormData(event.target);
    const message = formData.get("message");
    state.server.send(String(message));
    event.target.reset();
}
