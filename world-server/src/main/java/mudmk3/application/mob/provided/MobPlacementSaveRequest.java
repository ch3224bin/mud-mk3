package mudmk3.application.mob.provided;

/**
 * 방에 몹을 두거나 고치는 요청. 몹 id가 없으면 만들 때 {@code IllegalArgumentException} 을 던져, JSON 을 읽는 중이면
 * 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 되게 한다. 수 범위(1~10)는 도메인이 본다
 */
public record MobPlacementSaveRequest(Long mobId, int count) {

    public MobPlacementSaveRequest {
        MobTypeSaveRequest.required(mobId, "mobId");
    }
}
