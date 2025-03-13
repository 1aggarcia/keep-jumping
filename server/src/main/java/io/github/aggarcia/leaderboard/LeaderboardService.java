package io.github.aggarcia.leaderboard;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class LeaderboardService {
    private final JdbcTemplate jdbcTemplate;

    public LeaderboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds the top 10 entries in the leaderboard, sorted by score.
     */
    public List<LeaderboardEntry> getTop10() throws DataAccessException {
        return jdbcTemplate.query(
            "SELECT * FROM Leaderboard ORDER BY score",
            (row, i) -> new LeaderboardEntry(
                row.getString("player"),
                row.getInt("score"),
                row.getTimestamp("timestamp")
            )
        );
    }

    /**
     * Saves the leaderboard entry to the database only if the player
     * is in the top 10 by score. Overrides any previous entry for the player
     * if the new score is larger.
     */
    public void update(LeaderboardEntry entry) {
        // TODO
        throw new UnimplementedException();
    }

    class UnimplementedException extends RuntimeException {}
}
