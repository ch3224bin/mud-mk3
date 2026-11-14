package mudmk3.adapter.webapi.dto;

import mudmk3.application.cultivation.provided.CharacterSummary;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

/**
 * 캐릭터 목록 한 줄. {@code spiritRootName} 은 {@code 목·화 진영근}(정하지 않았으면 null),
 * {@code online} 은 지금 접속해 세계에 있는지
 */
public record CharacterSummaryResponse(
        Long id,
        String name,
        Realm realm,
        String realmName,
        @Nullable String spiritRootName,
        int techniqueCount,
        boolean online
) {

    public static CharacterSummaryResponse of(CharacterSummary summary, boolean online) {
        return new CharacterSummaryResponse(summary.id(), summary.name(), summary.realm(),
                summary.realm().koreanName(), summary.spiritRootName(), summary.techniqueCount(), online);
    }
}
