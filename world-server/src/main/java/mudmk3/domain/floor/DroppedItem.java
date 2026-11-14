package mudmk3.domain.floor;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.item.ItemType;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/** 플레이어가 버린 물건. 버린 시각부터 5분 동안 누구든 줍고, 그 뒤 사라진다. 같은 물건을 또 버리면 따로 5분을 잰다 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "itemType")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DroppedItem extends AbstractEntity {

    public static final Duration LIFETIME = Duration.ofMinutes(5);

    private Long roomId;

    @ManyToOne
    private ItemType itemType;

    private int count;

    private Instant expiresAt;

    public static DroppedItem drop(Long roomId, ItemType itemType, int count, Instant now) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
        DroppedItem dropped = new DroppedItem();
        dropped.roomId = requireNonNull(roomId);
        dropped.itemType = requireNonNull(itemType);
        dropped.count = count;
        dropped.expiresAt = now.plus(LIFETIME);
        return dropped;
    }

    /** 그 시각에 이미 사라졌는지(사라지는 시각 포함) */
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** {@code count} 개를 덜어 낸다. 0이 되면 서비스가 행을 지운다 */
    public void take(int count) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
        state(count <= this.count, () -> "바닥에 있는 것보다 많이 주울 수 없습니다: " + itemType.getName());
        this.count -= count;
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
