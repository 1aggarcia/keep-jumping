package io.github.aggarcia.leaderboard;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;


public class LeaderboardService {
    // TODO: Add a dynamic SQL table name (instead of hardcoding 'Leaderboard')
    // so that different leaderboards can be used for dev and prod

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds the top 10 entries in the leaderboard, sorted by score.
     */
    public List<LeaderboardEntry> getTop10() throws DataAccessException {
        return jdbcTemplate.query(
            "SELECT * FROM Leaderboard ORDER BY score DESC LIMIT 10",
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
    @Transactional
    public void update(LeaderboardEntry newEntry) throws DataAccessException {
        Integer existingScore;
        try {
            // this may be null according to the JDBC spec, or it may throw
            existingScore = jdbcTemplate.queryForObject(
                "SELECT score FROM Leaderboard WHERE player = ?",
                Integer.class,
                newEntry.player()
            );
        } catch (EmptyResultDataAccessException e) {
            existingScore = null;
        }

        if (existingScore == null) {
            jdbcTemplate.update(
                "INSERT INTO Leaderboard VALUES (?, ?, ?)",
                newEntry.player(),
                newEntry.score(),
                newEntry.timestamp()
            );
        } else if (newEntry.score() > existingScore) {
            jdbcTemplate.update(
                "UPDATE Leaderboard SET score=?, timestamp=? WHERE player=?",
                newEntry.score(),
                newEntry.timestamp(),
                newEntry.player()
            );
        }

        // TODO: cleanup lowest scores when there are more than 10 entries
    }
}
