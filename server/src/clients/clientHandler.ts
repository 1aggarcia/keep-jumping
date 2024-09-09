/** Event listeners for client events join/leave/message */

import WebSocket from "ws";
import { ClientBridge } from "./clientBridge";
import { GameJoinUpdate } from "@lib/types";
import { GameRepository } from "../game/gameRepository";
import { handleClientDisconnect, handleClientMessage } from "./eventHandler";
import { runGameLoop } from "../game/gameLoop";

/**
 * Registers client in the client bridge, creates a new player for the client,
 * sends the game state to all clients.
 * @returns id of the new client
 */
export function setUpClient(
    socket: WebSocket,
    clientBridge: ClientBridge,
    game: GameRepository
) {
    const clientId = clientBridge.addClient(socket);
    console.log(`New connection - ${clientId}`);
    const newPlayer = game.createPlayer(clientId);
    if (game.isRunning === false) {
        runGameLoop(game, clientBridge);
    }

    const response: GameJoinUpdate = {
        type: "gameJoinUpdate",
        playerId: newPlayer.id,
        serverAge: game.age,
        players: game.getAllPlayersState(),
    };
    clientBridge.send(response, clientId);
    clientBridge.broadcast(game.getUpdate());
    return clientId;
}

export function onClientMessage(
    clientId: string,
    message: WebSocket.RawData,
    clientBridge: ClientBridge,
    game: GameRepository,
) {
    const response = handleClientMessage(clientId, message, game);
    clientBridge.broadcast(response);
}

export function onClientDisconnect(
    clientId: string,
    clientBridge: ClientBridge,
    game: GameRepository
) {
    if (!clientBridge.removeClient(clientId)) {
        console.error(`Client ${clientId} does not exist`);
    }
    console.log(`Client ${clientId} disconnected`);
    const announcement = handleClientDisconnect(clientId, game);
    clientBridge.broadcast(announcement);
}
