package mudmk3.application.cultivation.provided;

import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

/** 캐릭터 목록 한 줄: 영근을 아직 정하지 않았으면 {@code spiritRootName} 은 null */
public record CharacterSummary(Long id, String name, Realm realm, @Nullable String spiritRootName,
                               int techniqueCount) {
}
