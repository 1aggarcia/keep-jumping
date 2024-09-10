import { PlayerControl, PlayerState } from "@lib/types";
import {
    GAME_HEIGHT,
    GAME_WIDTH,
    PLAYER_HEIGHT,
    PLAYER_WIDTH
} from "./constants";

/** Movement speed in pixels per second */
export const PLAYER_SPEED = 10;

export type Player = {
    id: string;
    color: string;
    age: number;

    /** [x, y] */
    position: [number, number];

    /**
     * - velocity[0] = change in x per second
     * - velocity[1] = change in y per second
    */
    velocity: [number, number];
};

/**
 * Changes the position of the player passed in according to its current
 * velocity.
 * If the player is at the game boundary, the player is not moved.
 * @param player to move
 */
export function movePlayer(player: Player) {
    const newX = player.position[0] + player.velocity[0];
    const newY = player.position[1] + player.velocity[1];

    if (0 <= newX && newX + PLAYER_WIDTH <= GAME_WIDTH) {
        player.position[0] = newX;
    }
    if (0 <= newY && newY + PLAYER_HEIGHT <= GAME_HEIGHT) {
        player.position[1] = newY;
    }
}

/**
 * Given a set of pressed controls, find and return the player's velocity.
 * If two conflicting controls, such as "Up" and "Down" are passed in, behavior
 * is undefined.
 * @param pressedControls A list of pressed controls. Duplicates are ignored.
 * @returns velocity as a tuple [dx, dy]
 */
export function
getPlayerVelocity(pressedControls: PlayerControl[]): [number, number] {
    const controlSet = new Set(pressedControls);
    let xVelocity = 0;
    let yVelocity = 0;

    for (const control of controlSet) {
        if (control === "Right") {
            xVelocity = PLAYER_SPEED;
        }
        if (control === "Left") {
            xVelocity = -PLAYER_SPEED;
        }
        if (control === "Down") {
            yVelocity = PLAYER_SPEED;
        }
        if (control === "Up") {
            yVelocity = -PLAYER_SPEED;
        }
    }
    return [xVelocity, yVelocity];
}

/**
 * Removes the `velocity` paramter from a player
 * @param player complete player
 * @returns player without `velocity` param
 */
export function playerToPlayerState(player: Player): PlayerState {
    return {
        color: player.color,
        x: player.position[0],
        y: player.position[1],
        age: player.age,
    };
}
