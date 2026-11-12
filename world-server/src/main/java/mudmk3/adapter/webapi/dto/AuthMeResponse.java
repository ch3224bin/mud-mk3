package mudmk3.adapter.webapi.dto;

import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.domain.character.PlayerCharacter;
import org.jspecify.annotations.Nullable;

/** 로그인한 계정. characterName 은 캐릭터가 없으면 null */
public record AuthMeResponse(Long id, String email, boolean admin, @Nullable String characterName) {

    public static AuthMeResponse of(AccountPrincipal principal, @Nullable PlayerCharacter character) {
        return new AuthMeResponse(principal.accountId(), principal.email(), principal.admin(),
                character == null ? null : character.getName().value());
    }
}
