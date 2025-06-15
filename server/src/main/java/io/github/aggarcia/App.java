package io.github.aggarcia;


import static io.github.aggarcia.messages.Serializer.serialize;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import io.github.aggarcia.clients.ClientHandler;
import io.github.aggarcia.engine.GameLoop;
import io.github.aggarcia.leaderboard.LeaderboardService;
import io.github.aggarcia.models.GameStore;
import io.github.aggarcia.models.OutgoingEvent;
import io.github.aggarcia.models.PlayerStore;
import jakarta.annotation.PostConstruct;

/**
 * To deploy to GCP
 * https://cloud.google.com/run/docs/deploying-source-code#deploying
 * ./mvnw clean package
 * gcloud run deploy server --source .
 * region 39
 */

@SpringBootApplication
@CrossOrigin
@RestController
@EnableWebSocket
public class App implements WebSocketConfigurer {
    private static final int IDLE_TIMEOUT_SECONDS = 15 * 60;  // 15 minutes

    private final GameStore store = new GameStore();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @PostConstruct
    void init() {
        // I hate java sometimes
        new Thread(this::processLosers).start();
        new Thread(this::processOutgoingEvents).start();
    }

    @Override
    public void
    registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(clientHandler(), "/").setAllowedOrigins("*");
    }

    @Bean
    public LeaderboardService leaderboardService() {
        return new LeaderboardService(jdbcTemplate);
    }

    @Bean
    public ClientHandler clientHandler() {
        return new ClientHandler(store);
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

    /**
     * Should be run on a separate thread. Consumes players from the loser
     * queue and saves their stats.
     */
    void processLosers() {
        while (true) {
            try {
                PlayerStore nextLoser = store.unprocessedLosers().take();
                var entry = nextLoser.createLeaderboardEntry();
                leaderboardService().update(entry);
            } catch (DataAccessException e) {
                System.err.println(e);
            } catch (InterruptedException e) {
                System.err.println("Loser worker thread interrupted");
                break;
            }
        }
    }

    /**
     * Should be run on a separate thread. Consumes events from the event
     * queue to send out to clients.
     */
    void processOutgoingEvents() {
        while (true) {
            try {
                OutgoingEvent nextEvent = store.outgoingEvents().take();
                var message = new BinaryMessage(serialize(nextEvent.event()));
                System.out.println("Sending outgoing event: " + message);
                WebSocketSession session = nextEvent.client();
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (InterruptedException e) {
                System.err.println("Outgoing event thread interrupted");
                break;
            } catch (Exception e) {
                System.err.println("Error sending event: " + e);
            }
        }
    }

    @GetMapping("/api/leaderboard")
    ResponseEntity<?> getLeaderboard() {
        try {
            var top10 = leaderboardService().getTop10();
            return new ResponseEntity<>(top10, HttpStatus.OK);
        } catch (DataAccessException e) {
            System.err.println(e);
            return new ResponseEntity<>(
                e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/health")
    String getRoot() {
        return "Keep Jumping server is alive";
    }
}
