package mudmk3.adapter.websocket;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import mudmk3.support.stereotype.Adapter;

/**
 * HTTP 세션이 끝나면(로그아웃의 무효화, 유휴 만료) 그 세션으로 연 게임 연결을 안내 뒤 4401로 닫고,
 * 세션 id 가 바뀌면(로그인 때 세션 고정 방어) 연결이 새 id 를 따라가게 한다.
 * Spring Boot 가 이 빈을 서블릿 리스너로 등록한다.
 */
@Adapter
@RequiredArgsConstructor
public class HttpSessionEndListener implements HttpSessionListener, HttpSessionIdListener {

    private final WebSocketGameMessageSender sender;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sender.endHttpSession(event.getSession().getId());
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        sender.renameHttpSession(oldSessionId, event.getSession().getId());
    }
}
