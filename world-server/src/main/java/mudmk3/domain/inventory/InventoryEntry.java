package mudmk3.domain.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.item.ItemType;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/** 소지품 묶음: 한 소지품 안에서 종류마다 하나뿐인 물건 묶음과 그 수. Inventory 애그리거트 안의 엔티티다 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"inventory", "itemType"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryEntry extends AbstractEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne
    private Inventory inventory;

    @ManyToOne
    private ItemType itemType;

    private int count;

    static InventoryEntry create(Inventory inventory, ItemType itemType) {
        InventoryEntry entry = new InventoryEntry();
        entry.inventory = requireNonNull(inventory);
        entry.itemType = requireNonNull(itemType);
        return entry;
    }

    boolean isFor(ItemType itemType) {
        return this.itemType.equals(itemType);
    }

    /** 수를 더하거나 뺀다. 0 아래로 내려갈 수 없다 */
    void changeCount(long delta) {
        long newCount = count + delta;
        state(newCount >= 0, () -> "묶음의 수는 0 아래로 내려갈 수 없습니다: " + itemType.getName());
        this.count = Math.toIntExact(newCount);
    }
}
