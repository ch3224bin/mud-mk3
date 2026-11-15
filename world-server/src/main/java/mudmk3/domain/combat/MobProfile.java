package mudmk3.domain.combat;

import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobStats;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.shared.Realm;

import static java.util.Objects.requireNonNull;

/**
 * 메모리 몹이 쓰는 몹 유형의 값 복사본. 메모리 상태는 엔티티를 들고 있지 않으므로(트랜잭션 밖에서 detached) 나타날 때 복사한다
 */
public record MobProfile(Long mobTypeId, String name, String description, MobKind kind, Realm realm, MobTier tier,
                         String attackName, MobStats stats, int respawnSeconds, String rankName) {

    public MobProfile {
        requireNonNull(mobTypeId);
        requireNonNull(name);
        requireNonNull(description);
        requireNonNull(kind);
        requireNonNull(realm);
        requireNonNull(tier);
        requireNonNull(attackName);
        requireNonNull(stats);
        requireNonNull(rankName);
    }

    /** 저장된 몹 유형(id 있음)의 지금 정의 */
    public static MobProfile of(MobType type) {
        return new MobProfile(requireNonNull(type.getId(), "저장되지 않은 몹 유형입니다"), type.getName(),
                type.getDescription(), type.getKind(), type.getRealm(), type.getTier(), type.getAttackName(),
                type.stats(), type.effectiveRespawnSeconds(), type.rankName());
    }
}
