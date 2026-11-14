package mudmk3.application.character.provided;

import mudmk3.domain.shared.Realm;

/** 관리 툴이 캐릭터의 경지를 바꿨다(커밋 뒤 게임이 접속 중인 그 캐릭터에게 알린다) */
public record RealmChangedEvent(Long characterId, Realm from, Realm to) {
}
