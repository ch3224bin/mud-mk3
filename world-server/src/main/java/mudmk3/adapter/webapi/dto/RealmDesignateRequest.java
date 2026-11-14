package mudmk3.adapter.webapi.dto;

import mudmk3.domain.shared.Realm;

/**
 * 경지 지정 요청. {@code realm} 이 빠지면 생성자에서 던져 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 된다
 * ({@code @NotNull} 의 검증 실패 문구가 아니라)
 */
public record RealmDesignateRequest(Realm realm) {

    public RealmDesignateRequest {
        if (realm == null) {
            throw new IllegalArgumentException("realm 는 꼭 보내야 합니다");
        }
    }
}
