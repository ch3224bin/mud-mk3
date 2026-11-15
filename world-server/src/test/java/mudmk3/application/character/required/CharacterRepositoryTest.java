package mudmk3.application.character.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.character.SpiritRootEntry;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
record CharacterRepositoryTest(CharacterRepository characterRepository, EntityManager entityManager) {

    @Test
    void save() {
        PlayerCharacter character = characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();

        PlayerCharacter found = characterRepository.findById(character.getId()).orElseThrow();
        assertThat(found.getAccountId()).isEqualTo(1L);
        assertThat(found.getName()).isEqualTo(new CharacterName("한백"));
        assertThat(found.getRoomId()).isEqualTo(10L);
    }

    @Test
    void saveWithSpiritRoot() {
        PlayerCharacter character = characterRepository.save(
                createCharacter(1L, "한백", 10L, SpiritRoot.parse("화 금 목").orElseThrow()));
        flushAndClear();

        PlayerCharacter found = characterRepository.findById(character.getId()).orElseThrow();
        assertThat(found.getRealm()).isEqualTo(Realm.MORTAL);
        SpiritRoot root = found.findSpiritRoot().orElseThrow();
        assertThat(root.entries())
                .extracting(SpiritRootEntry::element, SpiritRootEntry::aptitude)
                .containsExactly(
                        tuple(Element.METAL, 100.0 / 3),
                        tuple(Element.WOOD, 100.0 / 3),
                        tuple(Element.FIRE, 100.0 / 3));
        assertThat(root.name()).isEqualTo("금·목·화 진영근");
    }

    @Test
    void saveWithoutSpiritRoot() {
        PlayerCharacter character = characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();

        assertThat(characterRepository.findById(character.getId()).orElseThrow().findSpiritRoot()).isEmpty();
    }

    @Test
    void fillMissingRealms() {
        PlayerCharacter legacy = characterRepository.save(createCharacter(1L, "한백", 10L));
        PlayerCharacter founded = createCharacter(2L, "한립", 10L);
        founded.changeRealm(Realm.FOUNDATION_EARLY);
        characterRepository.save(founded);
        PlayerCharacter mortal = characterRepository.save(createCharacter(3L, "남궁완", 10L));
        entityManager.flush();
        entityManager.createNativeQuery("update player_character set realm = null where id = ?")
                .setParameter(1, legacy.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(characterRepository.fillMissingRealms(Realm.QI_1)).isEqualTo(1);
        entityManager.clear();

        assertThat(characterRepository.findById(legacy.getId()).orElseThrow().getRealm()).isEqualTo(Realm.QI_1);
        assertThat(characterRepository.findById(founded.getId()).orElseThrow().getRealm()).isEqualTo(Realm.FOUNDATION_EARLY);
        assertThat(characterRepository.findById(mortal.getId()).orElseThrow().getRealm()).isEqualTo(Realm.MORTAL);
        assertThat(characterRepository.fillMissingRealms(Realm.QI_1)).isZero();
    }

    @Test
    void missingRealmFailsOnRead() {
        PlayerCharacter legacy = characterRepository.save(createCharacter(1L, "한백", 10L));
        entityManager.flush();
        entityManager.createNativeQuery("update player_character set realm = null where id = ?")
                .setParameter(1, legacy.getId())
                .executeUpdate();
        entityManager.clear();

        PlayerCharacter found = characterRepository.findById(legacy.getId()).orElseThrow();
        assertThatThrownBy(found::getRealm).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findByAccountId() {
        characterRepository.save(createCharacter(1L, "한백", 10L));
        characterRepository.save(createCharacter(2L, "한립", 10L));
        flushAndClear();

        assertThat(characterRepository.findByAccountId(2L))
                .map(character -> character.getName().value())
                .contains("한립");
        assertThat(characterRepository.findByAccountId(3L)).isEmpty();
    }

    @Test
    void existsByName() {
        characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();

        assertThat(characterRepository.existsByName(new CharacterName("한백"))).isTrue();
        assertThat(characterRepository.existsByName(new CharacterName("한립"))).isFalse();
    }

    @Test
    void findAllByRoomId() {
        characterRepository.save(createCharacter(1L, "한백", 10L));
        characterRepository.save(createCharacter(2L, "한립", 20L));
        characterRepository.save(createCharacter(3L, "남궁완", 10L));
        flushAndClear();

        assertThat(characterRepository.findAllByRoomId(10L))
                .extracting(character -> character.getName().value())
                .containsExactlyInAnyOrder("한백", "남궁완");
        assertThat(characterRepository.findAllByRoomId(30L)).isEmpty();
    }

    @Test
    void duplicateAccountIdFail() {
        characterRepository.save(createCharacter(1L, "한백", 10L));

        assertThatThrownBy(() -> {
            characterRepository.save(createCharacter(1L, "한립", 10L));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateNameFail() {
        characterRepository.save(createCharacter(1L, "한백", 10L));

        assertThatThrownBy(() -> {
            characterRepository.save(createCharacter(2L, "한백", 10L));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void legacyNullIsFull() {
        PlayerCharacter character = characterRepository.save(createCharacter(1L, "한백", 10L));
        entityManager.flush();
        entityManager.createNativeQuery("update player_character set qi_blood_lost = null").executeUpdate();
        entityManager.clear();

        assertThat(characterRepository.readQiBloodLost(character.getId())).contains(0.0);
        assertThat(characterRepository.findById(character.getId()).orElseThrow().getQiBloodLostAtLoad()).isNull();
    }

    @Test
    void qiBloodLostRoundTrip() {
        PlayerCharacter character = characterRepository.save(createCharacter(1L, "한백", 10L));

        assertThat(characterRepository.writeQiBloodLost(character.getId(), 12.5)).isEqualTo(1);
        flushAndClear();

        assertThat(characterRepository.readQiBloodLost(character.getId())).contains(12.5);
        assertThat(characterRepository.findById(character.getId()).orElseThrow().getQiBloodLostAtLoad()).isEqualTo(12.5);
        assertThat(characterRepository.readQiBloodLost(999L)).isEmpty();
        assertThat(characterRepository.writeQiBloodLost(999L, 1)).isZero();
    }

    @Test
    void entitySaveDoesNotWriteQiBloodLost() {
        PlayerCharacter saved = characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();
        PlayerCharacter character = characterRepository.findById(saved.getId()).orElseThrow();

        characterRepository.writeQiBloodLost(character.getId(), 30);
        character.moveTo(20L);
        characterRepository.save(character);
        flushAndClear();

        assertThat(characterRepository.findById(saved.getId()).orElseThrow().getRoomId()).isEqualTo(20L);
        assertThat(characterRepository.readQiBloodLost(saved.getId())).contains(30.0);
    }

    @Test
    void legacyNullIsFullMana() {
        PlayerCharacter character = characterRepository.save(createCharacter(1L, "한백", 10L));
        entityManager.flush();
        entityManager.createNativeQuery("update player_character set mana_lost = null").executeUpdate();
        entityManager.clear();

        assertThat(characterRepository.readManaLost(character.getId())).contains(0.0);
        assertThat(characterRepository.readManaLost(999L)).isEmpty();
        assertThat(characterRepository.writeManaLost(999L, 1)).isZero();
    }

    @Test
    void entitySaveDoesNotWriteManaLost() {
        PlayerCharacter saved = characterRepository.save(createCharacter(1L, "한백", 10L));
        flushAndClear();
        PlayerCharacter character = characterRepository.findById(saved.getId()).orElseThrow();

        assertThat(characterRepository.writeManaLost(character.getId(), 5.5)).isEqualTo(1);
        character.moveTo(20L);
        characterRepository.save(character);
        flushAndClear();

        assertThat(characterRepository.findById(saved.getId()).orElseThrow().getRoomId()).isEqualTo(20L);
        assertThat(characterRepository.readManaLost(saved.getId())).contains(5.5);
        assertThat(characterRepository.readQiBloodLost(saved.getId())).contains(0.0);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
