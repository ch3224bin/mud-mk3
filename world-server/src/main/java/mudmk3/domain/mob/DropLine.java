package mudmk3.domain.mob;

import static java.util.Objects.requireNonNull;

/** 굴려서 정한 드롭 한 줄: 물건 종류 id, 이름(문구용), 수 */
public record DropLine(Long itemTypeId, String name, int count) {

    public DropLine {
        requireNonNull(itemTypeId);
        requireNonNull(name);
        if (count < 1) {
            throw new IllegalArgumentException("드롭 수는 1 이상이어야 합니다: " + count);
        }
    }
}
