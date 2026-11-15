package mudmk3.domain.mob;

import static java.util.Objects.requireNonNull;

/**
 * 고정 재료 한 줄: 물건 종류 id와 수. 쓰러질 때마다 모두 나온다. 물건은 id로만 갖는다(FK 없음, 쓰이는 종류는 지우지 못하게
 * 아이템 서비스가 막는다). 수 범위는 물건 이름을 문구에 넣어야 하므로 {@link MobType} 이 검사한다
 */
public record MobFixedDrop(Long itemTypeId, int count) {

    public MobFixedDrop {
        requireNonNull(itemTypeId);
    }
}
