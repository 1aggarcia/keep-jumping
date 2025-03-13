package io.github.aggarcia.leaderboard;

import java.sql.Timestamp;

public record LeaderboardEntry(
    String player,
    int score,
    Timestamp timestamp
) {}
