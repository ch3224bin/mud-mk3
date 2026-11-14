package mudmk3.domain.world;

import static java.util.Objects.requireNonNull;

/** 새 캐릭터가 받는 시작 소지품 한 줄: 물건 종류 id와 수(1~999,999) */
public record StartingItem(Long itemTypeId, int count) {

    public static final int COUNT_MAX = 999_999;

    public StartingItem {
        requireNonNull(itemTypeId);
        if (count < 1 || count > COUNT_MAX) {
            throw new IllegalArgumentException("시작 소지품의 수는 1 이상 999,999 이하로 정하십시오: " + count);
        }
    }
}
