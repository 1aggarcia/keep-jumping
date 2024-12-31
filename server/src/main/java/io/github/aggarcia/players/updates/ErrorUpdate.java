package io.github.aggarcia.players.updates;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.players.replies.ErrorReply;

public record ErrorUpdate(
    String data
) implements PlayerUpdate {

    /**
     * Factory function to serialize a text message as a JSON ErorReply.
     * @param message
     * @return ErrorUpdate with ErrorReply serializes as a String
     */
    public static ErrorUpdate fromText(String message) {
        try {
            var serialized = new ObjectMapper()
                .writeValueAsString(new ErrorReply(message));

            return new ErrorUpdate(serialized);
        } catch (JsonProcessingException e) {
            // should never happen as long as jackson is set up correctly
            throw new IllegalStateException(
                "Impossible state - serialization failed: " + e);
        }
    }

    @Override
    public Optional<String> reply() {
        return Optional.of(data);
    }

    @Override
    public void applyTo(GameStore store) {
        // do nothing
    }
}
