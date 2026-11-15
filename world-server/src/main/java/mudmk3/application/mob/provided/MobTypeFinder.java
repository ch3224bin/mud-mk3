package mudmk3.application.mob.provided;

import mudmk3.domain.mob.MobType;

import java.util.List;

/** 몹 유형을 찾는다 */
public interface MobTypeFinder {

    /** 드롭 표까지 읽힌 몹 유형. 없으면 {@code IllegalArgumentException("몹을 찾을 수 없습니다. id: " + id)} */
    MobType find(Long mobTypeId);

    /** 트랜잭션 안에서 만든 몹 보기 값 */
    MobTypeView view(Long mobTypeId);

    /** 이름순 */
    List<MobTypeView> viewAll();
}
