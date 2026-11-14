package mudmk3.application.technique.provided;

import mudmk3.domain.technique.TechniqueStage;
import org.jspecify.annotations.Nullable;

/** 공법 단계 한 줄 요청. 빠진 숫자는 0이다. 범위 검사는 도메인이 한다 */
public record StageRequest(
        @Nullable Integer insightRequired,
        @Nullable Integer aptitudeRequired,
        @Nullable Integer manaBonus,
        @Nullable Integer senseBonus,
        @Nullable Integer spiritBonus,
        @Nullable Integer essenceBonus,
        @Nullable Integer bodyBonus,
        @Nullable Integer strengthBonus,
        @Nullable Integer aptitudeGain
) {
    public StageRequest {
        insightRequired = orZero(insightRequired);
        aptitudeRequired = orZero(aptitudeRequired);
        manaBonus = orZero(manaBonus);
        senseBonus = orZero(senseBonus);
        spiritBonus = orZero(spiritBonus);
        essenceBonus = orZero(essenceBonus);
        bodyBonus = orZero(bodyBonus);
        strengthBonus = orZero(strengthBonus);
        aptitudeGain = orZero(aptitudeGain);
    }

    public TechniqueStage toStage() {
        return new TechniqueStage(orZero(insightRequired), orZero(aptitudeRequired), orZero(manaBonus),
                orZero(senseBonus), orZero(spiritBonus), orZero(essenceBonus), orZero(bodyBonus),
                orZero(strengthBonus), orZero(aptitudeGain));
    }

    private static int orZero(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
