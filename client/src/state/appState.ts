import { Context2D } from "../canvas/types";
import { Button } from "../canvas/button";
import { PlayerControl } from "../game/types/models";

export type AppState = {
    server: WebSocket | null,
    connectedStatus: "CLOSED" | "CONNECTING" | "OPEN" | "ERROR";
    pressedControls: Set<PlayerControl>;
    context: Context2D;
    buttons: Button[];
    messagesIn: number;
    messagesOut: number;
};
