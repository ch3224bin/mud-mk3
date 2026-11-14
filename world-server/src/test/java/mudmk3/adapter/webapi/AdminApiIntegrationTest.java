package mudmk3.adapter.webapi;

import mudmk3.application.inventory.provided.InventoryEditor;
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
record AdminApiIntegrationTest(MockMvcTester mvc, InventoryEditor inventoryEditor) {

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

    @Test
    void deleteItemTypeInInventoryFail() {
        long itemTypeId = id(post("/api/admin/item-types", """
                {"name":"늑대 가죽","category":"MATERIAL","baseValue":2,"tradable":true}"""));
        inventoryEditor.addUpTo(900L, itemTypeId, 3);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(itemTypeId)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("쓰이는 물건은 지울 수 없습니다: 늑대 가죽");
        assertThat(put("/api/admin/item-types/%d".formatted(itemTypeId), """
                {"name":"늑대 가죽","category":"PILL","baseValue":2,"tradable":true}"""))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void manageStartingItems() {
        var itemTypes = assertThat(mvc.get().with(admin()).uri("/api/admin/item-types")).hasStatusOk().bodyJson();
        long lowStoneId = ((Number) itemTypes.extractingPath("$[0].id").actual()).longValue();
        long bagId = id(post("/api/admin/item-types", """
                {"name":"하급 저물대","category":"BAG","baseValue":10,"tradable":true,"bagGrade":"LOW"}"""));
        long pillId = id(post("/api/admin/item-types", """
                {"name":"회기단(연기 초)","category":"PILL","baseValue":3,"tradable":true}"""));

        var saved = assertThat(put("/api/admin/world/starting-items", """
                {"items":[{"itemTypeId":%d,"count":1},{"itemTypeId":%d,"count":30},{"itemTypeId":%d,"count":3}]}"""
                .formatted(bagId, lowStoneId, pillId))).hasStatusOk().bodyJson();
        saved.extractingPath("$[*].itemName").asArray().containsExactly("하급 저물대", "하품 영석", "회기단(연기 초)");

        var found = assertThat(mvc.get().with(admin()).uri("/api/admin/world/starting-items")).hasStatusOk().bodyJson();
        found.extractingPath("$[*].itemName").asArray().containsExactly("하급 저물대", "하품 영석", "회기단(연기 초)");
        found.extractingPath("$[*].count").asArray().containsExactly(1, 30, 3);

        assertThat(put("/api/admin/world/starting-items", """
                {"items":[{"itemTypeId":%d,"count":3},{"itemTypeId":%d,"count":2}]}""".formatted(pillId, pillId)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo("같은 물건이 두 번 있습니다: 회기단(연기 초)");
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(pillId)))
                .hasStatus(HttpStatus.CONFLICT);

        assertThat(put("/api/admin/world/starting-items", """
                {"items":[]}""")).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(pillId)))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void manageFloorItems() {
        long zoneId = id(post("/api/admin/zones", """
                {"name":"흑풍림"}"""));
        long roomId = id(post("/api/admin/rooms", """
                {"zoneId":%d,"name":"늑대 굴","description":"늑대 냄새가 짙다."}""".formatted(zoneId)));
        long hideId = id(post("/api/admin/item-types", """
                {"name":"검은 늑대 가죽","category":"MATERIAL","baseValue":2,"tradable":true}"""));
        long pillId = id(post("/api/admin/item-types", """
                {"name":"청심단","category":"PILL","baseValue":3,"tradable":true}"""));

        var created = assertThat(post("/api/admin/rooms/%d/floor-items".formatted(roomId), """
                {"itemTypeId":%d,"count":3,"respawnMinutes":10}""".formatted(hideId))).hasStatus(HttpStatus.CREATED).bodyJson();
        created.extractingPath("$.itemName").isEqualTo("검은 늑대 가죽");
        long floorItemId = id(post("/api/admin/rooms/%d/floor-items".formatted(roomId), """
                {"itemTypeId":%d,"count":1,"respawnMinutes":null}""".formatted(hideId)));

        // 트랜잭션 밖에서 응답을 만들어도 물건 이름을 낸다
        var room = assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/%d".formatted(roomId))).hasStatusOk().bodyJson();
        room.extractingPath("$.floorItems[*].itemName").asArray().containsExactly("검은 늑대 가죽", "검은 늑대 가죽");
        room.extractingPath("$.floorItems[1].respawnMinutes").isNull();
        room.extractingPath("$.floorItems[1].remaining").isEqualTo(1);

        var updated = assertThat(put("/api/admin/floor-items/%d".formatted(floorItemId), """
                {"itemTypeId":%d,"count":5,"respawnMinutes":30}""".formatted(pillId))).hasStatusOk().bodyJson();
        updated.extractingPath("$.itemName").isEqualTo("청심단");
        updated.extractingPath("$.remaining").isEqualTo(5);
        assertThat(post("/api/admin/rooms/%d/floor-items".formatted(roomId), """
                {"itemTypeId":%d,"count":0}""".formatted(pillId))).hasStatus(HttpStatus.BAD_REQUEST);

        // 바닥에 둔 물건 종류는 실제 DB에서도 지울 수 없다
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(pillId)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("쓰이는 물건은 지울 수 없습니다: 청심단");

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/floor-items/%d".formatted(floorItemId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/floor-items/%d".formatted(floorItemId)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo("바닥 물건을 찾을 수 없습니다. id: " + floorItemId);
        assertThat(mvc.get().with(admin()).uri("/api/admin/rooms/%d".formatted(roomId))).hasStatusOk().bodyJson()
                .extractingPath("$.floorItems[*].itemName").asArray().containsExactly("검은 늑대 가죽");
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(pillId)))
                .hasStatus(HttpStatus.NO_CONTENT);

        // 방을 지우면 남은 배치도 지워져 그 종류를 지울 수 있다. 같은 DB를 쓰는 다른 테스트를 흐리지 않게 지역도 지운다
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/rooms/%d".formatted(roomId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(hideId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/zones/%d".formatted(zoneId)))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void manageShop() {
        long pillId = id(post("/api/admin/item-types", """
                {"name":"보원단","category":"PILL","baseValue":3,"tradable":true}"""));
        long swordId = id(post("/api/admin/item-types", """
                {"name":"청강검","category":"ARTIFACT","baseValue":50,"tradable":true}"""));
        long npcId = id(post("/api/admin/npcs", """
                {"name":"주씨","greeting":"어서 오시오.","defaultAnswer":"글쎄.","llmEnabled":false}"""));

        var opened = assertThat(put("/api/admin/npcs/%d/shop".formatted(npcId), """
                {"kind":"PILL_SHOP","items":[{"itemTypeId":%d,"price":null}]}""".formatted(pillId))).hasStatusOk().bodyJson();
        opened.extractingPath("$.shop.kindName").isEqualTo("단약방");
        opened.extractingPath("$.shop.items[0].effectivePrice").isEqualTo(3);

        // 트랜잭션 밖에서 응답을 만들어도 상점 물건 이름을 낸다
        var detail = assertThat(mvc.get().with(admin()).uri("/api/admin/npcs/%d".formatted(npcId))).hasStatusOk().bodyJson();
        detail.extractingPath("$.shop.kind").isEqualTo("PILL_SHOP");
        detail.extractingPath("$.shop.items[0].itemName").isEqualTo("보원단");
        detail.extractingPath("$.shop.items[0].price").isNull();
        assertThat(mvc.get().with(admin()).uri("/api/admin/npcs")).hasStatusOk().bodyJson()
                .extractingPath("$[?(@.id==%d)].shopKindName".formatted(npcId)).asArray().containsExactly("단약방");

        assertThat(put("/api/admin/npcs/%d/shop".formatted(npcId), """
                {"kind":"PILL_SHOP","items":[{"itemTypeId":%d}]}""".formatted(swordId)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo("단약방은 법기를 팔 수 없습니다.");
        assertThat(put("/api/admin/item-types/%d".formatted(pillId), """
                {"name":"보원단","category":"PILL","baseValue":0,"tradable":true}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("기준가가 0이면 상점이 값을 정해야 합니다: 보원단 (주씨)");
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(pillId)))
                .hasStatus(HttpStatus.CONFLICT);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/npcs/%d/shop".formatted(npcId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().with(admin()).uri("/api/admin/npcs/%d".formatted(npcId))).hasStatusOk().bodyJson()
                .extractingPath("$.shop").isNull();
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(pillId)))
                .hasStatus(HttpStatus.NO_CONTENT);

        // 같은 DB를 쓰는 다른 테스트를 흐리지 않게 지운다
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/npcs/%d".formatted(npcId)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/%d".formatted(swordId)))
                .hasStatus(HttpStatus.NO_CONTENT);
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
