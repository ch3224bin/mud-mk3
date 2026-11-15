package mudmk3.application.cultivation.provided;

import mudmk3.domain.cultivation.InsightKind;

import java.util.Map;

/** 캐릭터의 깨달음을 쌓는다 */
public interface InsightEditor {

    /** 갈래마다 깨달음을 소수 그대로 더한다(행이 없으면 만든다). 0 이하인 갈래는 건너뛴다 */
    void add(Long characterId, Map<InsightKind, Double> gains);
}
