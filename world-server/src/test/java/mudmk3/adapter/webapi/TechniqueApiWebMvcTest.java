package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.technique.provided.TechniqueEditor;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueInUseException;
import mudmk3.application.technique.provided.TechniqueSaveRequest;
import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.required.ScrollItem;
import mudmk3.domain.technique.AbilityStagePower;
import mudmk3.domain.technique.DuplicateTechniqueNameException;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.TechniqueStage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.util.List;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(TechniqueApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {TechniqueFinder.class, TechniqueEditor.class})
record TechniqueApiWebMvcTest(MockMvcTester mvc, TechniqueFinder techniqueFinder, TechniqueEditor techniqueEditor) {

    private static final String CHANGCHUN_JSON = """
            {"name":"장춘공","description":"목 속성 입문 공법.","elements":["WOOD"],"startRealm":"MORTAL",
             "stages":[{"insightRequired":0,"manaBonus":0},{"insightRequired":120,"manaBonus":2,"senseBonus":1,"aptitudeGain":1}],
             "abilities":[{"name":"화탄술","learnStage":1,"kind":"ATTACK","elements":["FIRE"],"bodyRefining":false,
                           "target":"SINGLE","costStat":"MANA","costAmount":5,"costPerTick":false,"castSeconds":0,
                           "cooldownSeconds":3,"usableWhen":"COMBAT_ONLY",
                           "effectLines":["{대상}에게 불꽃이 튄다!"],
                           "stagePowers":[{"stage":1,"basePower":2,"coefficient":1.2},
                                          {"stage":2,"basePower":2.2,"coefficient":1.2}]}]}""";

    @Test
    void create() {
        when(techniqueEditor.create(any())).thenReturn(view(TechniqueFixture.changchun(), 1L,
                List.of(new ScrollItem(9L, "장춘공 옥간"))));

        var body = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/techniques")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGCHUN_JSON))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson();
        body.isLenientlyEqualTo("""
                {"id":1,"name":"장춘공","elements":["WOOD"],"elementNames":"목","lineName":"오행",
                 "startRealm":"MORTAL","startRealmName":"범인","learnerCount":0,
                 "scrollItems":[{"id":9,"name":"장춘공 옥간"}]}""");
        body.extractingPath("$.stages").asArray().hasSize(9);
        body.isLenientlyEqualTo("""
                {"stages":[{"insightRequired":0,"aptitudeRequired":0,"manaBonus":0,"senseBonus":0,"spiritBonus":0,
                            "essenceBonus":0,"bodyBonus":0,"strengthBonus":0,"aptitudeGain":0},
                           {"insightRequired":120,"manaBonus":2,"senseBonus":1,"aptitudeGain":1},
                           {},{},{},{},{},{},{}]}""");
        body.isLenientlyEqualTo("""
                {"abilities":[{"name":"화탄술","learnStage":1,"kind":"ATTACK","kindName":"공격","elements":["FIRE"],
                               "bodyRefining":false,"target":"SINGLE","targetName":"한 대상","costStat":"MANA",
                               "costAmount":5.0,"costPerTick":false,"castSeconds":0.0,"cooldownSeconds":3.0,
                               "usableWhen":"COMBAT_ONLY","usableInGame":true,
                               "stagePowers":[{"stage":1,"basePower":2.00,"coefficient":1.200},{},{},{},{},{},{},{},{}]},
                              {"stagePowers":[],"usableInGame":false},{}]}""");
        body.extractingPath("$.abilities[0].effectLines").isEqualTo(TechniqueFixture.FIRE_BALL_LINES);
        // 저장한 모양(scale 2·3) 그대로 나간다
        assertThat(body.actual().getJson())
                .contains("\"stagePowers\":[{\"stage\":1,\"basePower\":2.00,\"coefficient\":1.200}");
        body.extractingPath("$.description").isEqualTo("범인도 수련을 시작할 수 있는 목 속성 입문 공법.");
    }

    @Test
    void createWithMissingValues() {
        when(techniqueEditor.create(any())).thenReturn(view(TechniqueFixture.ironBody(), 2L, List.of()));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/techniques")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"강체결","startRealm":"MORTAL","stages":[{}],
                         "abilities":[{"name":"철권","learnStage":1,"kind":"ATTACK","bodyRefining":true,
                                       "target":"SINGLE","costStat":"NONE","costPerTick":false,"usableWhen":"COMBAT_ONLY"}]}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.elementNames").isNull();

        ArgumentCaptor<TechniqueSaveRequest> captor = ArgumentCaptor.forClass(TechniqueSaveRequest.class);
        verify(techniqueEditor).create(captor.capture());
        TechniqueSaveRequest request = captor.getValue();
        assertThat(request.elements()).isEmpty();
        assertThat(request.toInfo().description()).isEmpty();
        assertThat(request.toInfo().stages()).containsExactly(new TechniqueStage(0, 0, 0, 0, 0, 0, 0, 0, 0));
        var ability = request.toInfo().abilities().getFirst();
        assertThat(ability.elements()).isEmpty();
        assertThat(ability.costAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ability.castSeconds()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ability.cooldownSeconds()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ability.effectLines()).isEmpty();
        assertThat(ability.stagePowers()).isEmpty();
    }

    @Test
    void createPassesEffectLinesAndStagePowers() {
        when(techniqueEditor.create(any())).thenReturn(view(TechniqueFixture.changchun(), 1L, List.of()));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/techniques")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGCHUN_JSON))
                .hasStatus(HttpStatus.CREATED);

        ArgumentCaptor<TechniqueSaveRequest> captor = ArgumentCaptor.forClass(TechniqueSaveRequest.class);
        verify(techniqueEditor).create(captor.capture());
        var ability = captor.getValue().toInfo().abilities().getFirst();
        assertThat(ability.effectLines()).containsExactly("{대상}에게 불꽃이 튄다!");
        assertThat(ability.stagePowers()).containsExactly(
                new AbilityStagePower(1, new BigDecimal("2"), new BigDecimal("1.2")),
                new AbilityStagePower(2, new BigDecimal("2.2"), new BigDecimal("1.2")));
    }

    @Test
    void createUnreadableBodyFail() {
        String wrongKind = CHANGCHUN_JSON.replace("\"kind\":\"ATTACK\"", "\"kind\":\"PUNCH\"");
        String nullStartRealm = CHANGCHUN_JSON.replace("\"startRealm\":\"MORTAL\"", "\"startRealm\":null");
        String nullKind = CHANGCHUN_JSON.replace("\"kind\":\"ATTACK\"", "\"kind\":null");

        // 신통의 boolean 칸은 빠지면 0 으로 채우는 숫자 칸과 달리 꼭 보내야 한다
        String missingCostPerTick = CHANGCHUN_JSON.replace("\"costPerTick\":false,", "");
        String missingBodyRefining = CHANGCHUN_JSON.replace("\"bodyRefining\":false,", "");

        // 단계별 값의 세 칸은 꼭 보내야 한다
        String nullStage = CHANGCHUN_JSON.replace("{\"stage\":1,", "{\"stage\":null,");
        String missingCoefficient = CHANGCHUN_JSON.replace(",\"coefficient\":1.2}", "}");

        for (String json : List.of(wrongKind, nullStartRealm, nullKind, missingCostPerTick, missingBodyRefining,
                nullStage, missingCoefficient)) {
            assertThat(json).isNotEqualTo(CHANGCHUN_JSON);
            assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/techniques")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.detail").isEqualTo("요청 본문을 읽을 수 없습니다");
        }
        verifyNoInteractions(techniqueEditor);
    }

    @Test
    void list() {
        when(techniqueFinder.viewAll()).thenReturn(List.of(
                view(TechniqueFixture.ironBody(), 2L, List.of()),
                view(TechniqueFixture.changchun(), 1L, List.of())));

        assertThat(mvc.get().with(admin()).uri("/api/admin/techniques"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"id":2,"name":"강체결","elements":[],"elementNames":null,"lineName":"연체술",
                          "startRealm":"MORTAL","startRealmName":"범인","stageCount":9,"abilityCount":1,
                          "learnerCount":0},
                         {"id":1,"name":"장춘공","elements":["WOOD"],"elementNames":"목","lineName":"오행",
                          "startRealm":"MORTAL","startRealmName":"범인","stageCount":9,"abilityCount":3,
                          "learnerCount":0}]""");
    }

    @Test
    void find() {
        when(techniqueFinder.view(1L)).thenReturn(view(TechniqueFixture.changchun(), 1L, List.of()));

        assertThat(mvc.get().with(admin()).uri("/api/admin/techniques/1"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":1,"name":"장춘공","scrollItems":[]}""");
    }

    @Test
    void findUnknownFail() {
        when(techniqueFinder.view(9L)).thenThrow(new IllegalArgumentException("공법을 찾을 수 없습니다. id: 9"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/techniques/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("공법을 찾을 수 없습니다. id: 9");
    }

    @Test
    void update() {
        when(techniqueEditor.update(any(), any())).thenReturn(view(TechniqueFixture.changchun(), 1L, List.of()));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/techniques/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGCHUN_JSON))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":1,"name":"장춘공"}""");
        verify(techniqueEditor).update(eq(1L), any());
    }

    @Test
    void createDuplicateNameFail() {
        when(techniqueEditor.create(any())).thenThrow(new DuplicateTechniqueNameException("이미 있는 공법 이름입니다: 장춘공"));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/techniques")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGCHUN_JSON))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("이미 있는 공법 이름입니다: 장춘공");
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/techniques/1"))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(techniqueEditor).delete(1L);
    }

    @Test
    void deleteInUseFail() {
        doThrow(new TechniqueInUseException("이 공법이 담긴 물건이 있어 지울 수 없습니다: 장춘공 옥간"))
                .when(techniqueEditor).delete(1L);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/techniques/1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("이 공법이 담긴 물건이 있어 지울 수 없습니다: 장춘공 옥간");
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/techniques")).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(techniqueFinder);
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/techniques")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CHANGCHUN_JSON))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(techniqueEditor);
    }

    private static TechniqueView view(Technique technique, Long id, List<ScrollItem> scrollItems) {
        ReflectionTestUtils.setField(technique, "id", id);
        return TechniqueView.of(technique, 0, scrollItems);
    }
}
