package mudmk3.domain.cultivation;

import mudmk3.domain.technique.TechniqueStage;

/** 공법 단계 스탯 증가의 합(익힌 공법 하나의 1성~지금 단계, 또는 여러 공법의 합) */
public record StageBonusSum(int mana, int sense, int spirit, int essence, int body, int strength, int aptitudeGain) {

    public static final StageBonusSum ZERO = new StageBonusSum(0, 0, 0, 0, 0, 0, 0);

    public static StageBonusSum of(TechniqueStage stage) {
        return new StageBonusSum(stage.manaBonus(), stage.senseBonus(), stage.spiritBonus(), stage.essenceBonus(),
                stage.bodyBonus(), stage.strengthBonus(), stage.aptitudeGain());
    }

    public StageBonusSum plus(StageBonusSum other) {
        return new StageBonusSum(mana + other.mana, sense + other.sense, spirit + other.spirit,
                essence + other.essence, body + other.body, strength + other.strength,
                aptitudeGain + other.aptitudeGain);
    }
}
