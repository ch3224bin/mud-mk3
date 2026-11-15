package mudmk3.domain.character;

import mudmk3.domain.shared.Realm;

import static java.util.Objects.requireNonNull;

/**
 * 깎인 법력 계산. 저장하는 값은 법력 최대치에서 깎인 양(소수 그대로)이고, 현재 법력 = 최대 − 깎인 양(0 아래로 내려가지 않음)이다
 */
public final class Mana {

    /** 초마다 채우는 몫(최대치 × 0.5%, 영기 농도 보통) */
    public static final double REGEN_RATE_PER_SECOND = 0.005;

    private static final int MANA_PER_SPIRIT = 5;

    private Mana() {
    }

    /** 법력을 쓴 뒤 깎인 양. 최대를 넘지 않고, 음수는 0 으로 본다 */
    public static double lostAfterSpend(double lost, double amount, int max) {
        return Math.min(max, lost + Math.max(0, amount));
    }

    /** 법력을 채운 뒤 깎인 양. 0 아래로 내려가지 않고(최대를 넘는 몫은 버림), 음수는 0 으로 본다 */
    public static double lostAfterRecover(double lost, double amount) {
        return Math.max(0, lost - Math.max(0, amount));
    }

    /**
     * 경지가 바뀐 뒤 깎인 양. 범인(영력 0)이 되면 0. 영력이 줄면 최대치의 경지 몫이 준 만큼(영력 차 × 5) 깎인 양도 줄인다
     * (0 아래로는 아님, 공법 몫은 보지 않음). 영력이 같거나 늘면 그대로다
     */
    public static double lostAfterDemotion(double lost, Realm from, Realm to) {
        requireNonNull(from);
        requireNonNull(to);
        if (to.spirit() == 0) {
            return 0;
        }
        if (to.spirit() >= from.spirit()) {
            return lost;
        }
        return Math.max(0, lost - (double) (from.spirit() - to.spirit()) * MANA_PER_SPIRIT);
    }

    /** 현재 법력 = 최대 − 깎인 양, 0 아래로 내려가지 않는다 */
    public static double current(double lost, int max) {
        return Math.max(0, max - lost);
    }
}
