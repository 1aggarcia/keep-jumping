import { PlayerControlUpdate, SocketMessage } from "@lib/types";
import { GameRepository } from "../game/gameRepository";
import WebSocket from "ws";
import { serverError } from "../error/serverError";
import { getPlayerVelocity } from "../game/player";

export function handleClientMessage(
    clientId: string,
    message: WebSocket.RawData,
    gameRepo: GameRepository
) {
    let json: SocketMessage;
    try {
        json = JSON.parse(message.toString());
    } catch (error) {
        return serverError(`JSON parse error: ${error}`);
    }
    switch (json.type) {
        case "playerControlUpdate":
            return playerUpdateResponse(clientId, json, gameRepo);
        default:
            return serverError(`Unsupported message type: ${json.type}`);
    }
}

export function handleClientDisconnect(
    oldClientId: string,
    game: GameRepository
) {
    if (!game.deletePlayer(oldClientId)) {
        console.error(`Error deleting player for client ${oldClientId}`);
    }
    if (game.playerCount() === 0) {
        game.isRunning = false;
    }
    return game.getUpdate();
}

export function playerUpdateResponse(
    clientId: string,
    playerUpdate: PlayerControlUpdate,
    game: GameRepository
) {
    if (!game.isRunning) {
        return serverError("Game is not running");
    }
    const player = game.getPlayer(clientId);
    if (player === null) {
        return serverError(`Client '${clientId}' has no associated player`);
    }
    player.velocity = getPlayerVelocity(playerUpdate.pressedControls);
    return game.getUpdate();
}
