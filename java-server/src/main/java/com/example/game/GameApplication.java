package com.example.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.game.demo.MyHandler;

@SpringBootApplication
@EnableWebSocket
public class GameApplication implements WebSocketConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/").setAllowedOrigins("*");
    }

    @Bean
    public MyHandler myHandler() {
        return new MyHandler();
    }
}
