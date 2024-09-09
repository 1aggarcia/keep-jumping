export * from "./socketMessage";

export type Context2D = CanvasRenderingContext2D;

export type PlayerControl = "Up" | "Down" | "Left" | "Right";

export type PlayerState = {
    /** should be unique */
    id: string;

    /** CSS compatible string */
    color: string;

    /** [x, y] */
    position: [number, number];

    /** Number of seconds since the player has joined */
    age: number;
}
