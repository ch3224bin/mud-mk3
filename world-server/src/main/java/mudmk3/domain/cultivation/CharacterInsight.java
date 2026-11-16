package mudmk3.domain.cultivation;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;

import static java.util.Objects.requireNonNull;

/**
 * 캐릭터의 한 갈래 깨달음: (캐릭터, 갈래)마다 하나, 행이 없으면 0. 소수로 쌓는다.
 * 캐릭터 행과 다른 표라 관리 경지 지정·이동의 저장이 되돌리지 않는다
 */
@Entity
@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharacterInsight extends AbstractEntity {

    private Long characterId;

    private InsightKind kind;

    private double amount;

    public static CharacterInsight create(Long characterId, InsightKind kind, double amount) {
        CharacterInsight insight = new CharacterInsight();
        insight.characterId = requireNonNull(characterId);
        insight.kind = requireNonNull(kind);
        insight.amount = amount;
        return insight;
    }

    /** 깨달음을 더한다. 0 이상 유한한 값만 받는다(깨달음은 줄지 않음) */
    public void add(double gain) {
        if (!Double.isFinite(gain) || gain < 0) {
            throw new IllegalArgumentException("깨달음은 0 이상 유한한 값만 더합니다: " + gain);
        }
        this.amount += gain;
    }
}
