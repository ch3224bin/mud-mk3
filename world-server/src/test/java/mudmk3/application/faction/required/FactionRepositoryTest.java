package mudmk3.application.faction.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionFixture;
import mudmk3.domain.faction.FactionKind;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record FactionRepositoryTest(FactionRepository factionRepository, EntityManager entityManager) {

    @Test
    void save() {
        Faction faction = factionRepository.save(FactionFixture.createFaction(FactionKind.SECT, "황엽곡"));
        entityManager.flush();
        entityManager.clear();

        Faction found = factionRepository.findById(faction.getId()).orElseThrow();
        assertThat(found.getKind()).isEqualTo(FactionKind.SECT);
        assertThat(found.getName()).isEqualTo("황엽곡");
        assertThat(found.getDescription()).isEqualTo(faction.getDescription());
    }

    @Test
    void duplicateNameFail() {
        factionRepository.save(FactionFixture.createFaction(FactionKind.CASTLE, "청석성"));

        // 성과 종문을 통틀어 이름이 하나뿐이다
        assertThatThrownBy(() -> {
            factionRepository.save(FactionFixture.createFaction(FactionKind.SECT, "청석성"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByName() {
        Faction faction = factionRepository.save(FactionFixture.createFaction(FactionKind.CASTLE, "청석성"));
        Faction other = factionRepository.save(FactionFixture.createFaction(FactionKind.SECT, "황엽곡"));

        assertThat(factionRepository.existsByName("청석성")).isTrue();
        assertThat(factionRepository.existsByName("남원성")).isFalse();
        assertThat(factionRepository.existsByNameAndIdNot("청석성", faction.getId())).isFalse();
        assertThat(factionRepository.existsByNameAndIdNot("청석성", other.getId())).isTrue();
    }

    @Test
    void countZones() {
        Faction faction = factionRepository.save(FactionFixture.createFaction(FactionKind.CASTLE, "청석성"));
        Faction other = factionRepository.save(FactionFixture.createFaction(FactionKind.SECT, "황엽곡"));
        entityManager.persist(Zone.create(ZoneFixture.createZoneSaveInfo("청석숲"), faction));
        entityManager.persist(Zone.create(ZoneFixture.createZoneSaveInfo("청석성 안"), faction));
        entityManager.persist(Zone.create(ZoneFixture.createZoneSaveInfo("가람골"), null));

        assertThat(factionRepository.countZones(faction.getId())).isEqualTo(2);
        assertThat(factionRepository.countZones(other.getId())).isZero();
    }

    @Test
    void delete() {
        Faction faction = factionRepository.save(FactionFixture.createFaction());

        factionRepository.delete(faction);
        entityManager.flush();

        assertThat(factionRepository.findById(faction.getId())).isEmpty();
    }
}
