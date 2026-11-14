package mudmk3.domain.cultivation;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.technique.Technique;

import static java.util.Objects.requireNonNull;

/**
 * 캐릭터가 익힌 공법: (캐릭터, 공법)마다 하나, 지금 단계(4차에는 늘 1). 익힌 순서는 id 순이다.
 * 스탯 증가는 저장하지 않고 공법 정의에서 그때그때 더한다(관리 툴에서 고치면 바로 반영)
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"character", "technique"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharacterTechnique extends AbstractEntity {

    private PlayerCharacter character;

    private Technique technique;

    private int stage;

    /** 그 공법을 1성으로 익힌다 */
    public static CharacterTechnique learn(PlayerCharacter character, Technique technique) {
        CharacterTechnique learned = new CharacterTechnique();
        learned.character = requireNonNull(character);
        learned.technique = requireNonNull(technique);
        learned.stage = 1;
        return learned;
    }

    /**
     * 1성부터 지금 단계까지 스탯 증가의 합. 관리 툴이 단계 수를 줄이는 것은 익힌 단계까지만 되므로 늘 정의 안에 있지만,
     * 안전하게 정의에 있는 단계까지만 더한다
     */
    public StageBonusSum bonusSum() {
        int upTo = Math.min(stage, technique.stageCount());
        StageBonusSum sum = StageBonusSum.ZERO;
        for (int n = 1; n <= upTo; n++) {
            sum = sum.plus(StageBonusSum.of(technique.stage(n)));
        }
        return sum;
    }
}
