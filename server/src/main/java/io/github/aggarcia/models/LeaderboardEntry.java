package io.github.aggarcia.models;

import java.sql.Timestamp;

public record LeaderboardEntry(
    String player,
    int score,
    Timestamp timestamp
) {}
