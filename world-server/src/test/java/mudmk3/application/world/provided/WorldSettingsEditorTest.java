package mudmk3.application.world.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.room.Room;
import mudmk3.domain.world.StartingItem;
import mudmk3.domain.zone.Zone;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record WorldSettingsEditorTest(
        WorldSettingsEditor worldSettingsEditor,
        WorldSettingsFinder worldSettingsFinder,
        ZoneEditor zoneEditor,
        RoomEditor roomEditor,
        ItemTypeEditor itemTypeEditor,
        ItemTypeFinder itemTypeFinder,
        EntityManager entityManager
) {

    @Test
    void noStartRoom() {
        assertThat(worldSettingsFinder.findStartRoom()).isEmpty();
        assertThat(worldSettingsFinder.isStartRoom(1L)).isFalse();
    }

    @Test
    void designateStartRoom() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));

        worldSettingsEditor.designateStartRoom(room.getId());
        flushAndClear();

        assertThat(worldSettingsFinder.findStartRoom()).hasValueSatisfying(start -> {
            assertThat(start.getName()).isEqualTo("청석숲 오솔길");
            assertThat(start.getZone().getName()).isEqualTo("청석숲");
        });
        assertThat(worldSettingsFinder.isStartRoom(room.getId())).isTrue();
    }

    @Test
    void designateAgainReplaces() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room first = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        Room second = roomEditor.create(createRoomSaveRequest(zone.getId(), "샘터"));
        worldSettingsEditor.designateStartRoom(first.getId());
        flushAndClear();

        worldSettingsEditor.designateStartRoom(second.getId());
        flushAndClear();

        assertThat(worldSettingsFinder.findStartRoom()).hasValueSatisfying(start ->
                assertThat(start.getName()).isEqualTo("샘터"));
        assertThat(worldSettingsFinder.isStartRoom(first.getId())).isFalse();
        assertThat(worldSettingsFinder.isStartRoom(second.getId())).isTrue();
        assertThat(entityManager.createQuery("select count(w) from WorldSettings w", Long.class).getSingleResult())
                .isEqualTo(1);
    }

    @Test
    void designateMissingRoomFail() {
        assertThatThrownBy(() -> worldSettingsEditor.designateStartRoom(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noStartingItems() {
        assertThat(worldSettingsFinder.findStartingItems()).isEmpty();
    }

    @Test
    void replaceStartingItems() {
        Long bag = itemType("하급 저물대", ItemCategory.BAG, BagGrade.LOW);
        Long lowStone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW).getId();
        Long pill = itemType("회춘단(연기 초)", ItemCategory.PILL, null);

        List<StartingItem> saved = worldSettingsEditor.replaceStartingItems(List.of(
                new StartingItemRequest(pill, 3), new StartingItemRequest(bag, 1), new StartingItemRequest(lowStone, 30)));
        flushAndClear();

        List<StartingItem> expected = List.of(new StartingItem(pill, 3), new StartingItem(bag, 1), new StartingItem(lowStone, 30));
        assertThat(saved).isEqualTo(expected);
        assertThat(worldSettingsFinder.findStartingItems()).isEqualTo(expected);
    }

    @Test
    void replaceStartingItemsAgainReplaces() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        worldSettingsEditor.designateStartRoom(room.getId());
        Long pill = itemType("회춘단(연기 초)", ItemCategory.PILL, null);
        Long lowStone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW).getId();
        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(pill, 3)));
        flushAndClear();

        worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(lowStone, 30), new StartingItemRequest(pill, 1)));
        flushAndClear();

        assertThat(worldSettingsFinder.findStartingItems())
                .containsExactly(new StartingItem(lowStone, 30), new StartingItem(pill, 1));
        assertThat(worldSettingsFinder.isStartRoom(room.getId())).isTrue();

        worldSettingsEditor.replaceStartingItems(List.of());
        flushAndClear();

        assertThat(worldSettingsFinder.findStartingItems()).isEmpty();
        assertThat(entityManager.createQuery("select count(w) from WorldSettings w", Long.class).getSingleResult())
                .isEqualTo(1);
    }

    @Test
    void duplicateItemFail() {
        Long pill = itemType("회춘단", ItemCategory.PILL, null);

        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(
                new StartingItemRequest(pill, 3), new StartingItemRequest(pill, 2))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 물건이 두 번 있습니다: 회춘단");
    }

    @Test
    void overCapacityFail() {
        Long lowStone = itemTypeFinder.spiritStone(SpiritStoneGrade.LOW).getId();
        Long pill = itemType("회춘단(연기 초)", ItemCategory.PILL, null);
        Long sword = itemType("비검", ItemCategory.ARTIFACT, null);

        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(
                new StartingItemRequest(lowStone, 30), new StartingItemRequest(pill, 3), new StartingItemRequest(sword, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 소지품이 칸을 넘습니다. (필요 3칸, 칸 2)");
        assertThat(worldSettingsFinder.findStartingItems()).isEmpty();
    }

    @Test
    void overCapacityWithBagFail() {
        Long bag = itemType("하급 저물대", ItemCategory.BAG, BagGrade.LOW);
        Long sword = itemType("비검", ItemCategory.ARTIFACT, null);

        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(
                new StartingItemRequest(bag, 1), new StartingItemRequest(sword, 2_000))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작 소지품이 칸을 넘습니다. (필요 2,000칸, 칸 22)");
    }

    @Test
    void fourBagsFail() {
        Long low = itemType("하급 저물대", ItemCategory.BAG, BagGrade.LOW);
        Long middle = itemType("중급 저물대", ItemCategory.BAG, BagGrade.MIDDLE);

        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(
                new StartingItemRequest(low, 3), new StartingItemRequest(middle, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("저물대는 세 개까지만 찰 수 있습니다.");
    }

    @Test
    void threeBags() {
        Long low = itemType("하급 저물대", ItemCategory.BAG, BagGrade.LOW);
        Long high = itemType("상급 저물대", ItemCategory.BAG, BagGrade.HIGH);
        Long sword = itemType("비검", ItemCategory.ARTIFACT, null);

        worldSettingsEditor.replaceStartingItems(List.of(
                new StartingItemRequest(low, 2), new StartingItemRequest(high, 1), new StartingItemRequest(sword, 122)));

        assertThat(worldSettingsFinder.findStartingItems()).hasSize(3);
    }

    @Test
    void unknownItemFail() {
        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(999_999L, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 999999");
    }

    @Test
    void invalidStartingItemsRequestFail() {
        Long pill = itemType("회춘단", ItemCategory.PILL, null);
        List<StartingItemRequest> tooMany = IntStream.rangeClosed(1, 31).mapToObj(i -> new StartingItemRequest(pill, 1)).toList();

        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(pill, 0))))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(pill, 1_000_000))))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(null, 1))))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> worldSettingsEditor.replaceStartingItems(tooMany))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private Long itemType(String name, ItemCategory category, @Nullable BagGrade bagGrade) {
        return itemTypeEditor.create(createItemTypeSaveRequest(name, category, 3, bagGrade)).getId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
