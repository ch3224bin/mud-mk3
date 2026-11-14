package mudmk3.application.npc.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.StartRoomDeletionException;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.npc.DuplicateNpcNameException;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcTopic;
import mudmk3.domain.npc.ShopItem;
import mudmk3.domain.npc.ShopKind;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record NpcEditorTest(NpcEditor npcEditor, NpcFinder npcFinder, ZoneEditor zoneEditor, RoomEditor roomEditor,
                     WorldSettingsEditor worldSettingsEditor, ItemTypeEditor itemTypeEditor, EntityManager entityManager) {

    @Test
    void createPlaced() {
        Room room = prepareRoom("마을 어귀");

        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.getName()).isEqualTo("왕씨");
        assertThat(found.getGreeting()).isEqualTo("어서 오시오.");
        assertThat(found.getDefaultAnswer()).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(found.getTopics()).extracting(NpcTopic::word).containsExactly("단약", "일거리");
        assertThat(found.findRoomId()).contains(room.getId());
    }

    @Test
    void createUnplaced() {
        Npc npc = npcEditor.create(new NpcSaveRequest(" 왕씨 ", "어서 오시오.", "글쎄.", null, false, null));
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.getName()).isEqualTo("왕씨");
        assertThat(found.getTopics()).isEmpty();
        assertThat(found.findRoomId()).isEmpty();
    }

    @Test
    void duplicateNameFail() {
        npcEditor.create(createNpcSaveRequest("Wang", null));

        assertThatThrownBy(() -> npcEditor.create(createNpcSaveRequest("wang", null)))
                .isInstanceOf(DuplicateNpcNameException.class)
                .hasMessage("이미 있는 NPC 이름입니다: wang");
    }

    @Test
    void updateToOtherNpcNameFail() {
        npcEditor.create(createNpcSaveRequest("왕씨", null));
        Npc other = npcEditor.create(createNpcSaveRequest("이씨", null));

        assertThatThrownBy(() -> npcEditor.update(other.getId(), createNpcSaveRequest("왕씨", null)))
                .isInstanceOf(DuplicateNpcNameException.class)
                .hasMessage("이미 있는 NPC 이름입니다: 왕씨");
    }

    @Test
    void updateKeepsOwnNameInOtherCase() {
        Npc npc = npcEditor.create(createNpcSaveRequest("wang", null));

        Npc updated = npcEditor.update(npc.getId(), createNpcSaveRequest("Wang", null));

        assertThat(updated.getName()).isEqualTo("Wang");
    }

    @Test
    void updateReplacesTopicsAndPlacement() {
        Room first = prepareRoom("마을 어귀");
        Room second = roomEditor.create(createRoomSaveRequest(first.getZone().getId(), "우물가"));
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", first.getId()));
        flushAndClear();

        npcEditor.update(npc.getId(), new NpcSaveRequest("왕씨", "또 왔구려.", "모르오.",
                List.of(new NpcTopicRequest("[소문]", "청석성에 도적이 들었다 하오.")), false, second.getId()));
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.getGreeting()).isEqualTo("또 왔구려.");
        assertThat(found.getDefaultAnswer()).isEqualTo("모르오.");
        assertThat(found.getTopics()).containsExactly(new NpcTopic("소문", "청석성에 도적이 들었다 하오."));
        assertThat(found.findRoomId()).contains(second.getId());

        npcEditor.update(npc.getId(), createNpcSaveRequest("왕씨", null));
        flushAndClear();

        assertThat(npcFinder.find(npc.getId()).findRoomId()).isEmpty();
    }

    @Test
    void unknownRoomFail() {
        assertThatThrownBy(() -> npcEditor.create(createNpcSaveRequest("왕씨", 999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방을 찾을 수 없습니다. id: 999");
    }

    @Test
    void unknownNpcFail() {
        assertThatThrownBy(() -> npcFinder.find(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("NPC를 찾을 수 없습니다. id: 999");
    }

    @Test
    void duplicateTopicFail() {
        assertThatThrownBy(() -> npcEditor.create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄.",
                List.of(new NpcTopicRequest("단약", "가"), new NpcTopicRequest("[단약]", "나")), false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("같은 낱말이 두 번 있습니다: 단약");
    }

    @Test
    void delete() {
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));
        flushAndClear();

        npcEditor.delete(npc.getId());
        flushAndClear();

        assertThat(npcFinder.findAll()).isEmpty();
    }

    @Test
    void deleteRoomUnplacesNpcs() {
        Room room = prepareRoom("마을 어귀");
        Npc wang = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        Npc lee = npcEditor.create(createNpcSaveRequest("이씨", room.getId()));
        flushAndClear();

        roomEditor.delete(room.getId());
        flushAndClear();

        assertThat(npcFinder.find(wang.getId()).findRoomId()).isEmpty();
        assertThat(npcFinder.find(lee.getId()).findRoomId()).isEmpty();
        assertThat(npcFinder.findAllInRoom(room.getId())).isEmpty();
    }

    @Test
    void deleteStartRoomKeepsNpcPlacement() {
        Room room = prepareRoom("마을 어귀");
        worldSettingsEditor.designateStartRoom(room.getId());
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", room.getId()));
        flushAndClear();

        assertThatThrownBy(() -> roomEditor.delete(room.getId()))
                .isInstanceOf(StartRoomDeletionException.class);
        flushAndClear();

        assertThat(npcFinder.find(npc.getId()).findRoomId()).contains(room.getId());
    }

    @Test
    void llmFlagStored() {
        Npc npc = npcEditor.create(new NpcSaveRequest("왕씨", "어서 오시오.", "글쎄.", List.of(), true, null));
        flushAndClear();

        assertThat(npcFinder.find(npc.getId()).isLlmEnabled()).isTrue();
    }

    @Test
    void openShopAndReplace() {
        ItemType pill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        ItemType other = itemTypeEditor.create(createItemTypeSaveRequest("청심단", ItemCategory.PILL, 5));
        ItemType hide = itemTypeEditor.create(createItemTypeSaveRequest("늑대 가죽", ItemCategory.MATERIAL, 2));
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));

        Npc opened = npcEditor.openShop(npc.getId(), new ShopSaveRequest(ShopKind.PILL_SHOP, List.of(
                new ShopItemRequest(other.getId(), 12), new ShopItemRequest(pill.getId(), null))));
        assertThat(opened.shopName()).isEqualTo("왕씨 단약방");
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.findShopKind()).contains(ShopKind.PILL_SHOP);
        assertThat(found.getShopItems()).containsExactly(new ShopItem(other.getId(), 12), new ShopItem(pill.getId(), null));

        npcEditor.openShop(npc.getId(), new ShopSaveRequest(ShopKind.MATERIAL_SHOP, List.of(new ShopItemRequest(hide.getId(), 5))));
        flushAndClear();

        Npc replaced = npcFinder.find(npc.getId());
        assertThat(replaced.shopName()).isEqualTo("왕씨 재료상");
        assertThat(replaced.getShopItems()).containsExactly(new ShopItem(hide.getId(), 5));
        // NPC를 고쳐도 상점은 그대로다
        npcEditor.update(npc.getId(), createNpcSaveRequest("왕노인", null));
        flushAndClear();
        assertThat(npcFinder.find(npc.getId()).shopName()).isEqualTo("왕노인 재료상");
    }

    @Test
    void closeShop() {
        ItemType pill = itemTypeEditor.create(createItemTypeSaveRequest("회춘단", ItemCategory.PILL, 3));
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));
        npcEditor.openShop(npc.getId(), new ShopSaveRequest(ShopKind.PILL_SHOP, List.of(new ShopItemRequest(pill.getId(), null))));
        flushAndClear();

        npcEditor.closeShop(npc.getId());
        flushAndClear();

        Npc found = npcFinder.find(npc.getId());
        assertThat(found.hasShop()).isFalse();
        assertThat(found.getShopItems()).isEmpty();
        // 상점이 없어도 된다
        npcEditor.closeShop(npc.getId());
        // 상점을 없애면 그 물건 종류를 지울 수 있다
        itemTypeEditor.delete(pill.getId());
    }

    @Test
    void unknownItemFail() {
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));

        assertThatThrownBy(() -> npcEditor.openShop(npc.getId(),
                new ShopSaveRequest(ShopKind.PILL_SHOP, List.of(new ShopItemRequest(999L, null)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> npcEditor.openShop(999L, new ShopSaveRequest(ShopKind.PILL_SHOP, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("NPC를 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> npcEditor.closeShop(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("NPC를 찾을 수 없습니다. id: 999");
    }

    @Test
    void openShopRuleFail() {
        ItemType sword = itemTypeEditor.create(createItemTypeSaveRequest("비검", ItemCategory.ARTIFACT, 50));
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));

        assertThatThrownBy(() -> npcEditor.openShop(npc.getId(),
                new ShopSaveRequest(ShopKind.PILL_SHOP, List.of(new ShopItemRequest(sword.getId(), null)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("단약방은 법기를 팔 수 없습니다.");
        flushAndClear();
        assertThat(npcFinder.find(npc.getId()).hasShop()).isFalse();
    }

    @Test
    void openShopInvalidRequestFail() {
        Npc npc = npcEditor.create(createNpcSaveRequest("왕씨", null));

        assertThatThrownBy(() -> npcEditor.openShop(npc.getId(),
                new ShopSaveRequest(ShopKind.PILL_SHOP, List.of(new ShopItemRequest(1L, 0)))))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> npcEditor.openShop(npc.getId(), new ShopSaveRequest(null, List.of())))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private Room prepareRoom(String name) {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청우촌"));
        return roomEditor.create(createRoomSaveRequest(zone.getId(), name));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
