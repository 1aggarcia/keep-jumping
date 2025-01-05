package io.github.aggarcia.clients.updates;

import static io.github.aggarcia.messages.Serializer.serialize;

import java.util.Optional;

import io.github.aggarcia.messages.Generated.ErrorReply;
import io.github.aggarcia.messages.Generated.SocketMessage;
import io.github.aggarcia.models.GameStore;

public record ErrorUpdate(
    byte[] data
) implements GameUpdate {

    /**
     * Factory function to serialize a text message as a JSON ErorReply.
     * @param message
     * @return ErrorUpdate with ErrorReply serializes as a String
     */
    public static ErrorUpdate fromText(String message) {
        var reply = ErrorReply.newBuilder().setMessage(message).build();
        var wrappedReply = SocketMessage
            .newBuilder()
            .setErrorReply(reply)
            .build();

        return new ErrorUpdate(serialize(wrappedReply));
    }

    @Override
    public Optional<byte[]> reply() {
        return Optional.of(data);
    }

    @Override
    public void applyTo(GameStore store) {
        // do nothing
    }
}
