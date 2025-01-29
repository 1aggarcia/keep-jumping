package io.github.aggarcia.clients.updates;

import static io.github.aggarcia.messages.Serializer.serialize;

import java.util.List;
import java.util.Optional;

import io.github.aggarcia.messages.Generated.JoinReply;
import io.github.aggarcia.messages.Generated.SocketMessage;
import io.github.aggarcia.models.GamePlatform;
import io.github.aggarcia.models.GameStore;
import io.github.aggarcia.models.PlayerStore;

public record CreateFirstPlayer(
    String client,
    PlayerStore player,
    List<GamePlatform> platforms,
    String serverId
) implements GameUpdate {
    @Override
    public void applyTo(GameStore store) {
        store.players().put(client, player);
        store.platforms(platforms);
        store.tiggerStartEvent();
    }

    @Override
    public Optional<byte[]> reply() {
        var reply = JoinReply.newBuilder().setServerId(serverId);
        var wrappedReply =
            SocketMessage.newBuilder().setJoinReply(reply).build();
        return Optional.of(serialize(wrappedReply));
    }
}
