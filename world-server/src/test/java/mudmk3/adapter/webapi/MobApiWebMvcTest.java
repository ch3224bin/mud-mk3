package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.mob.provided.DeletedMobType;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.provided.MobTypeSaveRequest;
import mudmk3.application.mob.provided.MobTypeView;
import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.DuplicateMobNameException;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.mob.MobTypeSaveInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Map;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.withDrops;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(MobApi.class)
@ExtendWith(OutputCaptureExtension.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {MobTypeFinder.class, MobTypeEditor.class})
record MobApiWebMvcTest(MockMvcTester mvc, MobTypeFinder mobTypeFinder, MobTypeEditor mobTypeEditor) {

    private static final String WOLF_JSON = """
            {"name":"청모랑","description":"청모랑의 묘사다.","kind":"BEAST","realm":"QI_2","tier":"NORMAL",
             "attackName":"물어뜯기","attackCoefficient":null,"respawnSeconds":null,
             "fixedDrops":[{"itemTypeId":5,"count":1}],"tierDrops":[{"tier":"ADVANCED","itemTypeId":6}]}""";

    private static final Map<Long, String> ITEM_NAMES = Map.of(5L, "청모랑 가죽", 6L, "1급 요단");

    @Test
    void create() {
        when(mobTypeEditor.create(any())).thenReturn(view(wolfWithDrops(), 1L, List.of()));

        var body = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WOLF_JSON))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson();
        body.isStrictlyEqualTo("""
                {"id":1,"name":"청모랑","description":"청모랑의 묘사다.","kind":"BEAST","kindName":"요수",
                 "realm":"QI_2","realmName":"연기 2층","tier":"NORMAL","tierName":"일반","rankName":"1급",
                 "attackName":"물어뜯기","attackCoefficient":null,"respawnSeconds":null,"effectiveRespawnSeconds":90,
                 "stats":{"qiBlood":49,"body":8,"power":8},
                 "fixedDrops":[{"itemTypeId":5,"itemTypeName":"청모랑 가죽","count":1}],
                 "tierDrops":[{"tier":"ADVANCED","tierName":"고급","itemTypeId":6,"itemTypeName":"1급 요단"}],
                 "placements":[]}""");

        ArgumentCaptor<MobTypeSaveRequest> captor = ArgumentCaptor.forClass(MobTypeSaveRequest.class);
        verify(mobTypeEditor).create(captor.capture());
        assertThat(captor.getValue().toInfo()).isEqualTo(withDrops(wolf(), List.of(new MobFixedDrop(5L, 1)),
                List.of(new MobTierDrop(DropTier.ADVANCED, 6L))));
    }

    @Test
    void createBoss() {
        when(mobTypeEditor.create(any())).thenReturn(view(MobType.create(boss(), Map.of()), 2L, List.of()));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"쌍두 흑망","kind":"BEAST","realm":"QI_12","tier":"BOSS","attackName":"두 머리 물기",
                         "attackCoefficient":0.6,"respawnSeconds":2700}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":2,"tierName":"두목","rankName":"2급","attackCoefficient":0.6,"respawnSeconds":2700,
                         "effectiveRespawnSeconds":2700,"stats":{"qiBlood":1700,"body":24,"power":19}}""");
    }

    @Test
    void createWithMissingValues() {
        when(mobTypeEditor.create(any())).thenReturn(view(MobType.create(wolf(), Map.of()), 1L, List.of()));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"청모랑","kind":"BEAST","realm":"QI_2","tier":"NORMAL","attackName":"물어뜯기"}"""))
                .hasStatus(HttpStatus.CREATED);

        // 빠진 묘사는 빈 글, 빠진 목록은 빈 목록
        ArgumentCaptor<MobTypeSaveRequest> captor = ArgumentCaptor.forClass(MobTypeSaveRequest.class);
        verify(mobTypeEditor).create(captor.capture());
        MobTypeSaveInfo info = captor.getValue().toInfo();
        assertThat(info.description()).isEmpty();
        assertThat(info.attackCoefficient()).isNull();
        assertThat(info.respawnSeconds()).isNull();
        assertThat(info.fixedDrops()).isEmpty();
        assertThat(info.tierDrops()).isEmpty();
    }

    /** 요청 컴팩트 생성자의 {@code IllegalArgumentException} 과 잘못된 enum 은 Jackson 을 거쳐 공통 400 이 된다 */
    @Test
    void createUnreadableBodyFail() {
        List<String> bodies = List.of(
                WOLF_JSON.replace("\"kind\":\"BEAST\"", "\"kind\":null"),
                WOLF_JSON.replace("\"realm\":\"QI_2\"", "\"realm\":null"),
                WOLF_JSON.replace("\"tier\":\"NORMAL\",", ""),
                WOLF_JSON.replace("\"tier\":\"NORMAL\"", "\"tier\":\"GIANT\""),
                WOLF_JSON.replace("\"tier\":\"ADVANCED\"", "\"tier\":\"EPIC\""),
                WOLF_JSON.replace("\"tier\":\"ADVANCED\",", ""),
                WOLF_JSON.replace("{\"itemTypeId\":5,\"count\":1}", "{\"count\":1}"),
                WOLF_JSON.replace(",\"count\":1}", "}"),
                WOLF_JSON.replace("[{\"itemTypeId\":5,\"count\":1}]", "[null]"));

        for (String json : bodies) {
            assertThat(json).isNotEqualTo(WOLF_JSON);
            assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/mobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.detail").isEqualTo("요청 본문을 읽을 수 없습니다");
        }
        verifyNoInteractions(mobTypeEditor);
    }

    @Test
    void createDomainRuleFail() {
        when(mobTypeEditor.create(any())).thenThrow(new IllegalArgumentException("공격 계수는 두목만 정합니다."));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WOLF_JSON.replace("\"attackCoefficient\":null", "\"attackCoefficient\":0.6")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("공격 계수는 두목만 정합니다.");
    }

    @Test
    void list() {
        when(mobTypeFinder.viewAll()).thenReturn(List.of(
                view(MobType.create(rabbit(), Map.of()), 2L, List.of()),
                view(MobType.create(wolf(), Map.of()), 1L, List.of(
                        new MobTypeView.PlacementView(3L, 10L, "샘터", 2),
                        new MobTypeView.PlacementView(4L, 11L, "오솔길", 1)))));

        assertThat(mvc.get().with(admin()).uri("/api/admin/mobs"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"id":2,"name":"들토끼","kind":"BEAST","kindName":"요수","rankName":"1급","tier":"NORMAL",
                          "tierName":"일반","stats":{"qiBlood":42,"body":7,"power":7},"effectiveRespawnSeconds":90,
                          "placementCount":0},
                         {"id":1,"name":"청모랑","kind":"BEAST","kindName":"요수","rankName":"1급","tier":"NORMAL",
                          "tierName":"일반","stats":{"qiBlood":49,"body":8,"power":8},"effectiveRespawnSeconds":90,
                          "placementCount":2}]""");
    }

    @Test
    void find() {
        when(mobTypeFinder.view(1L)).thenReturn(view(MobType.create(wolf(), Map.of()), 1L,
                List.of(new MobTypeView.PlacementView(3L, 10L, "샘터", 2))));

        assertThat(mvc.get().with(admin()).uri("/api/admin/mobs/1"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":1,"name":"청모랑","placements":[{"id":3,"roomId":10,"roomName":"샘터","count":2}]}""");
    }

    @Test
    void findUnknownFail() {
        when(mobTypeFinder.view(9L)).thenThrow(new IllegalArgumentException("몹을 찾을 수 없습니다. id: 9"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/mobs/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("몹을 찾을 수 없습니다. id: 9");
    }

    @Test
    void update() {
        when(mobTypeEditor.update(eq(1L), any())).thenReturn(view(wolfWithDrops(), 1L, List.of()));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/mobs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WOLF_JSON))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":1,"name":"청모랑","fixedDrops":[{"itemTypeName":"청모랑 가죽"}]}""");
        verify(mobTypeEditor).update(eq(1L), any());
    }

    @Test
    void createDuplicateNameFail() {
        when(mobTypeEditor.create(any())).thenThrow(new DuplicateMobNameException("이미 있는 몹 이름입니다: 청모랑"));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WOLF_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("이미 있는 몹 이름입니다: 청모랑");
    }

    @Test
    void delete() {
        when(mobTypeEditor.delete(1L)).thenReturn(new DeletedMobType(1L, "청모랑", 0));

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/mobs/1"))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(mobTypeEditor).delete(1L);
    }

    /** 지운 뒤 누가·무엇을 지웠는지 로그 한 줄 */
    @Test
    void deleteLogs(CapturedOutput output) {
        when(mobTypeEditor.delete(1L)).thenReturn(new DeletedMobType(1L, "청모랑", 2));

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/mobs/1"))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(output).contains(
                "관리자 삭제 — 몹 id: 1, 이름: 청모랑, 함께 지운 배치: 2, 관리자 계정 id: 1, 이메일: admin@example.com");
    }

    /** 거절된 삭제는 로그에 남기지 않는다 */
    @Test
    void deleteUnknownFail(CapturedOutput output) {
        when(mobTypeEditor.delete(9L)).thenThrow(new IllegalArgumentException("몹을 찾을 수 없습니다. id: 9"));

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/mobs/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("몹을 찾을 수 없습니다. id: 9");
        assertThat(output).doesNotContain("관리자 삭제");
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/mobs")).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(mobTypeFinder);
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/mobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(WOLF_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(mobTypeEditor);
    }

    private static MobType wolfWithDrops() {
        return MobType.create(withDrops(wolf(), List.of(new MobFixedDrop(5L, 1)),
                List.of(new MobTierDrop(DropTier.ADVANCED, 6L))), ITEM_NAMES);
    }

    static MobTypeView view(MobType mobType, Long id, List<MobTypeView.PlacementView> placements) {
        ReflectionTestUtils.setField(mobType, "id", id);
        return MobTypeView.of(mobType, ITEM_NAMES, placements);
    }
}
