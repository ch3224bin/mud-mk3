package mudmk3.domain.combat;

/** 피해와 기혈 수치를 다루는 계산 */
public final class Damage {

    private Damage() {
    }

    /** 게이지에 보일 정수. 0 초과 1 미만이면 1(살아 있는데 0으로 보이지 않게), 그 밖은 소수를 버린다 */
    public static int gauge(double current) {
        if (current > 0 && current < 1) {
            return 1;
        }
        return (int) Math.floor(current);
    }
}
