package mudmk3.application.item.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.item.BagGrade;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.ItemTypeFixture;
import mudmk3.domain.item.SpiritStoneGrade;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record ItemTypeRepositoryTest(ItemTypeRepository itemTypeRepository, EntityManager entityManager) {

    @Test
    void saveAndFind() {
        ItemType bag = itemTypeRepository.save(ItemTypeFixture.createBag(BagGrade.LOW));
        entityManager.flush();
        entityManager.clear();

        ItemType found = itemTypeRepository.findById(bag.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("하급 저물대");
        assertThat(found.getDescription()).isEqualTo(bag.getDescription());
        assertThat(found.getCategory()).isEqualTo(ItemCategory.BAG);
        assertThat(found.getBaseValue()).isEqualTo(10);
        assertThat(found.isTradable()).isTrue();
        assertThat(found.findBagGrade()).contains(BagGrade.LOW);
        assertThat(found.findSpiritStoneGrade()).isEmpty();
    }

    @Test
    void duplicateNameFail() {
        itemTypeRepository.save(ItemTypeFixture.createPill());

        assertThatThrownBy(() -> {
            itemTypeRepository.save(ItemTypeFixture.createPill());
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateSpiritStoneGradeFail() {
        itemTypeRepository.save(ItemType.spiritStone(SpiritStoneGrade.LOW));

        assertThatThrownBy(() -> {
            itemTypeRepository.save(ItemType.spiritStone(SpiritStoneGrade.LOW));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySpiritStoneGrade() {
        itemTypeRepository.save(ItemTypeFixture.createPill());
        ItemType stone = itemTypeRepository.save(ItemType.spiritStone(SpiritStoneGrade.MIDDLE));
        entityManager.flush();
        entityManager.clear();

        assertThat(itemTypeRepository.findBySpiritStoneGrade(SpiritStoneGrade.MIDDLE))
                .get().extracting(ItemType::getId).isEqualTo(stone.getId());
        assertThat(itemTypeRepository.findBySpiritStoneGrade(SpiritStoneGrade.LOW)).isEmpty();
    }

    @Test
    void existsByNameIgnoreCase() {
        ItemType slip = itemTypeRepository.save(ItemTypeFixture.createItemType("Jade Slip", ItemCategory.SCROLL, 1));
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());

        assertThat(itemTypeRepository.existsByNameIgnoreCase("jade slip")).isTrue();
        assertThat(itemTypeRepository.existsByNameIgnoreCase("jade")).isFalse();
        assertThat(itemTypeRepository.existsByNameIgnoreCaseAndIdNot("JADE SLIP", slip.getId())).isFalse();
        assertThat(itemTypeRepository.existsByNameIgnoreCaseAndIdNot("JADE SLIP", pill.getId())).isTrue();
    }

    @Test
    void findAllByIdIn() {
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());
        ItemType hide = itemTypeRepository.save(ItemTypeFixture.createWolfHide());
        itemTypeRepository.save(ItemTypeFixture.createBag(BagGrade.HIGH));

        assertThat(itemTypeRepository.findAllByIdIn(List.of(pill.getId(), hide.getId(), 999L)))
                .extracting(ItemType::getName)
                .containsExactlyInAnyOrder("회춘단(연기 초)", "늑대 가죽");
    }

    @Test
    void delete() {
        ItemType pill = itemTypeRepository.save(ItemTypeFixture.createPill());

        itemTypeRepository.delete(pill);
        entityManager.flush();

        assertThat(itemTypeRepository.findById(pill.getId())).isEmpty();
    }
}
