package mudmk3.domain.character;

import mudmk3.domain.shared.Realm;

import static java.util.Objects.requireNonNull;

/**
 * 깎인 기혈 계산. 저장하는 값은 최대 기혈에서 깎인 양(소수 그대로)이고, 현재 기혈 = 최대 − 깎인 양(0 아래로 내려가지 않음)이다
 */
public final class QiBlood {

    private static final int QI_BLOOD_PER_BODY = 10;

    private QiBlood() {
    }

    /** 피해를 받은 뒤 깎인 양. 최대를 넘지 않는다 */
    public static double lostAfterDamage(double lost, double damage, int max) {
        return Math.min(max, lost + damage);
    }

    /** 현재 기혈을 그 값으로 맞출 때의 깎인 양(0~최대) */
    public static double lostFor(double current, int max) {
        return Math.clamp(max - current, 0, max);
    }

    /**
     * 경지가 바뀐 뒤 깎인 양. 육신이 줄면 최대 기혈이 준 만큼(육신 차 × 10) 깎인 양도 줄인다(0 아래로는 아님).
     * 육신이 같거나 늘면 그대로라 현재 기혈이 최대가 는 만큼 오른다
     */
    public static double lostAfterDemotion(double lost, Realm from, Realm to) {
        requireNonNull(from);
        requireNonNull(to);
        if (to.body() >= from.body()) {
            return lost;
        }
        return Math.max(0, lost - (double) (from.body() - to.body()) * QI_BLOOD_PER_BODY);
    }

    /** 현재 기혈 = 최대 − 깎인 양, 0 아래로 내려가지 않는다 */
    public static double current(double lost, int max) {
        return Math.max(0, max - lost);
    }
}
