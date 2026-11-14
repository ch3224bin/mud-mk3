package mudmk3.application.faction.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.domain.faction.DuplicateFactionNameException;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionKind;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.faction.FactionFixture.createFactionSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record FactionEditorTest(FactionEditor factionEditor, FactionFinder factionFinder, ZoneEditor zoneEditor,
                         EntityManager entityManager) {

    @Test
    void create() {
        Faction faction = factionEditor.create(new FactionSaveRequest(FactionKind.CASTLE, " 청석성 ", "돌로 쌓은 성이다."));
        entityManager.flush();
        entityManager.clear();

        Faction found = factionFinder.find(faction.getId());
        assertThat(found.getKind()).isEqualTo(FactionKind.CASTLE);
        assertThat(found.getName()).isEqualTo("청석성");
        assertThat(found.getDescription()).isEqualTo("돌로 쌓은 성이다.");
    }

    @Test
    void createInvalidFail() {
        assertThatThrownBy(() -> factionEditor.create(new FactionSaveRequest(null, "청석성", null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> factionEditor.create(new FactionSaveRequest(FactionKind.CASTLE, "   ", null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> factionEditor.create(new FactionSaveRequest(FactionKind.CASTLE, "가".repeat(31), null)))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> factionEditor.create(new FactionSaveRequest(FactionKind.CASTLE, "청석성", "가".repeat(501))))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void duplicateNameFail() {
        factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));

        assertThatThrownBy(() -> factionEditor.create(createFactionSaveRequest(FactionKind.SECT, " 청석성  ")))
                .isInstanceOf(DuplicateFactionNameException.class)
                .hasMessage("이미 있는 성·종문 이름입니다: 청석성");
    }

    @Test
    void update() {
        Faction faction = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));

        factionEditor.update(faction.getId(), new FactionSaveRequest(FactionKind.SECT, "황엽곡", null));
        entityManager.flush();
        entityManager.clear();

        Faction found = factionFinder.find(faction.getId());
        assertThat(found.getKind()).isEqualTo(FactionKind.SECT);
        assertThat(found.getName()).isEqualTo("황엽곡");
        assertThat(found.getDescription()).isEmpty();
    }

    @Test
    void updateToOtherNameFail() {
        factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));
        Faction other = factionEditor.create(createFactionSaveRequest(FactionKind.SECT, "황엽곡"));

        assertThatThrownBy(() -> factionEditor.update(other.getId(), createFactionSaveRequest(FactionKind.SECT, " 청석성")))
                .isInstanceOf(DuplicateFactionNameException.class)
                .hasMessage("이미 있는 성·종문 이름입니다: 청석성");
    }

    @Test
    void updateKeepsOwnName() {
        Faction faction = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));

        Faction updated = factionEditor.update(faction.getId(), new FactionSaveRequest(FactionKind.CASTLE, "청석성", "바뀐 묘사다."));

        assertThat(updated.getDescription()).isEqualTo("바뀐 묘사다.");
    }

    @Test
    void unknownFactionFail() {
        assertThatThrownBy(() -> factionFinder.find(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("성·종문을 찾을 수 없습니다. id: 999");
    }

    @Test
    void findAllByKindThenName() {
        factionEditor.create(createFactionSaveRequest(FactionKind.SECT, "황엽곡"));
        factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));
        factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "남원성"));

        assertThat(factionFinder.findAll()).extracting(Faction::getName)
                .containsExactly("남원성", "청석성", "황엽곡");
    }

    @Test
    void countZones() {
        Faction faction = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));
        zoneEditor.create(new ZoneSaveRequest("청석숲", null, faction.getId()));

        assertThat(factionFinder.countZones(faction.getId())).isEqualTo(1);
    }

    @Test
    void delete() {
        Faction faction = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));

        factionEditor.delete(faction.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> factionFinder.find(faction.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteOwningFactionFail() {
        Faction faction = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));
        zoneEditor.create(new ZoneSaveRequest("청석숲", null, faction.getId()));

        assertThatThrownBy(() -> factionEditor.delete(faction.getId()))
                .isInstanceOf(FactionInUseException.class)
                .hasMessage("소유한 지역이 있는 성·종문은 지울 수 없습니다");
        entityManager.flush();
        entityManager.clear();
        assertThat(factionFinder.find(faction.getId()).getName()).isEqualTo("청석성");
    }

    @Test
    void deleteAfterOwnerRemoved() {
        Faction faction = factionEditor.create(createFactionSaveRequest(FactionKind.CASTLE, "청석성"));
        Zone zone = zoneEditor.create(new ZoneSaveRequest("청석숲", null, faction.getId()));
        zoneEditor.update(zone.getId(), new ZoneSaveRequest("청석숲", null, null));
        entityManager.flush();

        factionEditor.delete(faction.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(factionFinder.findAll()).isEmpty();
    }
}
