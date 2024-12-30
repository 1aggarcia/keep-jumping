package io.github.aggarcia.game;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.WantedButNotInvoked;

public class GameStoreTest {
    @Test
    void test_onStartEvent_doesNotCallStartAction() {
        Runnable mockAction = Mockito.spy(Runnable.class);
        var store = new GameStore();

        store.onStartEvent(mockAction);
        assertThrows(
            WantedButNotInvoked.class,
            () -> Mockito.verify(mockAction).run()
        );
    }

    @Test
    void test_triggerStartEvent_callsStartAction() {
        Runnable mockAction = Mockito.spy(Runnable.class);
        var store = new GameStore();

        store.onStartEvent(mockAction);
        store.tiggerStartEvent();
        Mockito.verify(mockAction).run();
    }
}
