package mudmk3.application.technique.provided;

import mudmk3.domain.technique.Technique;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * 옥간·서책에 담긴 공법을 볼 때 쓰는 값(익힌 수·담긴 물건은 세지 않는다).
 * {@code elementNames} 는 {@code 목}·{@code 화·토}(연체술이면 null), {@code lineName} 은 {@code 오행}·{@code 연체술}
 */
public record ContainedTechniqueView(
        String name,
        @Nullable String elementNames,
        String lineName,
        int stageCount,
        String startRealmName
) {
    public static ContainedTechniqueView of(Technique technique) {
        return new ContainedTechniqueView(requireNonNull(technique.getName()), technique.elementNames().orElse(null),
                technique.lineName(), technique.stageCount(), technique.getStartRealm().koreanName());
    }
}
