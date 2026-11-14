package mudmk3.application.technique.provided;

import mudmk3.domain.technique.Technique;

import java.util.List;

/** 공법·신통 정의를 찾는다 */
public interface TechniqueFinder {

    /** 속성·단계·신통까지 읽힌 공법. 없으면 {@code IllegalArgumentException} */
    Technique find(Long techniqueId);

    /** 트랜잭션 안에서 만든 공법 보기 값 */
    TechniqueView view(Long techniqueId);

    /** 이름순 */
    List<TechniqueView> viewAll();
}
