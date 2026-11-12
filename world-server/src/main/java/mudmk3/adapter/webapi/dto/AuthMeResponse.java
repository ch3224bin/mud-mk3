package mudmk3.adapter.webapi.dto;

import mudmk3.application.account.provided.AccountPrincipal;

/** 로그인한 계정. characterName 은 캐릭터가 없으면 null */
public record AuthMeResponse(Long id, String email, boolean admin, String characterName) {

    public static AuthMeResponse of(AccountPrincipal principal) {
        return new AuthMeResponse(principal.accountId(), principal.email(), principal.admin(), null);
    }
}
