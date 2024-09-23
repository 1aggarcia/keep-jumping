import { Context2D, PlayerControl } from "@lib/types";
import { Button } from "../game/button";

export type AppState = {
    server: WebSocket | null,
    connectedStatus: "CLOSED" | "CONNECTING" | "OPEN" | "ERROR";
    pressedControls: Set<PlayerControl>;
    context: Context2D;
    buttons: Button[]
    messagesIn: number;
    messagesOut: number;
};
