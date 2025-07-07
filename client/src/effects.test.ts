import { beforeEach, describe, expect, it, vi } from "vitest";
import jQuery from "jquery";
import {
    applyEffects,
    Effect,
    FetchEffect,
    throwError,
    updateState
} from "./effects";
import { mockState, openTestConnection } from "./testUtils";
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
        const testState = mockState();
        const testEffect: Effect = {
            action: "clearRect",
            data: {
                x: 1,
                y: 2,
                width: 3,
                height: 4
            },
        };
        applyEffects([testEffect], testState);
        expect(testState.context.__getEvents()).toMatchSnapshot();
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

    describe("fetch effect", () => {
        let testEffect: { action: "fetch"; data: FetchEffect };

        beforeEach(() => {
            const testSuccessHandler = vi.fn(async () => [
                updateState({ gameOverMessage: "test success update" })
            ]);
            const testErrorHandler = vi.fn(() => [
                updateState({ gameOverMessage: "test error update" })
            ]);
            testEffect = {
                action: "fetch",
                data: {
                    endpoint: "www.whatever.com",
                    onSuccess: testSuccessHandler,
                    onError: testErrorHandler,
                }
            };
        });

        it("calls fetch with endpoint passed in", () => {
            window.fetch = vi.fn(async () => new Response());
            applyEffects([testEffect], mockState());
            expect(window.fetch).toBeCalledWith("www.whatever.com");
        });

        it("calls success handler when fetch succeeds", async () => {
            const testResponse = new Response("success");
            window.fetch = () => Promise.resolve(testResponse);

            applyEffects([testEffect], mockState());
            await new Promise(r => setTimeout(r));
            expect(testEffect.data.onSuccess).toBeCalledWith(testResponse);
        });

        it("applies success effects when fetch succeeds", async () => {
            window.fetch = () => Promise.resolve(new Response());
            const testState = mockState();

            applyEffects([testEffect], testState);
            await new Promise(r => setTimeout(r));
            expect(testState.gameOverMessage).toBe("test success update");
        });

        it("calls error handler with error when fetch fails", async () => {
            const testError = new Error("Fetch Error");
            window.fetch = () => Promise.reject(testError);

            applyEffects([testEffect], mockState());
            await new Promise(r => setTimeout(r));
            expect(testEffect.data.onError).toBeCalledWith(testError);
        });

        it("calls error handler when success handler throws", async () => {
            testEffect.data.onSuccess = async () => [throwError("test error")];
            window.fetch = () => Promise.resolve(new Response());

            applyEffects([testEffect], mockState());
            await new Promise(r => setTimeout(r));
            expect(testEffect.data.onError)
                .toBeCalledWith(new Error("test error"));
        });
    });

    it("opens websocket server", () => {
        const testState = mockState({
            server: null,
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

    it("closes websocket server", async () => {
        const connection = await openTestConnection();
        const testState = mockState({
            server: connection.client
        });
        const testEffect: Effect = { action: "closeServer" };
        expect(testState.server?.readyState).toBe(WebSocket.OPEN);

        applyEffects([testEffect], testState);
        expect([WebSocket.CLOSING, WebSocket.CLOSED])
            .toContainEqual(testState.server?.readyState);
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

    it("calls element update function with jQuery element passed in", () => {
        const testUpdateFunc = vi.fn();
        const testElement = jQuery("<p>");
        const testEffect: Effect = {
            action: "updateElement",
            data: {
                element: testElement,
                operation: testUpdateFunc,
            }
        };
        applyEffects([testEffect], mockState());
        expect(testUpdateFunc).toBeCalledWith(testElement);
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
