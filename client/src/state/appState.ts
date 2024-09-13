import { Context2D, PlayerControl } from "@lib/types";

export type AppState = {
    server: WebSocket | null,
    connectedStatus: "CLOSED" | "CONNECTING" | "OPEN" | "ERROR";
    pressedControls: Set<PlayerControl>;
    context: Context2D | null;
    messagesIn: number;
    messagesOut: number;
};
