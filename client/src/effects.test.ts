import { describe, expect, it, vi } from "vitest";
import { applyEffects, Effect, throwError, updateState } from "./effects";
import { mockState } from "./testUtils";
import {
    onServerClose,
    onServerError,
    onServerMessage,
    onServerOpen
} from "./server";

describe(applyEffects, () => {
    it("throws error", () => {
        const testEffect: Effect = throwError("test error");
        expect(() => applyEffects([testEffect], mockState())).toThrow();
    });

    it("clears game context rectangle", () => {
        // TODO
    });

    it("updates game state", () => {
        const testState = mockState({
            gameOverMessage: "should not change",
            messagesOut: 123,
        });
        const testEffect = updateState({
            messagesOut: 567,
        });
        applyEffects([testEffect], testState);
        expect(testState.messagesOut).toEqual(567);
        expect(testState.gameOverMessage).toEqual("should not change");
    });

    it("opens websocket server", () => {
        const testState = mockState({
            server: null
        });
        const testEffect: Effect = {
            action: "openServer",
            data: {
                endpoint: "ws://localhost",
                username: "test user"
            }
        };
        applyEffects([testEffect], testState);
        expect(testState.server).not.toBeNull();
    });

    it("attaches event listeners to websocket server", () => {
        const testState = mockState();
        const testEffect: Effect = {
            action: "openServer",
            data: {
                endpoint: "ws://localhost",
                username: "test user"
            }
        };
        applyEffects([testEffect], testState);
        const testServer = testState.server!;

        expect(testServer.onopen?.toString())
            .toContain(onServerOpen.name);

        expect(testServer.onclose?.toString())
            .toContain(onServerClose.name);

        expect(testServer.onerror?.toString())
            .toContain(onServerError.name);

        expect(testServer.onmessage?.toString())
            .toContain(onServerMessage.name);
    });

    it("enqueues error in state", () => {
        const testState = mockState({
            errors: ["1", "2"]
        });
        const testEffect: Effect = {
            action: "addError",
            data: "test error"
        };
        applyEffects([testEffect], testState);
        expect(testState.errors).toStrictEqual(["test error", "1", "2"]);
    });

    it("pops last error after time delay", async () => {
        const testState = mockState({
            errors: ["1", "2"]
        });
        const testEffect: Effect = {
            action: "removeError",
            data: { delayMs: 50 }
        };
        applyEffects([testEffect], testState);
        expect(testState.errors).toHaveLength(2);

        await new Promise(r => setTimeout(r, 100));
        expect(testState.errors).toHaveLength(1);
        expect(testState.errors).toStrictEqual(["1"]);
    });

    it("calls thunk passed in", () => {
        const testThunk = vi.fn();
        const testEffect: Effect = {
            action: "generic",
            data: testThunk,
        };
        applyEffects([testEffect], mockState());
        expect(testThunk).toHaveBeenCalledOnce();
    });

    it("throws error for unrecognized action", () => {
        const testEffect = {
            action: "not a real action",
        } as unknown as Effect;
        expect(() => applyEffects([testEffect], mockState())).toThrow();
    });
});
