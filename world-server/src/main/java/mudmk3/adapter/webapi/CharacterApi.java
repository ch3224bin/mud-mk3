package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.CharacterDetailResponse;
import mudmk3.adapter.webapi.dto.CharacterSummaryResponse;
import mudmk3.adapter.webapi.dto.RealmDesignateRequest;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.game.provided.PresenceFinder;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

/** 관리: 캐릭터 목록·상세(접속 표시)와 경지 지정 */
@WebApiAdapter
@RequestMapping("/api/admin/characters")
@RequiredArgsConstructor
public class CharacterApi {

    private final CultivationFinder cultivationFinder;
    private final CharacterRealmEditor characterRealmEditor;
    private final PresenceFinder presenceFinder;

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
}
