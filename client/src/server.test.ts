import { describe, expect, it } from "vitest";
import {
    addErrorNotification,
    checkServerHealth,
    connectToServer,
    onServerClose,
    onServerError,
    onServerMessage,
    onServerOpen,
    SocketMessageObject,
    updateLeaderboard
} from "./server";
import {
    assertElementChanged,
    mockState,
    openTestConnection
} from "./testUtils";
import { SocketMessage } from "./generated/socketMessage";
import { gameElements } from "./ui/dom";
import { Effect, throwError } from "./effects";

describe(checkServerHealth, () => {
    const effect = checkServerHealth();

    it("contains correct metadata", () => {
        const expected: Effect = {
            action: "fetch",
            data: expect.objectContaining({
                endpoint: "http://localhost:8081/api/health"
            })
        };
        expect(effect).toStrictEqual(expected);
    });

    it("updates leaderboard if call is successful", async () => {
        const result = await effect.data.onSuccess();
        expect(result).toStrictEqual([updateLeaderboard()]);
    });

    it("sets DOM to unavailable state if call fails", () => {
        const result = effect.data.onError("test error");
        assertElementChanged(result, gameElements.joinForm);
        assertElementChanged(result, gameElements.leaderboard);
        assertElementChanged(result, gameElements.leaderboardStatus);
        assertElementChanged(result, gameElements.serverUnavailableBox);
    });
});

describe(connectToServer, () => {
    it("returns state update and connection request", () => {
        const actual = connectToServer("test user", mockState());
        expect(actual[0].action).toBe("clearRect");
        expect(actual[1]).toStrictEqual({
            action: "updateState",
            data: {
                bytesIn: 0,
                messagesIn: 0,
                messagesOut: 0,
                connectedStatus: "CONNECTING",
            }
        });
        expect(actual).toContainEqual(
            expect.objectContaining({
                action: "openServer",
                data: expect.objectContaining({ username: "test user" })
            })
        );
    });
});

describe(updateLeaderboard, () => {
    const fetchEffect = updateLeaderboard();
    const successHandler = fetchEffect.data.onSuccess;
    const errorHandler = fetchEffect.data.onError;

    it("return correct metadata", () => {
        const expected: Effect = {
            action: "fetch",
            data: expect.objectContaining({
                endpoint: "http://localhost:8081/api/leaderboard",
            }),
        };
        expect(fetchEffect).toStrictEqual(expected);
    });

    it("updates leaderboard status on error", () => {
        const result = errorHandler(new Error("Fetch error"));
        assertElementChanged(result, gameElements.leaderboardStatus);
    });

    it("throws error when response contains bad status", async () => {
        const testResponse = new Response("Bad Request", {
            status: 400
        });
        const result = await successHandler(testResponse);
        expect(result).toStrictEqual([throwError("Bad Request")]);
    });

    it("updates leaderboardStatus when server sends bad data", async () => {
        const malformedResponse = JSON.stringify([{
            player: "",
            score: "should be a number",
            timestamp: "",
        }]);
        const result = await successHandler(new Response(malformedResponse));
        // should not include effect to fill leaderboard
        expect(result).toHaveLength(1);
        assertElementChanged(result, gameElements.leaderboardStatus);
    });

    it("fills leaderboard when server sends good data", async () => {
       // TODO
    });
});

describe(onServerOpen, async () => {
    const testState = mockState({
        messagesOut: 15,
        server: (await openTestConnection()).client
    });

    const result = onServerOpen(testState, "test user");

    it("sends joinEvent to server", () => {
        const expectedMessage = SocketMessage.fromObject({
            joinEvent: { name: "test user" },
        });
        expect(result).toContainEqual({
            action: "updateState",
            data: { messagesOut: 16 }
        });
        expect(result).toContainEqual({
            action: "sendMessage",
            data: expectedMessage.serialize()
        });
    });

    it("clears the canvas", () => {
        expect(result).toContainEqual(
            expect.objectContaining({ action: "clearRect" })
        );
    });

    it("updates DOM to server open state", () => {
        assertElementChanged(result, gameElements.errorBox);
        assertElementChanged(result, gameElements.connectedBox);
        assertElementChanged(result, gameElements.inactiveOverlay);
    });
});

describe(onServerMessage, () => {
    function makeBlobMessage(message: SocketMessageObject) {
        const binary = SocketMessage.fromObject(message).serialize();
        const blob = new Blob([binary]);
        // Blob.arrayBuffer is not supported by the test environment
        blob.arrayBuffer = () => Promise.resolve(binary.buffer);
        return blob;
    }

    it("counts message in state", async () => {
        const result = await onServerMessage(null, mockState({
            messagesIn: 142
        }));
        expect(result).toContainEqual({
            action: "updateState",
            data: {
                messagesIn: 143,
            },
        });
    });

    it("returns error if data is not blob", async () => {
        const result = await onServerMessage("not a blob", mockState());
        expect(result).toContainEqual(
            expect.objectContaining({ action: "throwError" })
        );
    });

    it("returns error if data cannot be deserialized", async () => {
        const badMessage = new Uint8Array([1, 2, 3]);
        const badBlob = new Blob([badMessage]);
        // Blob.bytes is not supported by the test environment
        badBlob.arrayBuffer = () => Promise.resolve(badMessage);

        const result = await onServerMessage(badBlob, mockState());
        expect(result).toContainEqual(
            expect.objectContaining({ action: "throwError" })
        );
    });

    it("does not return error if data is SocketMessage", async () => {
        const blob = makeBlobMessage({
            gameOverEvent: { reason: "test reason" }
        });
        const result = await onServerMessage(blob, mockState());
        expect(result).not.toContainEqual(
            expect.objectContaining({ action: "error" })
        );
    });

    it("returns effects to update messageBox if history is full", async () => {
        const blob = makeBlobMessage({
            gameOverEvent: { reason: "test reason" }
        });
        const state = mockState({ messagesIn: 100 });
        const result = await onServerMessage(blob, state);
        assertElementChanged(result, gameElements.messagesBox, 2);
    });

    it("saves ping if message is gamePing", async () => {
        const testPing = {
            gamePing: { serverAge: 3, platforms: [], players: [] }
        };
        const blob = makeBlobMessage(testPing);
        const result = await onServerMessage(blob, mockState());
        const expectedPing = SocketMessage.fromObject(testPing);
        expect(result).toContainEqual({
            action: "updateState",
            data: {
                lastPing: expectedPing.gamePing
            }
        });
    });

    it("closes server and saves message if gameOverEvent", async () => {
        const blob = makeBlobMessage({
            gameOverEvent: {
                reason: "test reason"
            }
        });
        const result = await onServerMessage(blob, mockState());
        expect(result).toContainEqual({
            action: "updateState",
            data: {
                gameOverMessage: "test reason"
            }
        });
        expect(result).toContainEqual({ action: "closeServer" });
    });

    it("returns error if message is errorReply", async () => {
        const blob = makeBlobMessage({
            errorReply: {
                message: "test error"
            }
        });
        const result = await onServerMessage(blob, mockState());
        expect(result).toContainEqual({
            action: "addError",
            data: "test error"
        });
    });

    it("saves serverId if message is joinReply", async () => {
        const blob = makeBlobMessage({
            joinReply: {
                serverId: "test id",
            },
        });
        const result = await onServerMessage(blob, mockState());
        expect(result).toContainEqual({
            action: "updateState",
            data: { serverId: "test id" },
        });
    });

    it("returns error if message type is unsupported", async () => {
        const blob = makeBlobMessage({
            joinEvent: {}
        });
        const result = await onServerMessage(blob, mockState());
        expect(result).toContainEqual({
            action: "throwError",
            data: expect.stringContaining("Unsupported message type")
        });
    });
});

describe(onServerClose, () => {
    const result = onServerClose(mockState());

    it("resets server stats", () => {
        expect(result).toContainEqual({
            action: "updateState",
            data: {
                serverId: null,
                server: null,
                connectedStatus: "CLOSED"
            },
        });
    });

    it("updates the leaderboard", () => {
        expect(result).toContainEqual(updateLeaderboard());
    });

    it("hides and shows correct DOM elements", () => {
        assertElementChanged(result, gameElements.messagesBox);
        assertElementChanged(result, gameElements.connectedBox);
        assertElementChanged(result, gameElements.inactiveOverlay);

        // with count 0, this asserts that the element did not change
        assertElementChanged(result, gameElements.gameOverMessage, 0);
    });

    it("adds game over message if present in state", () => {
        const testResult = onServerClose(mockState({
            gameOverMessage: "test message"
        }));
        assertElementChanged(testResult, gameElements.gameOverMessage);
    });
});

describe(onServerError, () => {
    const result = onServerError(mockState());

    it("returns events to close server", () => {
        // generic event returned by onServerClose fails equality check
        const closeServerEvent = onServerClose(mockState())[0];
        expect(result).toContainEqual(closeServerEvent);
    });

    it("returns connection error", () => {
        expect(result).toContainEqual({
            action: "addError",
            data: "Connection error",
        });
        expect(result).toContainEqual({
            action: "updateState",
            data: { connectedStatus: "ERROR" },
        });
    });

    it("updates errorBox element", () => {
        assertElementChanged(result,gameElements.errorBox);
    });
});

describe(addErrorNotification, () => {
    it("adds and removes error after timeout", () => {
        const result = addErrorNotification(mockState(), "test error");
        expect(result).toContainEqual({
            action: "addError",
            data: "test error",
        });
        expect(result).toContainEqual({
            action: "removeError",
            data: { delayMs: 5000 },
        });
    });
});
