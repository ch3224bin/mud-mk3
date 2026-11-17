package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.domain.shared.Realm;
import org.springframework.stereotype.Component;

/** 캐릭터의 지금 경지. 메모리에 두지 않고 부를 때마다 DB에서 읽는다(관리 툴 지정이 바로 보이게) */
@Component
@RequiredArgsConstructor
class CharacterRealms {

    private final CharacterFinder characterFinder;

    Realm realm(Long characterId) {
        return characterFinder.find(characterId).getRealm();
    }

    /** 저물탁 칸이 열렸는지(경지가 축기 초기 이상) */
    boolean braceletOpen(Long characterId) {
        return realm(characterId).opensBracelet();
    }
}
