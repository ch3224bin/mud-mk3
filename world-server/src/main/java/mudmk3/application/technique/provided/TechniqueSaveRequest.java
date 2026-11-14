package mudmk3.application.technique.provided;

import jakarta.validation.constraints.NotNull;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueSaveInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 공법을 만들거나 고치는 요청. {@code stages[0]} 이 1성이고 길이가 단계 수다. 빠진 속성은 빈 목록, 빠진 묘사는 빈 글,
 * 빠진 이름은 빈 글(도메인이 이름 규칙으로 막는다)이다. 규칙 검사와 문구는 도메인이 맡는다.
 * 시작 경지가 없으면 만들 때 {@code IllegalArgumentException} 을 던져, JSON 을 읽는 중이면
 * 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 되게 한다(Bean Validation 문구와 다르므로 {@code @NotNull} 을 쓰지 않는다)
 */
public record TechniqueSaveRequest(
        String name,
        @Nullable String description,
        @Nullable List<Element> elements,
        Realm startRealm,
        @NotNull List<@NotNull StageRequest> stages,
        @NotNull List<@NotNull AbilityRequest> abilities
) {
    public TechniqueSaveRequest {
        name = name == null ? "" : name;
        elements = RequestValues.elements(elements);
        RequestValues.required(startRealm, "startRealm");
    }

    /** 컴팩트 생성자가 이미 채웠지만 컴포넌트가 @Nullable 타입이라 다시 채운다 */
    public TechniqueSaveInfo toInfo() {
        return new TechniqueSaveInfo(name, description == null ? "" : description, RequestValues.elements(elements),
                startRealm,
                stages.stream().map(StageRequest::toStage).toList(),
                abilities.stream().map(AbilityRequest::toInfo).toList());
    }
}
