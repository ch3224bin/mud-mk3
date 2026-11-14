package mudmk3.adapter.webapi.dto;

import mudmk3.domain.shared.Realm;

/** 경지. {@code code} 는 enum 이름, {@code name} 은 {@code 범인}·{@code 연기 1층} 같은 화면 이름 */
public record RealmResponse(Realm code, String name) {

    public static RealmResponse of(Realm realm) {
        return new RealmResponse(realm, realm.koreanName());
    }
}
