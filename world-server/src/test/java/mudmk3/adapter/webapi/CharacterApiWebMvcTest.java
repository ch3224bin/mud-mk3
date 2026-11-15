package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.cultivation.provided.CharacterSummary;
import mudmk3.application.cultivation.provided.CultivationFinder;
import mudmk3.application.cultivation.provided.CultivationProfile;
import mudmk3.application.cultivation.provided.LearnedTechniqueView;
import mudmk3.application.game.provided.PresenceFinder;
import mudmk3.domain.character.CharacterFixture;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Set;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(CharacterApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {CultivationFinder.class, CharacterRealmEditor.class, PresenceFinder.class})
record CharacterApiWebMvcTest(MockMvcTester mvc, CultivationFinder cultivationFinder,
                              CharacterRealmEditor characterRealmEditor, PresenceFinder presenceFinder) {

    @Test
    void list() {
        when(cultivationFinder.summaries()).thenReturn(List.of(
                new CharacterSummary(2L, "한립", Realm.QI_1, "목·화 진영근", 1),
                new CharacterSummary(1L, "한백", Realm.MORTAL, null, 0)));
        when(presenceFinder.onlineCharacterIds()).thenReturn(Set.of(2L));

        assertThat(mvc.get().with(admin()).uri("/api/admin/characters"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"id":2,"name":"한립","realm":"QI_1","realmName":"연기 1층","spiritRootName":"목·화 진영근",
                          "techniqueCount":1,"online":true},
                         {"id":1,"name":"한백","realm":"MORTAL","realmName":"범인","spiritRootName":null,
                          "techniqueCount":0,"online":false}]""");
    }

    @Test
    void find() {
        when(cultivationFinder.profile(2L)).thenReturn(profile(Realm.QI_1));

        var body = assertThat(mvc.get().with(admin()).uri("/api/admin/characters/2"))
                .hasStatusOk()
                .bodyJson();
        body.isStrictlyEqualTo("""
                {"id":2,"name":"한립","realm":"QI_1","realmName":"연기 1층",
                 "spiritRoot":{"name":"목·화 진영근","roots":[{"element":"WOOD","elementName":"목","aptitude":50.0},
                                                              {"element":"FIRE","elementName":"화","aptitude":50.0}]},
                 "stats":{"qiBlood":60,"mana":25,"sense":20,"essence":20,"spirit":5,"body":6,"strength":3},
                 "techniques":[{"id":1,"name":"장춘공","stage":1,"stageCount":9}]}""");
        body.extractingPath("$.spiritRoot.roots[0].aptitude").isEqualTo(50.0);
        body.extractingPath("$.stats.qiBlood").isEqualTo(60);
    }

    @Test
    void findWithoutSpiritRoot() {
        when(cultivationFinder.profile(1L)).thenReturn(new CultivationProfile(1L, "한백", Realm.MORTAL, null,
                StatSheet.of(Realm.MORTAL, null, List.of(), 0, 0), List.of()));

        var body = assertThat(mvc.get().with(admin()).uri("/api/admin/characters/1"))
                .hasStatusOk()
                .bodyJson();
        body.extractingPath("$.spiritRoot").isNull();
        body.extractingPath("$.realmName").isEqualTo("범인");
        body.extractingPath("$.techniques").asArray().isEmpty();
    }

    @Test
    void findUnknownFail() {
        when(cultivationFinder.profile(9L)).thenThrow(new IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: 9"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/characters/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("캐릭터를 찾을 수 없습니다. id: 9");
    }

    @Test
    void designateRealm() {
        when(cultivationFinder.profile(2L)).thenReturn(profile(Realm.QI_1));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/characters/2/realm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"realm":"QI_1"}"""))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.realmName").isEqualTo("연기 1층");

        // 지정한 뒤 다시 읽는다
        var order = inOrder(characterRealmEditor, cultivationFinder);
        order.verify(characterRealmEditor).designate(2L, Realm.QI_1);
        order.verify(cultivationFinder).profile(2L);
    }

    @Test
    void designateUnreadableBodyFail() {
        for (String json : List.of("""
                {"realm":null}""", """
                {}""", """
                {"realm":"QI_14"}""")) {
            assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/characters/2/realm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.detail").isEqualTo("요청 본문을 읽을 수 없습니다");
        }
        verifyNoInteractions(characterRealmEditor, cultivationFinder);
    }

    @Test
    void designateUnknownFail() {
        doThrow(new IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: 9"))
                .when(characterRealmEditor).designate(9L, Realm.QI_1);

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/characters/9/realm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"realm":"QI_1"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("캐릭터를 찾을 수 없습니다. id: 9");
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/characters")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().with(player()).uri("/api/admin/characters/2")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.put().with(player()).with(clientHeader()).uri("/api/admin/characters/2/realm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"realm":"QI_1"}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(characterRealmEditor);
    }

    @Test
    void designateWithoutClientHeaderFail() {
        assertThat(mvc.put().with(admin()).uri("/api/admin/characters/2/realm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"realm":"QI_1"}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(characterRealmEditor);
    }

    private static CultivationProfile profile(Realm realm) {
        var root = CharacterFixture.spiritRoot();
        return new CultivationProfile(2L, "한립", realm, root, StatSheet.of(realm, root, List.of(), 0, 0),
                List.of(new LearnedTechniqueView(1L, "장춘공", 1, 9)));
    }
}
