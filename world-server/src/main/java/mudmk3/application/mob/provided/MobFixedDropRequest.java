package mudmk3.application.mob.provided;

import mudmk3.domain.mob.MobFixedDrop;

/**
 * 고정 재료 한 줄 요청. 물건 종류 id가 없으면 만들 때 {@code IllegalArgumentException} 을 던져, JSON 을 읽는 중이면
 * 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 되게 한다. 수 범위는 도메인이 본다
 */
public record MobFixedDropRequest(Long itemTypeId, int count) {

    public MobFixedDropRequest {
        MobTypeSaveRequest.required(itemTypeId, "itemTypeId");
    }

    public MobFixedDrop toDrop() {
        return new MobFixedDrop(itemTypeId, count);
    }
}
