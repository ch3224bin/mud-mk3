package mudmk3.application.floor.required;

import jakarta.persistence.EntityManager;
import mudmk3.application.item.required.ItemTypeRepository;
import mudmk3.domain.floor.DroppedItem;
import mudmk3.domain.floor.FloorPlacement;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Duration;
import java.time.Instant;

import static mudmk3.domain.floor.FloorFixture.createFloorPlacementSaveInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
record FloorPlacementRepositoryTest(FloorPlacementRepository floorPlacementRepository,
                                    DroppedItemRepository droppedItemRepository,
                                    ItemTypeRepository itemTypeRepository, EntityManager entityManager) {

    static final Instant T0 = Instant.parse("2026-11-14T00:00:00Z");

    @Test
    void saveAndFindInRoom() {
        ItemType hide = itemTypeRepository.save(ItemTypeFixture.createWolfHide());
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        FloorPlacement first = floorPlacementRepository.save(FloorPlacement.create(3L, pill, createFloorPlacementSaveInfo(2, null)));
        floorPlacementRepository.save(FloorPlacement.create(4L, hide, createFloorPlacementSaveInfo(1, 10)));
        FloorPlacement second = floorPlacementRepository.save(FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(3, 10)));
        second.take(3, T0);
        entityManager.flush();
        entityManager.clear();

        var found = floorPlacementRepository.findAllByRoomIdOrderByIdAsc(3L);
        entityManager.clear();

        assertThat(found).extracting(FloorPlacement::getId).containsExactly(first.getId(), second.getId());
        assertThat(found).allMatch(placement -> Hibernate.isInitialized(placement.getItemType()));
        assertThat(found).extracting(placement -> placement.getItemType().getName(), FloorPlacement::getCount,
                        placement -> placement.findRespawnMinutes().orElse(null))
                .containsExactly(tuple("회춘단(연기 초)", 2, null), tuple("늑대 가죽", 3, 10));
        // 남은 수와 비워진 시각이 저장된다
        FloorPlacement emptied = found.get(1);
        assertThat(emptied.availableAt(T0.plus(Duration.ofMinutes(9)))).isZero();
        assertThat(emptied.availableAt(T0.plus(Duration.ofMinutes(10)))).isEqualTo(3);
        assertThat(Hibernate.isInitialized(floorPlacementRepository.findById(first.getId()).orElseThrow().getItemType())).isTrue();
    }

    @Test
    void droppedItems() {
        ItemType hide = itemTypeRepository.save(ItemTypeFixture.createWolfHide());
        droppedItemRepository.save(DroppedItem.drop(3L, hide, 1, T0));
        DroppedItem later = droppedItemRepository.save(DroppedItem.drop(3L, hide, 2, T0.plus(Duration.ofMinutes(3))));
        droppedItemRepository.save(DroppedItem.drop(4L, hide, 5, T0));
        entityManager.flush();
        entityManager.clear();

        droppedItemRepository.deleteByRoomIdAndExpiresAtLessThanEqual(3L, T0.plus(Duration.ofMinutes(5)));
        entityManager.flush();
        entityManager.clear();

        var found = droppedItemRepository.findAllByRoomIdOrderByIdAsc(3L);
        entityManager.clear();
        assertThat(found).extracting(DroppedItem::getId).containsExactly(later.getId());
        assertThat(found.getFirst().getItemType().getName()).isEqualTo("늑대 가죽");
        assertThat(found.getFirst().getExpiresAt()).isEqualTo(T0.plus(Duration.ofMinutes(8)));
        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(4L)).hasSize(1);
    }

    @Test
    void deleteByRoomId() {
        ItemType hide = itemTypeRepository.save(ItemTypeFixture.createWolfHide());
        floorPlacementRepository.save(FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(2, null)));
        floorPlacementRepository.save(FloorPlacement.create(4L, hide, createFloorPlacementSaveInfo(2, null)));
        droppedItemRepository.save(DroppedItem.drop(3L, hide, 1, T0));
        entityManager.flush();
        entityManager.clear();

        floorPlacementRepository.deleteByRoomId(3L);
        droppedItemRepository.deleteByRoomId(3L);
        entityManager.flush();
        entityManager.clear();

        assertThat(floorPlacementRepository.findAllByRoomIdOrderByIdAsc(3L)).isEmpty();
        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(3L)).isEmpty();
        assertThat(floorPlacementRepository.findAllByRoomIdOrderByIdAsc(4L)).hasSize(1);
    }

    @Test
    void countAndDeleteExpiredForItemType() {
        ItemType hide = itemTypeRepository.save(ItemTypeFixture.createWolfHide());
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        FloorPlacement placement = floorPlacementRepository.save(FloorPlacement.create(3L, hide, createFloorPlacementSaveInfo(1, null)));
        placement.take(1, T0);
        droppedItemRepository.save(DroppedItem.drop(3L, pill, 1, T0));
        droppedItemRepository.save(DroppedItem.drop(4L, pill, 1, T0.plus(Duration.ofMinutes(1))));
        entityManager.flush();
        Instant now = T0.plus(Duration.ofMinutes(5));

        assertThat(itemTypeRepository.countInFloorPlacements(hide.getId())).isEqualTo(1);
        assertThat(itemTypeRepository.countInFloorPlacements(pill.getId())).isZero();
        assertThat(itemTypeRepository.countInDroppedItems(pill.getId(), now)).isEqualTo(1);
        assertThat(itemTypeRepository.countInDroppedItems(pill.getId(), now.plus(Duration.ofMinutes(1)))).isZero();

        itemTypeRepository.deleteExpiredDroppedItems(pill.getId(), now);
        entityManager.clear();

        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(3L)).isEmpty();
        assertThat(droppedItemRepository.findAllByRoomIdOrderByIdAsc(4L)).hasSize(1);
    }
}
