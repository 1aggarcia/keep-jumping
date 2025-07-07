import "vitest-canvas-mock";
import WS from "vitest-websocket-mock";
import { AppState } from "./types";
import { expect } from "vitest";
import { Effect } from "./effects";

export function mockState(overrides?: Partial<AppState>): AppState {
    const testContext = document
        .createElement("canvas")
        .getContext("2d")!;
    expect(testContext).not.toBeNull();

    return {
        server: null,
        connectedStatus: "CLOSED",
        pressedControls: new Set(),
        lastPing: null,
        errors: [],
        context: testContext,
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

export function assertElementChanged(
    effects: Effect[],
    element: JQuery,
    count?: number
) {
    const elementUpdates = effects.filter(effect =>
        effect.action === "updateElement"
        && effect.data.element === element
    );
    if (count === undefined) {
        expect(elementUpdates).not.toHaveLength(0);
    } else {
        expect(elementUpdates).toHaveLength(count);
    }
}
