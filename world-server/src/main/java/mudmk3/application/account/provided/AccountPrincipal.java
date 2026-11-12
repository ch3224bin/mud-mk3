package mudmk3.application.account.provided;

import java.io.Serializable;
import java.security.Principal;

/**
 * 로그인한 계정. HTTP 세션의 인증 정보로 저장되므로 Serializable 이다.
 * admin 은 로그인할 때 관리자 이메일 목록으로 정한 값이다.
 */
public record AccountPrincipal(Long accountId, String email, boolean admin) implements Principal, Serializable {

    @Override
    public String getName() {
        return String.valueOf(accountId);
    }
}
