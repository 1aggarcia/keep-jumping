import { GamePlatform, PlayerControl, PlayerState } from "./models";

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
    platforms: GamePlatform[];
};

export type GameJoinUpdate = {
    type: "gameJoinUpdate";
    playerId: string;
    serverAge: number;
    players: PlayerState[];
    platforms: GamePlatform[];
};

export type GameOverUpdate = {
    type: "gameOverUpdate";
    reason: string;
};

export type PlayerControlUpdate = {
    type: "playerControlUpdate";
    pressedControls: PlayerControl[];  // player control
};

export type ServerError = {
    type: "serverError";
    message: string;
};
