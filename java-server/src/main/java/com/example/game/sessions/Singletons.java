package com.example.game.sessions;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class Singletons {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    private Singletons() {}
}
