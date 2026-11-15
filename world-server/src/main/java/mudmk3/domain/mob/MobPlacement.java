package mudmk3.domain.mob;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;

import static java.util.Objects.requireNonNull;

/**
 * 관리자가 방에 둔 몹: 몹 유형과 수(1~10). 한 방에 배치 여럿을 둘 수 있고, 배치 하나의 몹은 수만큼 따로 싸우고 따로
 * 재등장한다(메모리의 몹은 서버 틱이 이 배치에 맞춘다). 방은 id로만 갖는다(방을 지우면 서비스가 함께 지우므로 FK를 두지 않는다)
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "mobType")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobPlacement extends AbstractEntity {

    public static final int COUNT_MIN = 1;
    public static final int COUNT_MAX = 10;

    private Long roomId;

    private MobType mobType;

    private int count;

    public static MobPlacement create(Long roomId, MobType mobType, int count) {
        MobPlacement placement = new MobPlacement();
        placement.roomId = requireNonNull(roomId);
        placement.apply(mobType, count);
        return placement;
    }

    /** 몹 유형과 수를 바꾼다(방은 그대로) */
    public void update(MobType mobType, int count) {
        apply(mobType, count);
    }

    private void apply(MobType mobType, int count) {
        requireNonNull(mobType);
        if (count < COUNT_MIN || count > COUNT_MAX) {
            throw new IllegalArgumentException("몹 수는 1~10으로 정하십시오.");
        }
        this.mobType = mobType;
        this.count = count;
    }
}
