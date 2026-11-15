package mudmk3.domain.combat;

/** 법력 발휘: 남은 법력이 적으면 영력이 덜 실린다(룰북 공격력·법력 발휘) */
public final class SpiritOutput {

    private static final double FULL_OUTPUT_RATIO = 0.2;
    private static final double MIN_OUTPUT = 0.5;

    private SpiritOutput() {
    }

    /**
     * 법력 발휘 배율: 남은 법력 비율이 20% 이상이면 1.0, 아니면 0.5 + 0.5 × (비율 ÷ 0.2). 법력 최대치가 0 이면 1.0
     */
    public static double ratio(double manaCurrent, int manaMax) {
        if (manaMax <= 0) return 1.0;
        double ratio = Math.max(0, manaCurrent) / manaMax;
        if (ratio >= FULL_OUTPUT_RATIO) return 1.0;
        return MIN_OUTPUT + (1 - MIN_OUTPUT) * (ratio / FULL_OUTPUT_RATIO);
    }
}
