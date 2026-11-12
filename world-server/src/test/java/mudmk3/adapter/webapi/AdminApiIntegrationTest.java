package mudmk3.adapter.webapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 서비스·DB로 관리 API를 끝까지 부른다. 트랜잭션 밖(open-in-view 꺼짐)에서 응답 DTO를 만들 때
 * LAZY 연관 때문에 실패하지 않는지 확인한다. 다른 테스트와 데이터가 섞이지 않게 DB를 따로 쓴다.
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

        var roomB = assertThat(mvc.get().uri("/api/admin/rooms/%d".formatted(b))).hasStatusOk().bodyJson();
        roomB.extractingPath("$.zoneName").isEqualTo("황풍곡");
        roomB.extractingPath("$.exits[0].directionName").isEqualTo("남");
        roomB.extractingPath("$.exits[0].targetZoneName").isEqualTo("청석숲");

        assertThat(mvc.get().uri("/api/admin/zones/%d/rooms".formatted(zoneId))).hasStatusOk().bodyJson()
                .extractingPath("$[0].exitCount").isEqualTo(1);
        assertThat(mvc.get().uri("/api/admin/zones")).hasStatusOk().bodyJson()
                .extractingPath("$[0].roomCount").isEqualTo(1);

        assertThat(mvc.get().uri("/api/admin/world")).hasStatusOk().bodyJson()
                .extractingPath("$.startRoomId").isNull();
        assertThat(put("/api/admin/world/start-room", """
                {"roomId":%d}""".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.startZoneName").isEqualTo("청석숲");
        // 같은 방을 다시 지정해도(설정의 시작 방이 프록시로 먼저 읽혀도) 응답을 만든다
        assertThat(put("/api/admin/world/start-room", """
                {"roomId":%d}""".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.startZoneName").isEqualTo("청석숲");
        var world = assertThat(mvc.get().uri("/api/admin/world")).hasStatusOk().bodyJson();
        world.extractingPath("$.startRoomName").isEqualTo("샘터");
        world.extractingPath("$.startZoneName").isEqualTo("청석숲");
        assertThat(mvc.get().uri("/api/admin/rooms/%d".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.start").isEqualTo(true);

        assertThat(mvc.delete().uri("/api/admin/rooms/%d".formatted(a))).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.delete().uri("/api/admin/rooms/%d".formatted(b))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/admin/rooms/%d".formatted(a))).hasStatusOk().bodyJson()
                .extractingPath("$.exits").asArray().isEmpty();
        assertThat(mvc.delete().uri("/api/admin/zones/%d".formatted(otherZoneId))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().uri("/api/admin/zones/%d".formatted(zoneId))).hasStatus(HttpStatus.CONFLICT);
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult put(String uri, String json) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private long id(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ((Number) assertThat(result).bodyJson().extractingPath("$.id").actual()).longValue();
    }
}
