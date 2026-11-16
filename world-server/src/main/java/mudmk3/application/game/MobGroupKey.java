package mudmk3.application.game;

import mudmk3.domain.combat.MobInstance;

/** 방 보기 몹 묶음 키(D89): 몹 유형과 교전 중인가가 같으면 한 묶음이다 */
record MobGroupKey(Long mobTypeId, boolean fighting) {

    static MobGroupKey of(MobInstance mob) {
        return new MobGroupKey(mob.getProfile().mobTypeId(), mob.isFighting());
    }
}
