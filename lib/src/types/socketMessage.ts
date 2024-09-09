/**
 * Type definitions for socket messages
 *
 * "Game" updates are sent from server to client,
 * "Player" updates from client to server
 */

import { PlayerControl, PlayerState } from ".";

export type SocketMessage =
    | GameUpdate
    | GameJoinUpdate
    | GameOverUpdate
    | PlayerControlUpdate
    | ServerError
;

export type GameUpdate = {
    type: "gameUpdate";
    serverAge: number;
    players: PlayerState[];
};

export type GameJoinUpdate = {
    type: "gameJoinUpdate";
    playerId: string;
    serverAge: number;
    players: PlayerState[];
};

export type GameOverUpdate = {
    type: "gameOverUpdate";
    reason: string;
};

export type PlayerControlUpdate = {
    type: "playerControlUpdate";
    pressedControls: PlayerControl[];
};

export type ServerError = {
    type: "serverError";
    message: string;
};
