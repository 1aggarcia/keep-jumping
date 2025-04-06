package io.github.aggarcia.leaderboard;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;


public class LeaderboardService {
    @Autowired
    private Environment env;

    @Value("${database.leaderboard}")
    private String tableName;

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds the top 10 entries in the leaderboard, sorted by score.
     */
    public List<LeaderboardEntry> getTop10() throws DataAccessException {
        verifyDatabaseCredentialsDefined();
        return jdbcTemplate.query(
            "SELECT * FROM " + tableName + " ORDER BY score DESC LIMIT 10",
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
        verifyDatabaseCredentialsDefined();

        Integer existingScore;
        try {
            // this may be null according to the JDBC spec, or it may throw
            existingScore = jdbcTemplate.queryForObject(
                "SELECT score FROM " + tableName + " WHERE player = ?",
                Integer.class,
                newEntry.player()
            );
        } catch (EmptyResultDataAccessException e) {
            existingScore = null;
        }

        if (existingScore == null) {
            jdbcTemplate.update(
                "INSERT INTO " + tableName + " VALUES (?, ?, ?)",
                newEntry.player(),
                newEntry.score(),
                newEntry.timestamp()
            );
        } else if (newEntry.score() > existingScore) {
            jdbcTemplate.update(
                "UPDATE " + tableName
                    + " SET score=?, timestamp=? WHERE player=?",
                newEntry.score(),
                newEntry.timestamp(),
                newEntry.player()
            );
        }

        // TODO: cleanup lowest scores when there are more than 10 entries
    }

    /**
     * Throws an exception if the env variables for database connection
     * are not defiend.
     */
    private void verifyDatabaseCredentialsDefined() throws DataAccessException {
        System.out.println("Table name set to " + tableName);

        final String usernameVar = "SPRING_DATASOURCE_USERNAME";
        if (env.getProperty(usernameVar) == null) {
            throw new BadCredentialsException(usernameVar + " is not defined");
        }
        System.out.println(usernameVar + " is defined");

        final String passwordVar = "SPRING_DATASOURCE_PASSWORD";
        if (env.getProperty(passwordVar) == null) {
            throw new BadCredentialsException(passwordVar + " is not defined");
        }
        System.out.println(passwordVar + " is defined");
    }

    class BadCredentialsException extends DataAccessException {
        BadCredentialsException(String message) {
            super(message);
        }
    }
}
