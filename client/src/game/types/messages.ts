import { GamePlatform, PlayerControl, PlayerState } from "./models";

export type SocketMessage =
    | GameUpdate
    | GameJoinUpdate
    | GameOverUpdate
    | PlayerControlUpdate
    | PlayerJoinUpdate
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
    pressedControls: PlayerControl[];
};

export type PlayerJoinUpdate = {
    type: "playerJoinUpdate";
    name: string;
}

export type ServerError = {
    type: "serverError";
    message: string;
};
