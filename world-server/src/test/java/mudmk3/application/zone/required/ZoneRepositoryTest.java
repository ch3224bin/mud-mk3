package mudmk3.application.zone.required;

import jakarta.persistence.EntityManager;
import mudmk3.application.faction.required.FactionRepository;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionFixture;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record ZoneRepositoryTest(ZoneRepository zoneRepository, FactionRepository factionRepository, EntityManager entityManager) {

    @Test
    void save() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone("청석숲"));
        entityManager.flush();
        entityManager.clear();

        Zone found = zoneRepository.findById(zone.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("청석숲");
        assertThat(found.getDescription()).isEqualTo(zone.getDescription());
    }

    @Test
    void duplicateNameFail() {
        zoneRepository.save(ZoneFixture.createZone("청석숲"));

        // IDENTITY 전략이라 save 때 바로 insert 된다
        assertThatThrownBy(() -> {
            zoneRepository.save(ZoneFixture.createZone("청석숲"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByOrderByNameAsc() {
        zoneRepository.save(ZoneFixture.createZone("황풍곡"));
        zoneRepository.save(ZoneFixture.createZone("가람골"));
        zoneRepository.save(ZoneFixture.createZone("청석숲"));

        assertThat(zoneRepository.findAllByOrderByNameAsc())
                .extracting(Zone::getName)
                .containsExactly("가람골", "청석숲", "황풍곡");
    }

    @Test
    void existsByName() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone("청석숲"));
        Zone other = zoneRepository.save(ZoneFixture.createZone("황풍곡"));

        assertThat(zoneRepository.existsByName("청석숲")).isTrue();
        assertThat(zoneRepository.existsByName("가람골")).isFalse();
        assertThat(zoneRepository.existsByNameAndIdNot("청석숲", zone.getId())).isFalse();
        assertThat(zoneRepository.existsByNameAndIdNot("청석숲", other.getId())).isTrue();
    }

    @Test
    void delete() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone("청석숲"));

        zoneRepository.delete(zone);
        entityManager.flush();

        assertThat(zoneRepository.findById(zone.getId())).isEmpty();
    }

    @Test
    void ownerPersisted() {
        Faction owner = factionRepository.save(FactionFixture.createFaction());
        Zone zone = zoneRepository.save(Zone.create(ZoneFixture.createZoneSaveInfo("청석숲"), owner));
        entityManager.flush();
        entityManager.clear();

        Zone found = zoneRepository.findById(zone.getId()).orElseThrow();
        assertThat(found.findOwner()).map(Faction::getName).contains("청석성");
    }
}
