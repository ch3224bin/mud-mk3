package mudmk3.application.npc.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcFixture;
import mudmk3.domain.npc.NpcTopic;
import mudmk3.domain.npc.ShopItem;
import mudmk3.domain.npc.ShopKind;
import mudmk3.domain.npc.ShopOffer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record NpcRepositoryTest(NpcRepository npcRepository, EntityManager entityManager) {

    @Test
    void saveWithShopItems() {
        ItemType pill = persist(ItemTypeFixture.createItemType("회춘단", ItemCategory.PILL, 3));
        ItemType other = persist(ItemTypeFixture.createItemType("청심단", ItemCategory.PILL, 5));
        Npc npc = NpcFixture.createNpc("왕씨");
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(other, 12), new ShopOffer(pill, null)));
        npcRepository.save(npc);
        entityManager.flush();
        entityManager.clear();

        Npc found = npcRepository.findById(npc.getId()).orElseThrow();
        entityManager.clear();

        // 트랜잭션 밖 응답이 읽을 수 있게 상점 물건도 함께 읽는다
        assertThat(found.findShopKind()).contains(ShopKind.PILL_SHOP);
        assertThat(found.getShopItems()).containsExactly(new ShopItem(other.getId(), 12), new ShopItem(pill.getId(), null));
        assertThat(found.getTopics()).hasSize(2);
    }

    @Test
    void closeShopRemovesRows() {
        ItemType pill = persist(ItemTypeFixture.createItemType("회춘단", ItemCategory.PILL, 3));
        Npc npc = NpcFixture.createNpc("왕씨");
        npc.openShop(ShopKind.PILL_SHOP, List.of(new ShopOffer(pill, null)));
        npcRepository.save(npc);
        entityManager.flush();
        entityManager.clear();

        npcRepository.findById(npc.getId()).orElseThrow().closeShop();
        entityManager.flush();
        entityManager.clear();

        Npc found = npcRepository.findById(npc.getId()).orElseThrow();
        assertThat(found.hasShop()).isFalse();
        assertThat(entityManager.createNativeQuery("select count(*) from npc_shop_item").getSingleResult()).isEqualTo(0L);
    }


    @Test
    void saveWithTopics() {
        Npc npc = NpcFixture.createNpc("왕씨");
        npc.placeIn(7L);
        npcRepository.save(npc);
        entityManager.flush();
        entityManager.clear();

        Npc found = npcRepository.findById(npc.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("왕씨");
        assertThat(found.getGreeting()).isEqualTo("어서 오시오.");
        assertThat(found.getDefaultAnswer()).isEqualTo("글쎄, 잘 모르겠구려.");
        assertThat(found.isLlmEnabled()).isFalse();
        assertThat(found.findRoomId()).contains(7L);
        assertThat(found.getTopics()).containsExactly(
                new NpcTopic("단약", "단약은 귀한 물건이오."),
                new NpcTopic("일거리", "숲의 늑대를 잡아 주시오."));
    }

    @Test
    void replaceTopicsKeepsOrder() {
        Npc npc = npcRepository.save(NpcFixture.createNpc("왕씨"));
        entityManager.flush();
        entityManager.clear();

        Npc loaded = npcRepository.findById(npc.getId()).orElseThrow();
        loaded.update(NpcFixture.createNpcSaveInfo("왕씨", List.of(
                new NpcTopic("일거리", "없소."),
                new NpcTopic("단약", "비싸오."),
                new NpcTopic("길", "북쪽이오."))));
        entityManager.flush();
        entityManager.clear();

        assertThat(npcRepository.findById(npc.getId()).orElseThrow().getTopics())
                .extracting(NpcTopic::word)
                .containsExactly("일거리", "단약", "길");
    }

    @Test
    void duplicateNameFail() {
        npcRepository.save(NpcFixture.createNpc("왕씨"));

        assertThatThrownBy(() -> {
            npcRepository.save(NpcFixture.createNpc("왕씨"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByRoomIdOrderByIdAsc() {
        Npc first = placed("왕씨", 1L);
        Npc second = placed("이씨", 1L);
        placed("박씨", 2L);
        npcRepository.save(NpcFixture.createNpc("최씨"));
        entityManager.flush();
        entityManager.clear();

        List<Npc> found = npcRepository.findAllByRoomIdOrderByIdAsc(1L);

        assertThat(found).extracting(Npc::getName).containsExactly("왕씨", "이씨");
        assertThat(found).containsExactly(first, second);
    }

    @Test
    void findAllByOrderByNameAsc() {
        npcRepository.save(NpcFixture.createNpc("이씨"));
        npcRepository.save(NpcFixture.createNpc("왕씨"));
        entityManager.flush();
        entityManager.clear();

        List<Npc> found = npcRepository.findAllByOrderByNameAsc();
        entityManager.clear();

        // 트랜잭션 밖 응답이 낱말 수를 읽을 수 있게 낱말까지 함께 읽는다
        assertThat(found).extracting(Npc::getName).containsExactly("왕씨", "이씨");
        assertThat(found).allSatisfy(npc -> assertThat(npc.getTopics()).hasSize(2));
    }

    @Test
    void existsByNameIgnoreCase() {
        npcRepository.save(NpcFixture.createNpc("Wang"));

        assertThat(npcRepository.existsByNameIgnoreCase("wANG")).isTrue();
        assertThat(npcRepository.existsByNameIgnoreCase("Wan")).isFalse();
    }

    @Test
    void delete() {
        Npc npc = npcRepository.save(NpcFixture.createNpc());
        entityManager.flush();

        npcRepository.delete(npc);
        entityManager.flush();

        assertThat(npcRepository.findById(npc.getId())).isEmpty();
        assertThat(entityManager.createNativeQuery("select count(*) from npc_topic").getSingleResult())
                .isEqualTo(0L);
    }

    private ItemType persist(ItemType itemType) {
        entityManager.persist(itemType);
        return itemType;
    }

    private Npc placed(String name, Long roomId) {
        Npc npc = Npc.create(NpcFixture.createNpcSaveInfo(name));
        npc.placeIn(roomId);
        return npcRepository.save(npc);
    }
}
