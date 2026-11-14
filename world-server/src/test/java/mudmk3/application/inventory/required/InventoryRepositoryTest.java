package mudmk3.application.inventory.required;

import jakarta.persistence.EntityManager;
import mudmk3.application.item.required.ItemTypeRepository;
import mudmk3.domain.inventory.Inventory;
import mudmk3.domain.inventory.InventoryEntry;
import mudmk3.domain.inventory.PurchaseResult;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.EnumMap;
import java.util.Map;

import static mudmk3.domain.item.SpiritStoneGrade.LOW;
import static mudmk3.domain.item.SpiritStoneGrade.MIDDLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
record InventoryRepositoryTest(InventoryRepository inventoryRepository, ItemTypeRepository itemTypeRepository,
                               EntityManager entityManager) {

    @Test
    void saveWithEntries() {
        Map<SpiritStoneGrade, ItemType> stones = saveStones();
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        ItemType sword = itemTypeRepository.save(ItemTypeFixture.createItemType("비검", ItemCategory.ARTIFACT, 50));
        ItemType bag = itemTypeRepository.save(ItemTypeFixture.createBag(BagGrade.LOW));
        Inventory inventory = Inventory.empty(7L);
        inventory.add(pill, 3);
        inventory.add(bag, 1);
        inventory.add(sword, 2);
        inventoryRepository.save(inventory);
        inventory.add(stones.get(LOW), 5);
        entityManager.flush();
        entityManager.clear();

        Inventory found = inventoryRepository.findByCharacterId(7L).orElseThrow();
        assertThat(found.getCharacterId()).isEqualTo(7L);
        assertThat(found.getEntries())
                .extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(
                        tuple("하품 영석", 5),
                        tuple("회춘단(연기 초)", 3),
                        tuple("하급 저물대", 1),
                        tuple("비검", 2));
        assertThat(inventoryRepository.findByCharacterId(8L)).isEmpty();
    }

    @Test
    void payAllLowStonesThenReceiveChange() {
        Map<SpiritStoneGrade, ItemType> stones = saveStones();
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        ItemType bag = itemTypeRepository.save(ItemTypeFixture.createBag(BagGrade.LOW));
        Inventory inventory = Inventory.empty(7L);
        inventory.add(bag, 1);
        inventory.add(stones.get(LOW), 50);
        inventory.add(stones.get(MIDDLE), 2);
        inventoryRepository.save(inventory);
        entityManager.flush();

        PurchaseResult.Bought bought = (PurchaseResult.Bought) inventory.checkPurchase(pill, 1, 120, stones.get(LOW));
        inventory.purchase(pill, 1, bought.payment(), stones);
        entityManager.flush();
        entityManager.clear();

        Inventory found = inventoryRepository.findByCharacterId(7L).orElseThrow();
        assertThat(found.spiritStones()).isEqualTo(Map.of(LOW, 30L, MIDDLE, 1L));
        assertThat(found.getEntries()).extracting(entry -> entry.getItemType().getName(), InventoryEntry::getCount)
                .containsExactly(tuple("하품 영석", 30), tuple("중품 영석", 1), tuple("하급 저물대", 1), tuple("회춘단(연기 초)", 1));
    }

    @Test
    void removeEmptiedEntry() {
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        Inventory inventory = inventoryRepository.save(Inventory.empty(7L));
        inventory.add(pill, 3);
        entityManager.flush();

        inventory.remove(pill, 3);
        entityManager.flush();
        entityManager.clear();

        assertThat(inventoryRepository.findByCharacterId(7L).orElseThrow().getEntries()).isEmpty();
        assertThat(itemTypeRepository.countInInventories(pill.getId())).isZero();
    }

    @Test
    void duplicateCharacterFail() {
        inventoryRepository.save(Inventory.empty(7L));

        assertThatThrownBy(() -> {
            inventoryRepository.save(Inventory.empty(7L));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countInInventories() {
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        ItemType hide = itemTypeRepository.save(ItemTypeFixture.createWolfHide());
        Inventory first = Inventory.empty(7L);
        first.add(pill, 3);
        Inventory second = Inventory.empty(8L);
        second.add(pill, 1);
        inventoryRepository.save(first);
        inventoryRepository.save(second);
        entityManager.flush();

        assertThat(itemTypeRepository.countInInventories(pill.getId())).isEqualTo(2);
        assertThat(itemTypeRepository.countInInventories(hide.getId())).isZero();
    }

    private Map<SpiritStoneGrade, ItemType> saveStones() {
        Map<SpiritStoneGrade, ItemType> stones = new EnumMap<>(SpiritStoneGrade.class);
        for (SpiritStoneGrade grade : SpiritStoneGrade.values()) {
            stones.put(grade, itemTypeRepository.save(ItemType.spiritStone(grade)));
        }
        return stones;
    }
}
