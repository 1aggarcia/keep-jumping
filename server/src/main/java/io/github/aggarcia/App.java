package io.github.aggarcia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.github.aggarcia.networking.ConnectionHandler;

@SpringBootApplication
@EnableWebSocket
public class App implements WebSocketConfigurer {

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
        return new ConnectionHandler();
    }
}
