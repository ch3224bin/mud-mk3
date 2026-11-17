package mudmk3.adapter.webapi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import mudmk3.QueryCounter;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.domain.npc.NpcFixture.createNpcSaveRequest;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * NPC 목록 요청이 내는 쿼리 수: 배치된 방은 NPC 수와 상관없이 한 번에 읽는다.
 * {@link AdminApiIntegrationTest} 와 같은 DB·컨텍스트를 쓰되 테스트마다 롤백한다(요청이 테스트 트랜잭션에 들어온다)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:admin-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@Transactional
class NpcApiQueryCountTest {

    private final MockMvcTester mvc;
    private final NpcEditor npcEditor;
    private final ZoneEditor zoneEditor;
    private final RoomEditor roomEditor;
    private final EntityManager entityManager;
    private final QueryCounter queryCounter;

    NpcApiQueryCountTest(MockMvcTester mvc, NpcEditor npcEditor, ZoneEditor zoneEditor, RoomEditor roomEditor,
                         EntityManager entityManager, EntityManagerFactory entityManagerFactory) {
        this.mvc = mvc;
        this.npcEditor = npcEditor;
        this.zoneEditor = zoneEditor;
        this.roomEditor = roomEditor;
        this.entityManager = entityManager;
        this.queryCounter = new QueryCounter(entityManagerFactory);
    }

    @Test
    void listQueryCountConstant() {
        Long zoneId = zoneEditor.create(createZoneSaveRequest("청우촌")).getId();
        npcEditor.create(createNpcSaveRequest("왕씨", roomEditor.create(createRoomSaveRequest(zoneId, "마을 어귀")).getId()));
        long oneNpc = countList();

        npcEditor.create(createNpcSaveRequest("이씨", roomEditor.create(createRoomSaveRequest(zoneId, "우물가")).getId()));
        npcEditor.create(createNpcSaveRequest("박씨", roomEditor.create(createRoomSaveRequest(zoneId, "주막")).getId()));
        long threeNpcs = countList();

        assertThat(threeNpcs).isEqualTo(oneNpc);
    }

    private long countList() {
        entityManager.flush();
        entityManager.clear();
        queryCounter.reset();
        assertThat(mvc.get().with(admin()).uri("/api/admin/npcs")).hasStatusOk();
        return queryCounter.prepared();
    }
}
