package mudmk3.domain.technique;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Names;
import mudmk3.domain.shared.Realm;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * 공법을 만들거나 고칠 때 받는 값. 이름·묘사는 앞뒤 공백을 자른다.
 * {@code stages[0]} 이 1성이다. 규칙 검사는 {@link Technique} 가 한다
 */
public record TechniqueSaveInfo(
        String name,
        String description,
        List<Element> elements,
        Realm startRealm,
        List<TechniqueStage> stages,
        List<AbilitySaveInfo> abilities
) {
    public TechniqueSaveInfo {
        name = requireNonNull(Names.strip(name));
        description = description.strip();
        elements = List.copyOf(elements);
        requireNonNull(startRealm);
        stages = List.copyOf(stages);
        abilities = List.copyOf(abilities);
    }
}
