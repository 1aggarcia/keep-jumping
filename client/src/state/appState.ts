import { Context2D } from "../canvas/types";
import { Button } from "../canvas/button";
import { GamePing, PlayerControl } from "../generated/socketMessage";

export type AppState = {
    server: WebSocket | null,
    connectedStatus: "CLOSED" | "CONNECTING" | "OPEN" | "ERROR";
    pressedControls: Set<PlayerControl>;
    lastPing: GamePing | null;
    errors: string[];
    context: Context2D;
    buttons: Button[];

    // non-essential stats
    bytesIn: number;
    messagesIn: number;
    messagesOut: number;
};
