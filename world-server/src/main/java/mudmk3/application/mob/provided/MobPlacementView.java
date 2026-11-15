package mudmk3.application.mob.provided;

import mudmk3.domain.mob.MobPlacement;

import static java.util.Objects.requireNonNull;

/** 몹 배치 보기 값(admin-tool.md {@code MobPlacementResponse} 칸): 트랜잭션 안에서 만들어 밖에서도 읽을 수 있다 */
public record MobPlacementView(Long id, Long roomId, Long mobId, String mobName, int count) {

    public static MobPlacementView of(MobPlacement placement) {
        return new MobPlacementView(requireNonNull(placement.getId()), placement.getRoomId(),
                requireNonNull(placement.getMobType().getId()), placement.getMobType().getName(), placement.getCount());
    }
}
