package mudmk3.application.zone.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.RoomSaveRequest;
import mudmk3.domain.zone.DuplicateZoneNameException;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record ZoneEditorTest(ZoneEditor zoneEditor, ZoneFinder zoneFinder, RoomEditor roomEditor, EntityManager entityManager) {

    @Test
    void create() {
        Zone zone = zoneEditor.create(new ZoneSaveRequest(" 청석숲 ", "푸른 돌이 흩어진 숲이다."));
        entityManager.flush();
        entityManager.clear();

        Zone found = zoneFinder.find(zone.getId());
        assertThat(found.getName()).isEqualTo("청석숲");
        assertThat(found.getDescription()).isEqualTo("푸른 돌이 흩어진 숲이다.");
    }

    @Test
    void createNameSizeCheckedAfterStrip() {
        String thirty = "가".repeat(30);

        Zone zone = zoneEditor.create(new ZoneSaveRequest("  " + thirty + "  ", null));

        assertThat(zone.getName()).isEqualTo(thirty);
        assertThatThrownBy(() -> zoneEditor.create(new ZoneSaveRequest("나".repeat(31), null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> zoneEditor.create(new ZoneSaveRequest("   ", null)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void duplicateNameFail() {
        zoneEditor.create(createZoneSaveRequest("청석숲"));

        assertThatThrownBy(() -> zoneEditor.create(createZoneSaveRequest(" 청석숲  ")))
                .isInstanceOf(DuplicateZoneNameException.class)
                .hasMessage("이미 있는 지역 이름입니다: 청석숲");
    }

    @Test
    void update() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));

        zoneEditor.update(zone.getId(), new ZoneSaveRequest("청석숲", "바뀐 묘사다."));
        zoneEditor.update(zone.getId(), new ZoneSaveRequest("황풍곡", "바람이 거센 골짜기다."));
        entityManager.flush();
        entityManager.clear();

        Zone found = zoneFinder.find(zone.getId());
        assertThat(found.getName()).isEqualTo("황풍곡");
        assertThat(found.getDescription()).isEqualTo("바람이 거센 골짜기다.");
    }

    @Test
    void updateToOtherZoneNameFail() {
        zoneEditor.create(createZoneSaveRequest("청석숲"));
        Zone other = zoneEditor.create(createZoneSaveRequest("황풍곡"));

        assertThatThrownBy(() -> zoneEditor.update(other.getId(), createZoneSaveRequest(" 청석숲")))
                .isInstanceOf(DuplicateZoneNameException.class)
                .hasMessage("이미 있는 지역 이름입니다: 청석숲");
    }

    @Test
    void findAllByName() {
        zoneEditor.create(createZoneSaveRequest("황풍곡"));
        zoneEditor.create(createZoneSaveRequest("가람골"));
        zoneEditor.create(createZoneSaveRequest("청석숲"));

        assertThat(zoneFinder.findAll()).extracting(Zone::getName)
                .containsExactly("가람골", "청석숲", "황풍곡");
    }

    @Test
    void countRooms() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Zone other = zoneEditor.create(createZoneSaveRequest("황풍곡"));
        roomEditor.create(new RoomSaveRequest(zone.getId(), "샘터", "맑은 샘이 솟는다."));
        roomEditor.create(new RoomSaveRequest(zone.getId(), "오솔길", "좁은 길이다."));

        assertThat(zoneFinder.countRooms(zone.getId())).isEqualTo(2);
        assertThat(zoneFinder.countRooms(other.getId())).isZero();
    }

    @Test
    void deleteNotEmptyFail() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        roomEditor.create(new RoomSaveRequest(zone.getId(), "샘터", "맑은 샘이 솟는다."));

        assertThatThrownBy(() -> zoneEditor.delete(zone.getId()))
                .isInstanceOf(ZoneNotEmptyException.class)
                .hasMessage("방이 남아 있는 지역은 지울 수 없습니다");
    }

    @Test
    void delete() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));

        zoneEditor.delete(zone.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> zoneFinder.find(zone.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
