package mudmk3.application.cultivation.provided;

import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.Insight;
import mudmk3.domain.cultivation.StatSheet;

import java.util.List;

/** 캐릭터의 수련 상태(경지·영근·익힌 공법·스탯)를 본다 */
public interface CultivationFinder {

    /** 지금 경지·영근·익힌 공법으로 계산한 스탯. 없는 캐릭터면 {@code IllegalArgumentException} */
    StatSheet stats(Long characterId);

    /** 익힌 공법(익힌 순서). 공법 정의의 단계·신통은 지연 로딩이라 부르는 쪽 트랜잭션 안에서 쓴다 */
    List<CharacterTechnique> findLearned(Long characterId);

    /** 쌓인 깨달음(여섯 갈래, 없으면 0) */
    Insight insight(Long characterId);

    /** 트랜잭션 안에서 만든 캐릭터 수련 보기 값. 없는 캐릭터면 {@code IllegalArgumentException} */
    CultivationProfile profile(Long characterId);

    /** 모든 캐릭터의 요약(이름순) */
    List<CharacterSummary> summaries();
}
