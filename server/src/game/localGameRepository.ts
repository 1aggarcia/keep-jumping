import { GameUpdate } from "@lib/types";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";
import { GameRepository } from "./gameRepository";
import { Player, playerToPlayerState } from "./player";
import { randomHexColor, randomInt } from "../util/random";

let nextId = 0;

export class LocalGameRepository implements GameRepository {
    private playerStore = new Map<string, Player>();
    public age = 0;
    public isRunning = false;

    createPlayer = (clientId: string) => {
        const playerId = getNextId();
        if (this.playerStore.has(clientId)) {
            throw new ReferenceError(`Id already exists: ${clientId}`);
        }
        const newPlayer: Player = {
            id: playerId,
            color: randomHexColor(),
            position: [
                randomInt(GAME_WIDTH, PLAYER_WIDTH),
                randomInt(GAME_HEIGHT, PLAYER_HEIGHT),
            ],
            velocity: [0, 0],
            age: 0,
        };
        this.playerStore.set(clientId, newPlayer);
        return newPlayer;
    };

    deletePlayer = (clientId: string) => this.playerStore.delete(clientId);

    getAllPlayersState = () => {
        return this.getAllPlayers().map(playerToPlayerState);
    };

    getAllPlayers = () => Array.from(this.playerStore.values());

    getPlayer = (clientId: string) => this.playerStore.get(clientId) ?? null;

    playerCount = () => this.playerStore.size;

    getUpdate = (): GameUpdate => ({
        type: "gameUpdate",
        serverAge: this.age,
        players: this.getAllPlayersState(),
    });
};

function getNextId() {
    nextId++;
    return String(nextId - 1);
}
