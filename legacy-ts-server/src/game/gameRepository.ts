import { GameUpdate, PlayerState } from "@lib/types";
import { Player } from "./player";

export interface GameRepository {
    /**
     * Number of seconds since the game repository was created
     */
    age: number;

    /**
     * `true` if the game loop is active, `false` otherwise
     */
    isRunning: boolean

    /**
     * Creates a new player in the repo, bounded to the client id passed in
     */
    createPlayer(clientId: string): Player

    /**
     * @returns `true` if player existed and was deleted, `false` otherwise
     */
    deletePlayer(clientId: string): boolean

    /**
     * @returns the state of all players in the repo
     */
    getAllPlayersState(): PlayerState[]

    /**
     * @returns list of all players
     */
    getAllPlayers(): Player[]

    /**
     * @returns the number of players
     */
    playerCount(): number

    /**
     * @returns the game state formatted to send over a web socket
     */
    getUpdate(): GameUpdate

    /**
     * @param clientId
     * @returns player, or null if none found
     */
    getPlayer(clientId: string): Player | null
};
