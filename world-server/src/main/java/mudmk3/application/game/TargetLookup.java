package mudmk3.application.game;

import mudmk3.domain.combat.MobInstance;

import static java.util.Objects.requireNonNull;

/** 같은 방에서 칠 몹을 찾은 결과: 찾은 몹 또는 거절 문구({@link CombatActions#findTarget}) */
sealed interface TargetLookup {

    record Found(MobInstance mob) implements TargetLookup {
        public Found {
            requireNonNull(mob);
        }
    }

    record Rejected(String text) implements TargetLookup {
        public Rejected {
            requireNonNull(text);
        }
    }
}
