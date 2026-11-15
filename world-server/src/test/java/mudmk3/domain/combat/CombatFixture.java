package mudmk3.domain.combat;

import mudmk3.domain.mob.MobFixture;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.mob.MobTypeSaveInfo;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/** 교전 도메인 테스트용 몹 프로필과 몹 인스턴스 */
public final class CombatFixture {

    private CombatFixture() {
    }

    /** 청모랑(id 2): 기혈 49, 육신 8, 위력 8, 재등장 90초 */
    public static MobProfile wolfProfile() {
        return profile(MobFixture.wolf(), 2L);
    }

    /** 들토끼(id 1): 기혈 42, 육신 7, 위력 7 */
    public static MobProfile rabbitProfile() {
        return profile(MobFixture.rabbit(), 1L);
    }

    public static MobProfile profile(MobTypeSaveInfo info, Long id) {
        MobType type = MobFixture.create(info);
        ReflectionTestUtils.setField(type, "id", id);
        return MobProfile.of(type);
    }

    /** 방 10, 배치 100 첫 자리의 청모랑 */
    public static MobInstance wolf(Instant now) {
        return MobInstance.spawn(1, 100L, 0, 10L, 1, wolfProfile(), now);
    }
}
