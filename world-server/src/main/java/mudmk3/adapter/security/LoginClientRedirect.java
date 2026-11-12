package mudmk3.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mudmk3.support.config.AuthProperties;
import mudmk3.support.stereotype.Adapter;
import org.jspecify.annotations.Nullable;

/**
 * 로그인을 시작한 클라이언트(play/admin)를 세션에 적어 두었다가 로그인 뒤 돌아갈 주소를 정한다.
 * 정해 둔 두 주소로만 보낸다(열린 리다이렉트 방지). 모르는 값은 play 로 본다.
 */
@Adapter
@RequiredArgsConstructor
public class LoginClientRedirect {

    public static final String SESSION_ATTRIBUTE = "MUD_LOGIN_CLIENT";

    static final String ADMIN = "admin";
    static final String PLAY = "play";

    private final AuthProperties authProperties;

    public void remember(HttpServletRequest request, @Nullable String client) {
        request.getSession().setAttribute(SESSION_ATTRIBUTE, ADMIN.equals(client) ? ADMIN : PLAY);
    }

    /** 돌아갈 클라이언트 주소. 세션에 적어 둔 값은 지운다 */
    public String takeTargetUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object client = null;
        if (session != null) {
            client = session.getAttribute(SESSION_ATTRIBUTE);
            session.removeAttribute(SESSION_ATTRIBUTE);
        }
        return ADMIN.equals(client) ? authProperties.adminConsoleUrl() : authProperties.playClientUrl();
    }
}
