package mudmk3.domain.combat;

import static java.util.Objects.requireNonNull;

/** 신통 하나를 가리키는 키: 공법 id와 신통 이름(재사용 대기시간을 신통마다 잰다) */
public record AbilityKey(Long techniqueId, String abilityName) {

    public AbilityKey {
        requireNonNull(techniqueId);
        requireNonNull(abilityName);
    }
}
