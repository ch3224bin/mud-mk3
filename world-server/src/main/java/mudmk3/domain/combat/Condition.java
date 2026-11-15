package mudmk3.domain.combat;

/** 기혈 상태 구간(룰북: 100~75% 멀쩡함, 75~40% 다침, 40~15% 크게 다침, 15% 미만 빈사). 이름은 {@code 상태} 줄, 문장은 {@code 보기} 줄 */
public enum Condition {
    HEALTHY("멀쩡함", "멀쩡하다."),
    HURT("다침", "다쳤다."),
    BADLY_HURT("크게 다침", "크게 다쳤다."),
    DYING("빈사", "빈사 상태다.");

    private final String koreanName;
    private final String sentence;

    Condition(String koreanName, String sentence) {
        this.koreanName = koreanName;
        this.sentence = sentence;
    }

    /** 비율 ≥ 75% 멀쩡함, ≥ 40% 다침, ≥ 15% 크게 다침, 그 밖 빈사. 경계를 소수 오차 없이 보려고 백분율로 곱해 비교한다 */
    public static Condition of(double current, int max) {
        double percent = current * 100;
        if (percent >= 75.0 * max) return HEALTHY;
        if (percent >= 40.0 * max) return HURT;
        if (percent >= 15.0 * max) return BADLY_HURT;
        return DYING;
    }

    /** {@code 멀쩡함}, {@code 다침}, {@code 크게 다침}, {@code 빈사} */
    public String koreanName() {
        return koreanName;
    }

    /** {@code 멀쩡하다.}, {@code 다쳤다.}, {@code 크게 다쳤다.}, {@code 빈사 상태다.} */
    public String sentence() {
        return sentence;
    }
}
