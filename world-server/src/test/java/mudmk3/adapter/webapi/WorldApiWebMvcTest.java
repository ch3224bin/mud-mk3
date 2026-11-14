package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.world.provided.StartingItemRequest;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.room.Room;
import mudmk3.domain.world.StartingItem;
import mudmk3.domain.world.WorldSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.room;
import static mudmk3.adapter.webapi.ZoneApiWebMvcTest.zone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(WorldApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {WorldSettingsFinder.class, WorldSettingsEditor.class, ItemTypeFinder.class})
record WorldApiWebMvcTest(MockMvcTester mvc, WorldSettingsFinder worldSettingsFinder,
                          WorldSettingsEditor worldSettingsEditor, ItemTypeFinder itemTypeFinder) {

    @Test
    void getWithoutStartRoom() {
        when(worldSettingsFinder.findStartRoom()).thenReturn(Optional.empty());

        assertThat(mvc.get().with(admin()).uri("/api/admin/world"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"startRoomId":null,"startRoomName":null,"startZoneName":null}""");
    }

    @Test
    void get() {
        when(worldSettingsFinder.findStartRoom()).thenReturn(Optional.of(room(zone(1L, "청석숲"), 10L, "샘터")));

        assertThat(mvc.get().with(admin()).uri("/api/admin/world"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"startRoomId":10,"startRoomName":"샘터","startZoneName":"청석숲"}""");
    }

    @Test
    void designateStartRoom() {
        Room room = room(zone(1L, "청석숲"), 10L, "샘터");
        WorldSettings settings = WorldSettings.create();
        settings.designateStartRoom(room);
        when(worldSettingsEditor.designateStartRoom(10L)).thenReturn(settings);

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/world/start-room")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"roomId":10}"""))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"startRoomId":10,"startRoomName":"샘터","startZoneName":"청석숲"}""");
    }

    @Test
    void designateStartRoomWithoutIdFail() {
        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/world/start-room")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(worldSettingsEditor);
    }

    @Test
    void unauthenticatedFail() {
        assertThat(mvc.get().uri("/api/admin/world")).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(worldSettingsFinder);
    }

    @Test
    void designateStartRoomWithoutClientHeaderFail() {
        assertThat(mvc.put().with(admin()).uri("/api/admin/world/start-room")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"roomId":10}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(worldSettingsEditor);
    }

    @Test
    void getStartingItems() {
        when(worldSettingsFinder.findStartingItems()).thenReturn(List.of(new StartingItem(5L, 3), new StartingItem(1L, 30)));
        when(itemTypeFinder.findAllById(List.of(5L, 1L))).thenReturn(Map.of(1L, lowStone(1L), 5L, pill(5L)));

        assertThat(mvc.get().with(admin()).uri("/api/admin/world/starting-items"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"itemTypeId":5,"itemName":"회춘단(연기 초)","count":3},
                         {"itemTypeId":1,"itemName":"하품 영석","count":30}]""");
    }

    @Test
    void getStartingItemsEmpty() {
        when(worldSettingsFinder.findStartingItems()).thenReturn(List.of());
        when(itemTypeFinder.findAllById(List.of())).thenReturn(Map.of());

        assertThat(mvc.get().with(admin()).uri("/api/admin/world/starting-items"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("[]");
    }

    @Test
    void replaceStartingItems() {
        when(worldSettingsEditor.replaceStartingItems(List.of(new StartingItemRequest(1L, 30), new StartingItemRequest(5L, 3))))
                .thenReturn(List.of(new StartingItem(1L, 30), new StartingItem(5L, 3)));
        when(itemTypeFinder.findAllById(List.of(1L, 5L))).thenReturn(Map.of(1L, lowStone(1L), 5L, pill(5L)));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/world/starting-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items":[{"itemTypeId":1,"count":30},{"itemTypeId":5,"count":3}]}"""))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [{"itemTypeId":1,"itemName":"하품 영석","count":30},
                         {"itemTypeId":5,"itemName":"회춘단(연기 초)","count":3}]""");
    }

    @Test
    void replaceStartingItemsZeroCountFail() {
        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/world/starting-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items":[{"itemTypeId":1,"count":0}]}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(worldSettingsEditor);
    }

    @Test
    void replaceStartingItemsWithoutItemsFail() {
        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/world/starting-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(worldSettingsEditor);
    }

    @Test
    void replaceStartingItemsRuleFail() {
        when(worldSettingsEditor.replaceStartingItems(any()))
                .thenThrow(new IllegalArgumentException("시작 소지품이 칸을 넘습니다. (필요 25칸, 칸 22)"));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/world/starting-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items":[{"itemTypeId":1,"count":30}]}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("시작 소지품이 칸을 넘습니다. (필요 25칸, 칸 22)");
    }

    @Test
    void replaceStartingItemsWithoutClientHeaderFail() {
        assertThat(mvc.put().with(admin()).uri("/api/admin/world/starting-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"items":[]}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(worldSettingsEditor);
    }

    private static ItemType lowStone(Long id) {
        return withId(ItemType.spiritStone(SpiritStoneGrade.LOW), id);
    }

    private static ItemType pill(Long id) {
        return withId(ItemTypeFixture.createItemType("회춘단(연기 초)", ItemCategory.PILL, 3), id);
    }

    private static ItemType withId(ItemType itemType, Long id) {
        ReflectionTestUtils.setField(itemType, "id", id);
        return itemType;
    }
}
