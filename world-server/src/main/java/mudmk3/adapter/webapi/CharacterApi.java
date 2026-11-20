package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.adapter.security.SessionAuthenticator;
import mudmk3.adapter.webapi.dto.CharacterDetailResponse;
import mudmk3.adapter.webapi.dto.CharacterSummaryResponse;
import mudmk3.adapter.webapi.dto.RealmDesignateRequest;
import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.character.provided.DeletedCharacter;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.provided.OfflineCharacterRemover;
import mudmk3.application.game.provided.PresenceFinder;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Set;

/** 관리: 캐릭터 목록·상세(접속 표시), 경지 지정, 삭제 */
@Slf4j
@WebApiAdapter
@RequestMapping("/api/admin/characters")
@RequiredArgsConstructor
public class CharacterApi {

    private final CultivationFinder cultivationFinder;
    private final CharacterRealmEditor characterRealmEditor;
    private final PresenceFinder presenceFinder;
    private final OfflineCharacterRemover offlineCharacterRemover;

    @GetMapping
    public List<CharacterSummaryResponse> list() {
        Set<Long> online = presenceFinder.onlineCharacterIds();
        return cultivationFinder.summaries().stream()
                .map(summary -> CharacterSummaryResponse.of(summary, online.contains(summary.id())))
                .toList();
    }

    @GetMapping("/{characterId}")
    public CharacterDetailResponse find(@PathVariable Long characterId) {
        return CharacterDetailResponse.of(cultivationFinder.profile(characterId));
    }

    /** 지정한 뒤 다시 읽어 바뀐 경지·스탯을 돌려준다 */
    @PutMapping("/{characterId}/realm")
    public CharacterDetailResponse designateRealm(@PathVariable Long characterId,
                                                  @RequestBody @Valid RealmDesignateRequest request) {
        characterRealmEditor.designate(characterId, request.realm());
        return CharacterDetailResponse.of(cultivationFinder.profile(characterId));
    }

    /** 포트가 돌아오면 지우기가 커밋된 뒤라 그때 삭제 기록을 남긴다(거절된 요청은 남기지 않음) */
    @DeleteMapping("/{characterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long characterId, Authentication authentication) {
        DeletedCharacter deleted = offlineCharacterRemover.delete(characterId);
        AccountPrincipal admin = SessionAuthenticator.principalOf(authentication).orElseThrow();
        log.info("관리자 삭제 — 캐릭터 id: {}, 이름: {}, 계정 id: {}, 관리자 계정 id: {}, 이메일: {}",
                deleted.id(), deleted.name(), deleted.accountId(), admin.accountId(), admin.email());
    }
}
