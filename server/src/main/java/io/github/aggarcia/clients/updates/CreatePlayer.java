package io.github.aggarcia.clients.updates;

import static io.github.aggarcia.messages.Serializer.serialize;

import java.util.Optional;

import io.github.aggarcia.messages.Generated.JoinReply;
import io.github.aggarcia.messages.Generated.SocketMessage;
import io.github.aggarcia.models.GameStore;
import io.github.aggarcia.models.PlayerStore;

public record CreatePlayer(
    String client,
    PlayerStore player,
    String serverId
) implements GameUpdate {
    @Override
    public Optional<byte[]> reply() {
        var reply = JoinReply.newBuilder().setServerId(serverId);
        var wrappedReply = SocketMessage.newBuilder().setJoinReply(reply).build();
        return Optional.of(serialize(wrappedReply));
    }

    /**
     * Add the mapping `client -> player`.
     */
    @Override
    public void applyTo(GameStore store) {
        store.players().put(client, player);
    }
}
