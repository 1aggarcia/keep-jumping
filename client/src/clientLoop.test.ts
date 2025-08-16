import { describe, expect, it } from "vitest";
import { onClientTick } from "./clientLoop";
import { mockState, openTestConnection } from "./testUtils";
import { PlayerControl, SocketMessage } from "./generated/socketMessage";
import { Effect } from "./effects";

describe(onClientTick, async () => {
    const openServer = (await openTestConnection()).client;

    it("returns empty if server is missing", () => {
        const state = mockState({ server: null });
        expect(onClientTick(state)).toHaveLength(0);
    });

    it("returns empty if cached control set equals pressed controls", () => {
        const state = mockState({
            server: openServer,
            pressedControls: new Set([PlayerControl.UP, PlayerControl.LEFT]),
            cachedControls: new Set([PlayerControl.UP, PlayerControl.LEFT]),
        });
        expect(onClientTick(state)).toHaveLength(0);
    });

    it.each([
        [
            new Set([PlayerControl.UP]),
            new Set([PlayerControl.UP, PlayerControl.LEFT]),
            "different lengths",
        ],
        [
            new Set([PlayerControl.UP, PlayerControl.DOWN]),
            new Set([PlayerControl.UP, PlayerControl.LEFT]),
            "same length",
        ],
    ])(
        "returns effects when cached controls and pressed controls are not equal",
        (pressedControls, cachedControls, message) => {
            const state = mockState({
                server: openServer,
                pressedControls: pressedControls,
                cachedControls: cachedControls,
            });
            const expectedMessage = SocketMessage.fromObject({
                controlChangeEvent: {
                    pressedControls: Array.from(pressedControls)
                },
            });
            const sendMessageEffect: Effect = {
                action: "sendMessage",
                data: expectedMessage.serialize(),
            };
            expect(onClientTick(state), message).toContainEqual(sendMessageEffect);
            const cacheControlsEffect: Effect = { action: "cachePressedControls" };
            expect(onClientTick(state), message).toContainEqual(cacheControlsEffect);
        }
    );
});
