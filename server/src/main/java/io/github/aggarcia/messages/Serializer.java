package io.github.aggarcia.messages;

import java.util.Optional;

import com.google.protobuf.InvalidProtocolBufferException;

import io.github.aggarcia.messages.Generated.SocketMessage;

// facade to separate encoding scheme from Java representation of messages
public final class Serializer {
    private Serializer() {}

    /**
     * Convert a protobuf message to a binary array.
     * @param message
     * @return serialized data
     */
    public static byte[] serialize(SocketMessage message) {
        return message.toByteArray();
    }

    /**
     * Convert a binary array to a protobuf message.
     * @param payload
     * @return protobuf message if decoding is possible, empty otherwise
     */
    public static Optional<SocketMessage> deserialize(byte[] payload) {
        try {
            return Optional.of(SocketMessage.parseFrom(payload));
        } catch (InvalidProtocolBufferException e) {
            System.err.println(e);
            return Optional.empty();
        }
    }
}
