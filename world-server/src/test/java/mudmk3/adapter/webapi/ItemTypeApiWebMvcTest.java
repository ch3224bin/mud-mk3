package mudmk3.adapter.webapi;

import mudmk3.adapter.security.WebMvcSecurityTestConfiguration;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.provided.ItemTypeInUseException;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.item.provided.SpiritStoneChangeException;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.DuplicateItemTypeNameException;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static mudmk3.adapter.security.SecurityFixture.admin;
import static mudmk3.adapter.security.SecurityFixture.clientHeader;
import static mudmk3.adapter.security.SecurityFixture.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(ItemTypeApi.class)
@Import(WebMvcSecurityTestConfiguration.class)
@MockitoBean(types = {ItemTypeFinder.class, ItemTypeEditor.class})
record ItemTypeApiWebMvcTest(MockMvcTester mvc, ItemTypeFinder itemTypeFinder, ItemTypeEditor itemTypeEditor) {

    @Test
    void list() {
        ItemType stone = withId(ItemType.spiritStone(SpiritStoneGrade.LOW), 1L);
        ItemType hide = withId(ItemTypeFixture.createWolfHide(), 5L);
        when(itemTypeFinder.findAll()).thenReturn(List.of(stone, hide));

        assertThat(mvc.get().with(admin()).uri("/api/admin/item-types"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        [{"id":1,"name":"하품 영석","category":"SPIRIT_STONE","categoryName":"영석","stackSize":10000,
                          "baseValue":1,"spiritStone":true,"buyRatePercent":0,"bagGrade":null,"bagSlots":0},
                         {"id":5,"name":"늑대 가죽","category":"MATERIAL","categoryName":"재료","stackSize":99,
                          "spiritStone":false,"buyRatePercent":40}]""");
    }

    @Test
    void create() {
        when(itemTypeEditor.create(any())).thenReturn(withId(ItemTypeFixture.createPill(), 7L));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"회춘단(연기 초)","description":"회춘단(연기 초)의 묘사다.","category":"PILL",
                         "baseValue":3,"bagGrade":null}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"id":7,"name":"회춘단(연기 초)","description":"회춘단(연기 초)의 묘사다.","category":"PILL",
                         "categoryName":"단약","stackSize":99,"baseValue":3,"bagGrade":null,
                         "bagGradeName":null,"bagSlots":0,"spiritStone":false,"buyRatePercent":25}""");
        verify(itemTypeEditor).create(new ItemTypeSaveRequest(
                "회춘단(연기 초)", "회춘단(연기 초)의 묘사다.", ItemCategory.PILL, 3, null));
    }

    @Test
    void createBag() {
        when(itemTypeEditor.create(any())).thenReturn(withId(ItemTypeFixture.createBag(BagGrade.LOW), 8L));

        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"하급 저물대","category":"BAG","baseValue":10,"bagGrade":"LOW"}"""))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":8,"category":"BAG","categoryName":"저물대","stackSize":0,"bagGrade":"LOW",
                         "bagGradeName":"하급","bagSlots":20}""");
    }

    @Test
    void find() {
        when(itemTypeFinder.find(5L)).thenReturn(withId(ItemTypeFixture.createWolfHide(), 5L));

        assertThat(mvc.get().with(admin()).uri("/api/admin/item-types/5"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":5,"name":"늑대 가죽","baseValue":2}""");
    }

    @Test
    void findUnknownFail() {
        when(itemTypeFinder.find(9L)).thenThrow(new IllegalArgumentException("물건 종류를 찾을 수 없습니다. id: 9"));

        assertThat(mvc.get().with(admin()).uri("/api/admin/item-types/9"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("물건 종류를 찾을 수 없습니다. id: 9");
    }

    @Test
    void createBlankNameFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"   ","category":"PILL","baseValue":3}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: name");
        verify(itemTypeEditor, never()).create(any());
    }

    @Test
    void createUnknownCategoryFail() {
        assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"회춘단","category":"POTION","baseValue":3}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verify(itemTypeEditor, never()).create(any());
    }

    @Test
    void createDuplicateNameFail() {
        when(itemTypeEditor.create(any())).thenThrow(new DuplicateItemTypeNameException("이미 있는 물건 이름입니다: 회춘단"));

        var result = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"회춘단","category":"PILL","baseValue":3}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("이미 있는 물건 이름입니다: 회춘단");
        result.extractingPath("$.exception").isEqualTo("DuplicateItemTypeNameException");
    }

    @Test
    void createConcurrentChangeFail() {
        when(itemTypeEditor.create(any())).thenThrow(new DataIntegrityViolationException("uk_item_type_name"));

        var result = assertThat(mvc.post().with(admin()).with(clientHeader()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"회춘단","category":"PILL","baseValue":3}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("다른 곳에서 함께 바꾸고 있어 저장하지 못했습니다. 다시 시도하십시오.");
        result.extractingPath("$.exception").isEqualTo("DataIntegrityViolationException");
    }

    @Test
    void update() {
        when(itemTypeEditor.update(any(), any())).thenReturn(withId(ItemTypeFixture.createWolfHide(), 5L));

        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/item-types/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"늑대 가죽","description":"늑대 가죽의 묘사다.","category":"MATERIAL",
                         "baseValue":2}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"id":5,"name":"늑대 가죽","categoryName":"재료"}""");
        verify(itemTypeEditor).update(5L, new ItemTypeSaveRequest(
                "늑대 가죽", "늑대 가죽의 묘사다.", ItemCategory.MATERIAL, 2, null));
    }

    @Test
    void updateSpiritStoneFail() {
        when(itemTypeEditor.update(eq(1L), any()))
                .thenThrow(new SpiritStoneChangeException("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다."));

        var result = assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/item-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"하품 영석2","category":"PILL","baseValue":1}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.");
        result.extractingPath("$.exception").isEqualTo("SpiritStoneChangeException");
    }

    /** 리드 판정: 영석이라도 형식이 틀린 요청은 {@code @Valid} 가 먼저 막아 400이다 */
    @Test
    void updateSpiritStoneInvalidRequestFail() {
        assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/item-types/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","category":"PILL","baseValue":1}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("입력 값이 올바르지 않습니다: name");
        verify(itemTypeEditor, never()).update(any(), any());
    }

    @Test
    void updateInUseFail() {
        when(itemTypeEditor.update(eq(5L), any()))
                .thenThrow(new ItemTypeInUseException("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 늑대 가죽"));

        var result = assertThat(mvc.put().with(admin()).with(clientHeader()).uri("/api/admin/item-types/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"늑대 가죽","category":"PILL","baseValue":2}"""))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 늑대 가죽");
        result.extractingPath("$.exception").isEqualTo("ItemTypeInUseException");
    }

    @Test
    void delete() {
        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/5"))
                .hasStatus(HttpStatus.NO_CONTENT);
        verify(itemTypeEditor).delete(5L);
    }

    @Test
    void deleteInUseFail() {
        doThrow(new ItemTypeInUseException("쓰이는 물건은 지울 수 없습니다: 회춘단")).when(itemTypeEditor).delete(5L);

        var result = assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/5"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson();
        result.extractingPath("$.detail").isEqualTo("쓰이는 물건은 지울 수 없습니다: 회춘단");
        result.extractingPath("$.exception").isEqualTo("ItemTypeInUseException");
    }

    @Test
    void deleteSpiritStoneFail() {
        doThrow(new SpiritStoneChangeException("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다."))
                .when(itemTypeEditor).delete(1L);

        assertThat(mvc.delete().with(admin()).with(clientHeader()).uri("/api/admin/item-types/1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.");
    }

    @Test
    void playerFail() {
        assertThat(mvc.get().with(player()).uri("/api/admin/item-types")).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(itemTypeFinder);
    }

    @Test
    void createWithoutClientHeaderFail() {
        assertThat(mvc.post().with(admin()).uri("/api/admin/item-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"회춘단","category":"PILL","baseValue":3}"""))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(itemTypeEditor);
    }

    static ItemType withId(ItemType itemType, Long id) {
        ReflectionTestUtils.setField(itemType, "id", id);
        return itemType;
    }
}
