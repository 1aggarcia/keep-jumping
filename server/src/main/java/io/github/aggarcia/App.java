package io.github.aggarcia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.github.aggarcia.game.GameLoop;
import io.github.aggarcia.game.GameStore;
import io.github.aggarcia.networking.ConnectionHandler;

@SpringBootApplication
@EnableWebSocket
public class App implements WebSocketConfigurer {
    private static final int IDLE_TIMEOUT_SECONDS = 15 * 60;  // 15 minutes

    private final GameStore store = new GameStore();

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void
    registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(connectionHandler(), "/").setAllowedOrigins("*");
    }

    @Bean
    public ConnectionHandler connectionHandler() {
        return new ConnectionHandler(store);
    }

    @Bean
    public GameLoop gameLoop() {
        var loop = new GameLoop(store);
        loop.onIdleTimeout(() -> {
            System.out.println(
                "Shutting down: Idle timeout reached ("
                + IDLE_TIMEOUT_SECONDS + "s)"
            );
            System.exit(0);
        }, IDLE_TIMEOUT_SECONDS * 1000);

        store.onStartEvent(loop::start);
        return loop;
    }
}
