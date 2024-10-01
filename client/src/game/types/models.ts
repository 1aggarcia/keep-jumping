export type PlayerState = {
    /** CSS compatible string */
    color: string;

    /** X position */
    x: number;

    /** Y Position */
    y: number;

    /** Number of seconds since the player has joined */
    score: number;
}

export type GamePlatform = {
    x: number;
    y: number;
    width: number;
}

export type PlayerControl = "Up" | "Down" | "Left" | "Right";
