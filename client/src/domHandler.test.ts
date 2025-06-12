import WS from "vitest-websocket-mock";

import { handleKeyDown, handleKeyUp } from "./domHandler";
import { PlayerControl, SocketMessage } from "./generated/socketMessage";
import { AppState, Context2D } from "./types";
import { it, afterEach, expect, describe, beforeEach } from "vitest";

// https://github.com/akiomik/vitest-websocket-mock

function mockState(overrides?: Partial<AppState>): AppState {
    return {
        server: null,
        connectedStatus: "CLOSED",
        pressedControls: new Set(),
        lastPing: null,
        errors: [],
        context: null as unknown as Context2D,
        buttons: [],
        serverId: null,
        bytesIn: 0,
        messagesIn: 0,
        messagesOut: 0,
        ...overrides
    };
}

async function openTestConnection() {
    const server = new WS("ws://");
    const client = new WebSocket("ws://");
    await server.connected;

    return { client, server };
}

describe(handleKeyDown, () => {
    afterEach(() => WS.clean());

    it("does nothing for bad control", async () => {
        const connection = await openTestConnection();
        const state = mockState({ server: connection.client });

        handleKeyDown("not a control", state);
        expect(state.pressedControls.size).toBe(0);
        expect(connection.server).toHaveReceivedMessages([]);
    });

    it("sends ControlChangeEvent on open server", async () => {
        const connection = await openTestConnection();
        const state = mockState({ server: connection.client });

        handleKeyDown("ArrowUp", state);
        const expected = SocketMessage.fromObject({
            controlChangeEvent: {
                pressedControls: [PlayerControl.UP]
            }
        });
        await expect(connection.server).toReceiveMessage(expected.serialize());
    });

    it("saves new control to state", async () => {
        const client = (await openTestConnection()).client;
        const state = mockState({ server: client });

        handleKeyDown("ArrowDown", state);
        expect(state.pressedControls.size).toBe(1);
        expect(state.pressedControls.has(PlayerControl.DOWN)).toBeTruthy();
    });

    it("saves new control to state even if server is null", () => {
        const state = mockState();
        handleKeyDown("ArrowUp", state);
        expect(state.pressedControls.size).toBe(1);
        expect(state.pressedControls).toContain(PlayerControl.UP);
    });
});

describe(handleKeyUp, () => {
    let startingControls: Set<PlayerControl>;

    beforeEach(() => {
        startingControls = new Set([PlayerControl.DOWN, PlayerControl.RIGHT]);
    });

    afterEach(() => WS.clean());

    it("does nothing for bad control", async () => {
        const connection = await openTestConnection();
        const state = mockState({
            server: connection.client,
            pressedControls: startingControls
        });

        handleKeyUp("not a control", state);
        expect(state.pressedControls.size).toBe(2);
        expect(connection.server).toHaveReceivedMessages([]);
    });

    it("sends ControlChangeEvent on open server", async () => {
        const connection = await openTestConnection();
        const state = mockState({
            server: connection.client,
            pressedControls: startingControls
        });

        handleKeyUp("ArrowDown", state);
        const expected = SocketMessage.fromObject({
            controlChangeEvent: {
                pressedControls: [PlayerControl.RIGHT]
            }
        });
        await expect(connection.server).toReceiveMessage(expected.serialize());
    });

    it("removes control from state", async () => {
        const client = (await openTestConnection()).client;
        const state = mockState({
            server: client,
            pressedControls: startingControls
        });

        handleKeyUp("ArrowDown", state);
        expect(state.pressedControls.size).toBe(1);
        expect(state.pressedControls.has(PlayerControl.RIGHT));
    });

    it("removes control from state even if server is null", () => {
        const state = mockState({ pressedControls: startingControls });
        handleKeyUp("ArrowDown", state);
        expect(state.pressedControls.size).toBe(1);
        expect(state.pressedControls).toContain(PlayerControl.RIGHT);
    });
});
