package mudmk3.adapter.webapi.dto;

import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** 공법 목록 한 줄. {@code elementNames} 는 {@code 목}·{@code 화·토}(연체술이면 null) */
public record TechniqueSummaryResponse(
        Long id,
        String name,
        List<Element> elements,
        @Nullable String elementNames,
        String lineName,
        Realm startRealm,
        String startRealmName,
        int stageCount,
        int abilityCount,
        long learnerCount
) {

    public static TechniqueSummaryResponse of(TechniqueView view) {
        return new TechniqueSummaryResponse(view.id(), view.name(), view.elements(), view.elementNames(),
                view.lineName(), view.startRealm(), view.startRealmName(), view.stageCount(), view.abilityCount(),
                view.learnerCount());
    }
}
