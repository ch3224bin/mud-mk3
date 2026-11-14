package mudmk3.application.cultivation.provided;

import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 캐릭터 수련 보기 값: 서비스가 트랜잭션 안에서 만들어 트랜잭션 밖(REST 응답)에서도 읽을 수 있다.
 * 영근을 아직 정하지 않았으면 {@code spiritRoot} 는 null, 익힌 공법은 익힌 순서
 */
public record CultivationProfile(Long id, String name, Realm realm, @Nullable SpiritRoot spiritRoot, StatSheet stats,
                                 List<LearnedTechniqueView> techniques) {
    public CultivationProfile {
        techniques = List.copyOf(techniques);
    }
}
