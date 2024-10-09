package io.github.aggarcia.players;

public record PlayerState(
    String name,

    /** CSS compatible string */
    String color,

    /** X position */
    int x,

    /** Y Position */
    int y,

    /** Number of seconds since the player has joined */
    int score
) {}
