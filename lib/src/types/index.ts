export * from "./socketMessage";

export type Context2D = CanvasRenderingContext2D;

export type PlayerControl = "Up" | "Down" | "Left" | "Right";

export type PlayerState = {
    /** CSS compatible string */
    color: string;

    /** X position */
    x: number;

    /** Y Position */
    y: number;

    /** Number of seconds since the player has joined */
    age: number;
}
