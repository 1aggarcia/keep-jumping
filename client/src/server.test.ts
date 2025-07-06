import { beforeAll, describe, expect, it } from "vitest";
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
import { mockState, openTestConnection } from "./testUtils";
import { SocketMessage } from "./generated/socketMessage";
import { gameElements } from "./ui/dom";

describe(checkServerHealth, () => {
    it("resolves promise if fetch call is successful", async () => {
        window.fetch = () => Promise.resolve(new Response());
        expect(checkServerHealth()).resolves.toBeUndefined();
    });

    it("rejects promise with error if fetch call fails", async () => {
        window.fetch = () => Promise.reject("Test error");
        expect(checkServerHealth()).rejects.toEqual("Test error");
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
    beforeAll(() => {
        // TODO: render the real HTML instead
        document.body.innerHTML = `
            <p id='leaderboard-status'></p>
            <tbody id='leaderboard'></tbody>
        `;
    });

    it("shows error to user when API call fails", async () => {
        window.fetch = () => Promise.reject("Test error");
        await updateLeaderboard();
        expect(gameElements.leaderboardStatus.text())
            .toEqual("Failed to update leaderboard");
    });

    it("shows error to user when response contains bad status", async () => {
        window.fetch = () => Promise.resolve(new Response("Test error", {
            status: 400
        }));
        await updateLeaderboard();
        expect(gameElements.leaderboardStatus.text())
            .toEqual("Failed to update leaderboard");
    });

    it("shows error to user when server sends bad data", async () => {
        const malformedResponse = JSON.stringify([{
            player: "",
            score: "should be a number",
            timestamp: "",
        }]);
        window.fetch = () => Promise.resolve(new Response(malformedResponse));
        await updateLeaderboard();
        expect(gameElements.leaderboardStatus.text())
            .toEqual("Bad leaderboard data received from server");
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

    it("saves ping if message is gamePing", async () => {
        const blob = makeBlobMessage({
            gamePing: {
                serverAge: 3
            }
        });
        const result = await onServerMessage(blob, mockState());
        const expectedPing = SocketMessage.fromObject({
            gamePing: { serverAge: 3 }
        });
        expect(result).toContainEqual({
            action: "updateState",
            data: {
                lastPing: expectedPing.gamePing
            }
        });
    });

    it("saves message if message is gameOverEvent", async () => {
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
