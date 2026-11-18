package mudmk3.adapter.websocket;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HttpSessionEndListenerTest {

    private final WebSocketGameMessageSender sender = mock(WebSocketGameMessageSender.class);
    private final HttpSessionEndListener listener = new HttpSessionEndListener(sender);

    @Test
    void sessionDestroyedEndsThatSession() {
        listener.sessionDestroyed(eventOf("http-1"));

        verify(sender).endHttpSession("http-1");
        verifyNoMoreInteractions(sender);
    }

    @Test
    void sessionIdChangeIsFollowed() {
        listener.sessionIdChanged(eventOf("new-id"), "old-id");

        verify(sender).renameHttpSession("old-id", "new-id");
        verifyNoMoreInteractions(sender);
    }

    private static HttpSessionEvent eventOf(String sessionId) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);
        return new HttpSessionEvent(session);
    }
}
