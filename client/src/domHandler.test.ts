import WS from "vitest-websocket-mock";

import { handleKeyDown, handleKeyUp, onJoinSubmit } from "./domHandler";
import { PlayerControl } from "./generated/socketMessage";
import { it, afterEach, expect, describe, beforeEach } from "vitest";
import { mockState } from "./testUtils";

// https://github.com/akiomik/vitest-websocket-mock

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

    it("saves new control to state", async () => {
        const client = (await openTestConnection()).client;
        const state = mockState({ server: client });

        handleKeyDown("ArrowDown", state);
        expect(state.pressedControls.size).toBe(1);
        expect(state.pressedControls.has(PlayerControl.DOWN)).toBeTruthy();
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
});

describe(onJoinSubmit, () => {
    function makeSubmitEvent(name: string) {
        const nameInput = document.createElement("input");
        nameInput.value = name;
        nameInput.name = "name";

        const target = document.createElement("form");
        target.appendChild(nameInput);

        return { target } as JQuery.SubmitEvent;
    }

    it("returns no effects if name is empty", () => {
        const fakeEvent = makeSubmitEvent("");
        const result = onJoinSubmit(fakeEvent, mockState());
        expect(result).toHaveLength(0);
    });

    it("returns error if name is too long", () => {
        const fakeEvent = makeSubmitEvent("x".repeat(1000));
        const result = onJoinSubmit(fakeEvent, mockState());
        expect(result).toContainEqual({
            action: "addError",
            data: "Username is too long",
        });
    });

    it("connects to server if name is valid length", () => {
        const fakeEvent = makeSubmitEvent("test user");
        const result = onJoinSubmit(fakeEvent, mockState());
        // TODO: replace with stricter check once generic action is removed
        expect(result).toContainEqual(expect.objectContaining({
            action: "openServer"
        }));
    });
});
