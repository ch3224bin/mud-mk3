package mudmk3.application.npc.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcFixture;
import mudmk3.domain.npc.NpcTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record NpcRepositoryTest(NpcRepository npcRepository, EntityManager entityManager) {

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
    void existsByName() {
        Npc npc = npcRepository.save(NpcFixture.createNpc("왕씨"));
        Npc other = npcRepository.save(NpcFixture.createNpc("이씨"));

        assertThat(npcRepository.existsByName("왕씨")).isTrue();
        assertThat(npcRepository.existsByName("박씨")).isFalse();
        assertThat(npcRepository.existsByNameAndIdNot("왕씨", npc.getId())).isFalse();
        assertThat(npcRepository.existsByNameAndIdNot("왕씨", other.getId())).isTrue();
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

    private Npc placed(String name, Long roomId) {
        Npc npc = Npc.create(NpcFixture.createNpcSaveInfo(name));
        npc.placeIn(roomId);
        return npcRepository.save(npc);
    }
}
