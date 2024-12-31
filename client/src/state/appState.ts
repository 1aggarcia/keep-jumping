import { Context2D } from "../canvas/types";
import { Button } from "../canvas/button";
import { PlayerControl } from "../game/types/models";
import { GamePing } from "../game/types/messages";

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
