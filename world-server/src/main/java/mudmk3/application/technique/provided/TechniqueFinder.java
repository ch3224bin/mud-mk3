package mudmk3.application.technique.provided;

import mudmk3.domain.technique.Technique;

import java.util.List;
import java.util.Optional;

/** 공법·신통 정의를 찾는다 */
public interface TechniqueFinder {

    /** 속성·단계·신통까지 읽힌 공법. 없으면 {@code IllegalArgumentException} */
    Technique find(Long techniqueId);

    /** 트랜잭션 안에서 만든 공법 보기 값 */
    TechniqueView view(Long techniqueId);

    /** 옥간·서책에 담긴 공법 보기 값(익힌 수·담긴 물건은 세지 않는다) */
    ContainedTechniqueView viewContained(Long techniqueId);

    /** 이름순 */
    List<TechniqueView> viewAll();

    /**
     * 어느 공법에든 그 이름(영문 대소문자 무시)의 신통이 정의되어 있으면 처음 정의된 것의 저장된 이름. 없으면 빈 값
     */
    Optional<String> findAbilityName(String name);
}
