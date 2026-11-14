package mudmk3.application.cultivation.provided;

import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.TechniqueStage;

import java.util.List;

/** 읽기 결과. 거절이면 물건은 그대로 남는다 */
public sealed interface ReadResult {

    /** 종류가 옥간·서책이 아님 */
    record NotScroll() implements ReadResult {
    }

    /** 담긴 공법이 없음 */
    record NoTechnique() implements ReadResult {
    }

    /** 이미 익힌 공법 */
    record AlreadyLearned(String techniqueName, int stage, int stageCount) implements ReadResult {
    }

    /** 지금 경지가 공법 시작 경지보다 낮음 */
    record RealmTooLow(Realm required) implements ReadResult {
    }

    /** 공법 속성의 영근이 없음(오행 순으로 첫째) */
    record MissingRoot(Element element) implements ReadResult {
    }

    /** 1성 영근 자질 조건 미달(오행 순으로 첫째). {@code current} 는 소수점을 버린 값 */
    record LowAptitude(Element element, int required, int current) implements ReadResult {
    }

    /**
     * 익힘. {@code elements} 는 공법 속성(오행 순, 연체술이면 비어 있음 — 1성 영근 자질 증가를 속성마다 적는 데 쓴다),
     * {@code newAbilityNames} 는 1성 신통(정의 순서), {@code initiated} 는 범인이 연기 1층이 되었는가
     */
    record Learned(String techniqueName, List<Element> elements, TechniqueStage firstStage, List<String> newAbilityNames,
                   boolean initiated) implements ReadResult {
        public Learned {
            elements = List.copyOf(elements);
            newAbilityNames = List.copyOf(newAbilityNames);
        }
    }
}
