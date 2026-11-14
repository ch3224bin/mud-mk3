package mudmk3.adapter.webapi.dto;

import mudmk3.application.cultivation.provided.CultivationProfile;
import mudmk3.application.cultivation.provided.LearnedTechniqueView;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.character.SpiritRootEntry;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 캐릭터 상세. 영근을 정하지 않았으면 {@code spiritRoot} 는 null. 영근 자질은 처음 자질 + 공법 증가(소수),
 * 스탯은 최대치, 공법은 익힌 순서
 */
public record CharacterDetailResponse(
        Long id,
        String name,
        Realm realm,
        String realmName,
        @Nullable SpiritRootResponse spiritRoot,
        StatsResponse stats,
        List<TechniqueResponse> techniques
) {

    public static CharacterDetailResponse of(CultivationProfile profile) {
        SpiritRoot root = profile.spiritRoot();
        return new CharacterDetailResponse(profile.id(), profile.name(), profile.realm(),
                profile.realm().koreanName(),
                root == null ? null : SpiritRootResponse.of(root, profile.stats()),
                StatsResponse.of(profile.stats()),
                profile.techniques().stream().map(TechniqueResponse::of).toList());
    }

    public record SpiritRootResponse(String name, List<RootResponse> roots) {

        static SpiritRootResponse of(SpiritRoot root, StatSheet stats) {
            return new SpiritRootResponse(root.name(), root.entries().stream()
                    .map(SpiritRootEntry::element)
                    .map(element -> new RootResponse(element, element.koreanName(), stats.aptitude(element)))
                    .toList());
        }
    }

    public record RootResponse(Element element, String elementName, double aptitude) {
    }

    public record StatsResponse(int qiBlood, int mana, int sense, int essence, int spirit, int body, int strength) {

        static StatsResponse of(StatSheet stats) {
            return new StatsResponse(stats.qiBlood(), stats.mana(), stats.sense(), stats.essence(), stats.spirit(),
                    stats.body(), stats.strength());
        }
    }

    public record TechniqueResponse(Long id, String name, int stage, int stageCount) {

        static TechniqueResponse of(LearnedTechniqueView view) {
            return new TechniqueResponse(view.techniqueId(), view.name(), view.stage(), view.stageCount());
        }
    }
}
