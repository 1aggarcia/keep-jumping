package io.github.aggarcia.shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.aggarcia.game.GamePing;
import io.github.aggarcia.players.events.ControlChangeEvent;
import io.github.aggarcia.players.events.JoinEvent;

/**
 * Base class for all event, reply, and ping messages.
 * Subclasses automatically include type information for polymorphic
 * serialization and deserialization.
 *
 * IMPORTANT: to add new messages, make sure to add a @JsonSubTypes.Type
 * declaration below so that Jackson can use the name to serialize/deserialize
 * the message as JSON
 */
@JsonTypeInfo(
    use=Id.NAME,
    include=As.PROPERTY,
    property="type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = ControlChangeEvent.class, name = "ControlChangeEvent"),
    @JsonSubTypes.Type(value = GamePing.class, name = "GamePing"),
    @JsonSubTypes.Type(value = JoinEvent.class, name = "JoinEvent")
})
public interface SocketMessage {}
