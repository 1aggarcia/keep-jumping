import { Button } from "./ui/button";
import { GamePing, PlayerControl } from "./generated/socketMessage";

export type Context2D = CanvasRenderingContext2D;

export type AppState = {
    server: WebSocket | null,
    connectedStatus: "CLOSED" | "CONNECTING" | "OPEN" | "ERROR";
    pressedControls: Set<PlayerControl>;
    lastPing: GamePing | null;
    errors: string[];
    context: Context2D;
    buttons: Button[];
    serverId: string | null;

    // non-essential stats
    bytesIn: number;
    messagesIn: number;
    messagesOut: number;
};
