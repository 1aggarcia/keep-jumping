import $ from "jquery";
import { AppState } from "./types";

const devToolElements = {
    devTools: $("#dev-tools"),
    openCloseDevTools: $("#open-close-dev-tools"),
    messageForm: $<HTMLFormElement>("#message-form"),
};

export function enableDevTools(state: AppState) {
    devToolElements.openCloseDevTools.on("click", handleOpenCloseToggle);
    devToolElements.messageForm.on("submit",
        (event) => handleMessageFormSubmit(event, state)
    );
}

function handleOpenCloseToggle() {
    const isVisible = devToolElements.devTools.is(":visible");
    devToolElements.openCloseDevTools.text(
        isVisible ? "Show Dev Tools" : "Hide Dev Tools");
    devToolElements.devTools.toggle();
}

function handleMessageFormSubmit(
    event: JQuery.SubmitEvent, state: AppState
) {
    const formData = new FormData(event.target);
    const message = formData.get("message");
    state.server?.send(String(message));
    event.target.reset();
}
