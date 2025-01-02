package io.github.aggarcia.shared;

import static io.github.aggarcia.shared.Serializer.deserialize;
import static io.github.aggarcia.shared.Serializer.serialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.github.aggarcia.generated.SocketMessageOuterClass.JoinEvent;
import io.github.aggarcia.generated.SocketMessageOuterClass.SocketMessage;

public class SerializerTest {
    @Test
    void test_serialize_hasExpectedSize() {
        var message = testMessage();

        byte[] result = serialize(message);
        assertEquals(message.getSerializedSize(), result.length);
    }

    @Test
    void test_serialize_deserializeReturnsOriginalResult() {
        var message = testMessage();

        byte[] serialized = serialize(message);
        Optional<SocketMessage> deserialized = deserialize(serialized);

        assertTrue(deserialized.isPresent());
        assertEquals(message, deserialized.get());
    }

    @Test
    void test_serialize_isDeterministic() {
        byte[] lastResult = serialize(testMessage());

        for (int i = 0; i < 100; i++) {
            byte[] result = serialize(testMessage());
            assertArrayEquals(result, lastResult);
            lastResult = result;
        }
    }

    @Test
    void test_deserialize_badData_returnsEmpty() {
        // create random bytes
        byte[] badData = new byte[100];
        new Random().nextBytes(badData);
        
        Optional<SocketMessage> result = deserialize(badData);
        assertTrue(result.isEmpty());
    }

    private SocketMessage testMessage() {
        var joinEvent = JoinEvent
            .newBuilder()
            .setName("testName")
            .build();
        return SocketMessage
            .newBuilder()
            .setJoinEvent(joinEvent)
            .build();
    }
}
