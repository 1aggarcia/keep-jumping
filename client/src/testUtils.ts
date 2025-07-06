import WS from "vitest-websocket-mock";
import { AppState, Context2D } from "./types";

export function mockState(overrides?: Partial<AppState>): AppState {
    return {
        server: null,
        connectedStatus: "CLOSED",
        pressedControls: new Set(),
        lastPing: null,
        errors: [],
        context: null as unknown as Context2D,
        buttons: [],
        serverId: null,
        gameOverMessage: null,
        bytesIn: 0,
        messagesIn: 0,
        messagesOut: 0,
        ...overrides
    };
}

export async function openTestConnection() {
    const server = new WS("ws://");
    const client = new WebSocket("ws://");
    await server.connected;

    return { client, server };
}
