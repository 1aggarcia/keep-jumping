import t from "tap";
import { clientBridge, ClientBridge } from "./clientBridge";
import { onClientMessage } from "./clientHandler";
import { LocalGameRepository } from "../game/localGameRepository";

t.test("handleClientMessage broadcasts message", t => {
    const game = new LocalGameRepository();
    let announcement = {};

    const mockClientBridge: ClientBridge = {
        ...clientBridge,
        broadcast: (message) => {
            announcement = message as object;
        },
    };

    onClientMessage(
        "0",
        Buffer.from(JSON.stringify({ type: "unknown"})),
        mockClientBridge, game
    );
    t.matchStrict(announcement, {
        type: "serverError",
        message: "Unsupported message type: unknown",
    });
    t.end();
});
