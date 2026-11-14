package mudmk3.application.character.provided;

import mudmk3.domain.shared.Realm;

/** 캐릭터의 경지를 바꾸는 기능을 제공한다 */
public interface CharacterRealmEditor {

    /**
     * 관리 툴의 경지 지정. 경지가 바뀌면 {@link RealmChangedEvent} 를 내고, 같으면 아무것도 하지 않는다.
     * 없는 캐릭터면 {@code IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)}
     */
    void designate(Long characterId, Realm realm);

    /** 입문: 범인일 때만 연기 1층으로 올린다(이벤트를 내지 않는다). 범인이 아니면 그대로 둔다 */
    void initiate(Long characterId);
}
