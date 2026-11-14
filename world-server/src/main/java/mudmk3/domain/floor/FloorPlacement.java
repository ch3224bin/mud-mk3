package mudmk3.domain.floor;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.item.ItemType;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * 관리자가 방 바닥에 둔 물건. 모두가 함께 쓰는 하나다. 줍는 만큼 남은 수가 줄고, 0이 된 시각부터 재등장 분이 지나면
 * 처음 수로 다시 놓인다(재등장이 없으면 한 번뿐). 서버 틱이 없으므로 읽는 시각으로 계산한다.
 * 방은 id로만 갖는다(방을 지우면 서비스가 함께 지우므로 FK를 두지 않는다)
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "itemType")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FloorPlacement extends AbstractEntity {

    public static final int COUNT_MAX = 9_999;
    public static final int RESPAWN_MINUTES_MAX = 1_440;

    private Long roomId;

    @ManyToOne
    private ItemType itemType;

    private int count;

    @Getter(AccessLevel.NONE)
    private @Nullable Integer respawnMinutes;

    /** 저장된 남은 수. 재등장할 때가 지났어도 {@link #refresh} 전에는 0일 수 있으므로 {@link #availableAt} 으로 읽는다 */
    @Getter(AccessLevel.NONE)
    private int remaining;

    @Getter(AccessLevel.NONE)
    private @Nullable Instant emptiedAt;

    public static FloorPlacement create(Long roomId, ItemType itemType, FloorPlacementSaveInfo info) {
        FloorPlacement placement = new FloorPlacement();
        placement.roomId = requireNonNull(roomId);
        placement.apply(itemType, info);
        return placement;
    }

    /** 물건·수·재등장을 바꾸고 처음 수로 다시 놓는다 */
    public void update(ItemType itemType, FloorPlacementSaveInfo info) {
        apply(itemType, info);
    }

    public Optional<Integer> findRespawnMinutes() {
        return Optional.ofNullable(respawnMinutes);
    }

    /** 그 시각에 주울 수 있는 수(저장하지 않는 계산) */
    public int availableAt(Instant now) {
        return isRespawnDueAt(now) ? count : remaining;
    }

    /** 재등장할 때가 지났으면 처음 수로 채운다 */
    public void refresh(Instant now) {
        if (isRespawnDueAt(now)) {
            this.remaining = count;
            this.emptiedAt = null;
        }
    }

    /** 그 시각에 {@code count} 개를 덜어 낸다. 0이 되면 비워진 시각을 적는다 */
    public void take(int count, Instant now) {
        if (count < 1) throw new IllegalArgumentException("수는 1 이상이어야 합니다: " + count);
        refresh(now);
        state(count <= remaining, () -> "바닥에 있는 것보다 많이 주울 수 없습니다: " + itemType.getName());
        this.remaining -= count;
        if (remaining == 0) {
            this.emptiedAt = requireNonNull(now);
        }
    }

    private boolean isRespawnDueAt(Instant now) {
        if (remaining > 0 || emptiedAt == null || respawnMinutes == null) return false;
        return !now.isBefore(emptiedAt.plus(Duration.ofMinutes(respawnMinutes)));
    }

    private void apply(ItemType itemType, FloorPlacementSaveInfo info) {
        requireNonNull(info);
        if (info.count() < 1 || info.count() > COUNT_MAX) {
            throw new IllegalArgumentException("수는 1~9,999로 정하십시오.");
        }
        Integer newRespawnMinutes = info.respawnMinutes();
        if (newRespawnMinutes != null && (newRespawnMinutes < 1 || newRespawnMinutes > RESPAWN_MINUTES_MAX)) {
            throw new IllegalArgumentException("재등장은 1~1,440분으로 정하십시오.");
        }
        this.itemType = requireNonNull(itemType);
        this.count = info.count();
        this.respawnMinutes = newRespawnMinutes;
        this.remaining = info.count();
        this.emptiedAt = null;
    }
}
