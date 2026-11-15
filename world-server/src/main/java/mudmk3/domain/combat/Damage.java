package mudmk3.domain.combat;

/** 피해와 기혈 수치를 다루는 계산 */
public final class Damage {

    private static final double BODY_FACTOR = 0.5;
    private static final double POWER_BASE = 3;
    private static final double POWER_PER_SPIRIT = 1.8;

    private Damage() {
    }

    /** 육신 층을 거친 피해 = 위력 − 육신 × 0.5, 0 아래로 내려가지 않는다(룰북 피해 흐름) */
    public static double afterBody(double power, int body) {
        return Math.max(0, power - body * BODY_FACTOR);
    }

    /** 교전 줄의 피해: 0 은 {@code 0}, 0 초과 0.5 미만은 {@code 1 미만}, 그 밖은 반올림한 정수(룰북 소수점) */
    public static String display(double damage) {
        if (damage > 0 && damage < 0.5) {
            return "1 미만";
        }
        return Long.toString(Math.round(damage));
    }

    /** 게이지에 보일 정수. 0 초과 1 미만이면 1(살아 있는데 0으로 보이지 않게), 그 밖은 소수를 버린다 */
    public static int gauge(double current) {
        if (current > 0 && current < 1) {
            return 1;
        }
        return (int) Math.floor(current);
    }

    /** 5차 임시 공격 위력 = 3 + 1.8 × 영력 최대치(룰북 몹 스탯의 P) */
    public static double playerPower(int spirit) {
        return POWER_BASE + POWER_PER_SPIRIT * spirit;
    }
}
