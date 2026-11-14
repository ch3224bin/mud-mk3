package mudmk3.adapter.webapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 서비스·DB로 관리 API를 끝까지 부른다. 트랜잭션 밖(open-in-view 꺼짐)에서 응답 DTO를 만들 때
 * LAZY 연관 때문에 실패하지 않는지 확인한다. 다른 테스트와 데이터가 섞이지 않게 DB를 따로 쓴다.
 * 실제 보안 필터를 거치므로 관리자로 로그인하고, 변경 요청에는 {@code X-MUD-Client: 1} 헤더를 붙인다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:admin-api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
record AdminApiIntegrationTest(MockMvcTester mvc) {

    @Test
    void manageMap() {
        long zoneId = id(post("/api/admin/zones", """
                {"name":"청석숲","description":"푸른 돌이 흩어진 숲이다."}"""));
        long otherZoneId = id(post("/api/admin/zones", """
                {"name":"황풍곡"}"""));
        long a = id(post("/api/admin/rooms", """
                {"zoneId":%d,"name":"샘터","description":"맑은 샘이 솟는다."}""".formatted(zoneId)));
        long b = id(post("/api/admin/rooms", """
                {"zoneId":%d,"name":"바람 언덕","description":"바람이 거세다."}""".formatted(otherZoneId)));

        var putExit = assertThat(put("/api/admin/rooms/%d/exits/NORTH".formatted(a), """
                {"targetRoomId":%d,"twoWay":true}""".formatted(b))).hasStatusOk().bodyJson();
        putExit.extractingPath("$.zoneName").isEqualTo("청석숲");
        putExit.extractingPath("$.exits[0].targetRoomName").isEqualTo("바람 언덕");
        putExit.extractingPath("$.exits[0].targetZoneName").isEqualTo("황풍곡");

        var roomB = assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/%d".formatted(b))).hasStatusOk().bodyJson();
        roomB.extractingPath("$.zoneName").isEqualTo("황풍곡");
        roomB.extractingPath("$.exits[0].directionName").isEqualTo("남");
        roomB.extractingPath("$.exits[0].targetZoneName").isEqualTo("청석숲");

        assertThat(mvc.get().with(admin()).uri("/api/admin/zones/%d/rooms".formatted(zoneId))).hasStatusOk().bodyJson()
                .extractingPath("$[0].exitCount").isEqualTo(1);
        assertThat(mvc.get().with(admin()).uri("/api/admin/zones")).hasStatusOk().bodyJson()
                .extractingPath("$[0].roomCount").isEqualTo(1);

        assertThat(mvc.get().with(admin()).uri("/api/admin/world")).hasStatusOk().bodyJson()
                .extractingPath("$.startRoomId").isNull();
        assertThat(put("/api/admin/world/start-room", """
                {"roomId":%d}""".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.startZoneName").isEqualTo("청석숲");
        // 같은 방을 다시 지정해도(설정의 시작 방이 프록시로 먼저 읽혀도) 응답을 만든다
        assertThat(put("/api/admin/world/start-room", """
                {"roomId":%d}""".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.startZoneName").isEqualTo("청석숲");
        var world = assertThat(mvc.get().with(admin()).uri("/api/admin/world")).hasStatusOk().bodyJson();
        world.extractingPath("$.startRoomName").isEqualTo("샘터");
        world.extractingPath("$.startZoneName").isEqualTo("청석숲");
        assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/%d".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.start").isEqualTo(true);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/%d".formatted(a))).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/%d".formatted(b))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/%d".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.exits").asArray().isEmpty();
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/%d".formatted(otherZoneId))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/%d".formatted(zoneId))).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void manageFactionsAndOwnership() {
        long factionId = id(post("/api/admin/factions", """
                {"kind":"CASTLE","name":"청석성","description":"푸른 돌로 쌓은 성이다."}"""));
        long zoneId = id(post("/api/admin/zones", """
                {"name":"청석성 남문","ownerId":%d}""".formatted(factionId)));

        var zones = assertThat(mvc.get().with(admin()).uri("/api/admin/zones")).hasStatusOk().bodyJson();
        zones.extractingPath("$[?(@.id==%d)].ownerName".formatted(zoneId)).asArray().containsExactly("청석성");
        zones.extractingPath("$[?(@.id==%d)].ownerKind".formatted(zoneId)).asArray().containsExactly("CASTLE");
        assertThat(mvc.get().with(admin()).uri("/api/admin/factions")).hasStatusOk().bodyJson()
                .extractingPath("$[?(@.id==%d)].zoneCount".formatted(factionId)).asArray().containsExactly(1);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/factions/%d".formatted(factionId)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("소유한 지역이 있는 성·종문은 지울 수 없습니다");

        assertThat(put("/api/admin/zones/%d".formatted(zoneId), """
                {"name":"청석성 남문","ownerId":null}""")).hasStatusOk().bodyJson()
                .extractingPath("$.ownerName").isNull();
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/factions/%d".formatted(factionId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        // 같은 DB를 쓰는 다른 테스트의 지역 목록 순서를 흐리지 않게 지운다
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/%d".formatted(zoneId)))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void manageNpcs() {
        long zoneId = id(post("/api/admin/zones", """
                {"name":"청우촌"}"""));
        long roomId = id(post("/api/admin/rooms", """
                {"zoneId":%d,"name":"마을 어귀","description":"마을로 들어서는 길목이다."}""".formatted(zoneId)));
        String topics = """
                [{"word":"단약","answer":"단약은 귀한 물건이오."},{"word":"[일거리]","answer":"숲의 늑대를 잡아 주시오."}]""";
        long npcId = id(post("/api/admin/npcs", """
                {"name":"왕씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","topics":%s,"llmEnabled":false,"roomId":%d}"""
                .formatted(topics, roomId)));

        var detail = assertThat(mvc.get().with(admin()).uri("/api/admin/npcs/%d".formatted(npcId))).hasStatusOk().bodyJson();
        detail.extractingPath("$.roomName").isEqualTo("마을 어귀");
        detail.extractingPath("$.zoneName").isEqualTo("청우촌");
        detail.extractingPath("$.topics[1].word").isEqualTo("일거리");
        var list = assertThat(mvc.get().with(admin()).uri("/api/admin/npcs")).hasStatusOk().bodyJson();
        list.extractingPath("$[?(@.id==%d)].topicCount".formatted(npcId)).asArray().containsExactly(2);
        list.extractingPath("$[?(@.id==%d)].zoneName".formatted(npcId)).asArray().containsExactly("청우촌");
        assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/%d".formatted(roomId))).hasStatusOk().bodyJson()
                .extractingPath("$.npcs[0].name").isEqualTo("왕씨");

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/%d".formatted(roomId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        var unplaced = assertThat(mvc.get().with(admin()).uri("/api/admin/npcs/%d".formatted(npcId))).hasStatusOk().bodyJson();
        unplaced.extractingPath("$.roomId").isNull();
        unplaced.extractingPath("$.roomName").isNull();

        // 같은 DB를 쓰는 다른 테스트의 지역 목록 순서를 흐리지 않게 지운다
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/npcs/%d".formatted(npcId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/%d".formatted(zoneId)))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void manageItemTypes() {
        var list = assertThat(mvc.get().with(admin()).uri("/api/admin/item-types")).hasStatusOk().bodyJson();
        list.extractingPath("$[0:4].name").asArray().containsExactly("하품 영석", "중품 영석", "상품 영석", "극품 영석");
        list.extractingPath("$[0:4].spiritStone").asArray().containsOnly(true);

        long itemTypeId = id(post("/api/admin/item-types", """
                {"name":"회춘단","description":"몸을 되살리는 단약이다.","category":"PILL","baseValue":3,"tradable":true}"""));
        var found = assertThat(mvc.get().with(admin()).uri("/api/admin/item-types/%d".formatted(itemTypeId)))
                .hasStatusOk().bodyJson();
        found.extractingPath("$.name").isEqualTo("회춘단");
        found.extractingPath("$.categoryName").isEqualTo("단약");
        found.extractingPath("$.stackSize").isEqualTo(99);

        var updated = assertThat(put("/api/admin/item-types/%d".formatted(itemTypeId), """
                {"name":"회춘단(연기 초)","description":"몸을 되살리는 단약이다.","category":"PILL","baseValue":5,"tradable":false}"""))
                .hasStatusOk().bodyJson();
        updated.extractingPath("$.name").isEqualTo("회춘단(연기 초)");
        updated.extractingPath("$.baseValue").isEqualTo(5);
        updated.extractingPath("$.tradable").isEqualTo(false);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(itemTypeId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().with(admin()).uri("/api/admin/item-types/%d".formatted(itemTypeId)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo("물건 종류를 찾을 수 없습니다. id: " + itemTypeId);
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().with(admin()).with(clientHeader()).uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult put(String uri, String json) {
        return mvc.put().with(admin()).with(clientHeader()).uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private long id(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ((Number) assertThat(result).bodyJson().extractingPath("$.id").actual()).longValue();
    }
}
