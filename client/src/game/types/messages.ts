/**
 * Type definitions for messages sent over the network.
 * A message is one of the following types:
 *
 * - Ping: from server to client, send at a fixed interval
 * - Event: can be sent both ways, triggered by some event
 * - Reply: optional response to an event
 */

import { GamePlatform, PlayerControl, PlayerState } from "./models";

export type SocketMessage =
    | GamePing
    | ControlChangeEvent
    | JoinEvent
    | GameOverEvent
    | ErrorReply
;


// PINGS

export type GamePing = {
    type: "GamePing";
    serverAge: number;
    players: PlayerState[];
    platforms: GamePlatform[];
};


// EVENTS

/** client to server */
export type ControlChangeEvent = {
    type: "ControlChangeEvent";
    pressedControls: PlayerControl[];
};

/** client to server */
export type JoinEvent = {
    type: "JoinEvent";
    name: string;
}

/** server to client */
export type GameOverEvent = {
    type: "GameOverEvent";
    reason: string;
};


// REPLIES

/** server to client */
export type ErrorReply = {
    type: "ErrorReply";
    message: string;
};
