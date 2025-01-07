package io.github.aggarcia.engine;

public final class GameConstants {
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    public static final int TICK_DELAY_MS = 20;
    public static final int MAX_TIME_SECONDS = 3600;  // 1 hour

    public static final int INIT_PLATFORM_GRAVITY = 7;
    public static final int LEVELUP_PLATFORM_GRAVITY = 2;

    /** in seconds. */
    public static final int PLATFORM_SPEEDUP_INTERVAL = 15;

    private GameConstants() {}
}
