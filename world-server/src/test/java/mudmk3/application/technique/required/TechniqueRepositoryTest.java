package mudmk3.application.technique.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Ability;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilitySaveInfo;
import mudmk3.domain.technique.AbilityStagePower;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.domain.technique.UsableWhen;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static mudmk3.domain.technique.TechniqueFixture.ability;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.windRiding;
import static mudmk3.domain.technique.TechniqueFixture.withEffectLines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record TechniqueRepositoryTest(TechniqueRepository techniqueRepository, EntityManager entityManager) {

    @Test
    void saveWithStagesAndAbilities() {
        Technique technique = techniqueRepository.save(Technique.create(new TechniqueSaveInfo("화토공", "묘사",
                List.of(Element.EARTH, Element.FIRE), Realm.QI_3, TechniqueFixture.changchunStages(),
                List.of(windRiding(), mixed(), fireBall()))));
        entityManager.flush();
        entityManager.clear();

        Technique found = techniqueRepository.findById(technique.getId()).orElseThrow();

        // 속성은 함께 읽고 단계·신통·신통 속성은 트랜잭션 안에서 지연 로딩한다
        assertThat(found.getName()).isEqualTo("화토공");
        assertThat(found.getDescription()).isEqualTo("묘사");
        assertThat(found.getElements()).containsExactly(Element.FIRE, Element.EARTH);
        assertThat(found.getStartRealm()).isEqualTo(Realm.QI_3);
        assertThat(found.getStages()).containsExactlyElementsOf(TechniqueFixture.changchunStages());
        assertThat(found.getAbilities()).extracting(Ability::getName).containsExactly("어풍결", "토화술", "화탄술");
        Ability mixed = found.getAbilities().get(1);
        assertThat(mixed.getElements()).containsExactly(Element.FIRE, Element.EARTH);
        assertThat(mixed.getKind()).isEqualTo(AbilityKind.AREA_ATTACK);
        assertThat(mixed.getTarget()).isEqualTo(AbilityTarget.ALL_ENEMIES);
        assertThat(mixed.getCostStat()).isEqualTo(CostStat.SENSE);
        assertThat(mixed.getCostAmount()).isEqualByComparingTo("1.5");
        assertThat(mixed.getCastSeconds()).isEqualByComparingTo("2.5");
        assertThat(mixed.getCooldownSeconds()).isEqualByComparingTo("3600");
        assertThat(mixed.getUsableWhen()).isEqualTo(UsableWhen.ANYTIME);
    }

    @Test
    void replaceAbilities() {
        Technique technique = techniqueRepository.save(TechniqueFixture.changchun());
        entityManager.flush();
        entityManager.clear();

        Technique loaded = techniqueRepository.findById(technique.getId()).orElseThrow();
        loaded.update(new TechniqueSaveInfo("장춘공", "", List.of(Element.WOOD), Realm.MORTAL,
                List.of(new TechniqueStage(0, 0, 0, 0, 0, 0, 0, 0, 0), new TechniqueStage(120, 0, 2, 1, 0, 0, 0, 0, 1)),
                List.of(windRidingAt(2), withEffectLines(TechniqueFixture.upToStage(fireBall(), 2),
                        List.of("새 불꽃이 튄다!")))));
        entityManager.flush();
        entityManager.clear();

        Technique found = techniqueRepository.findById(technique.getId()).orElseThrow();
        assertThat(found.stageCount()).isEqualTo(2);
        assertThat(found.getAbilities()).extracting(Ability::getName).containsExactly("어풍결", "화탄술");
        assertThat(found.getAbilities()).extracting(Ability::getLearnStage).containsExactly(2, 1);
        assertThat(count("ability")).isEqualTo(2L);
        assertThat(count("technique_stage")).isEqualTo(2L);
    }

    @Test
    void saveEffectLinesAndStagePowersInOrder() {
        Technique technique = techniqueRepository.save(TechniqueFixture.changchun());
        entityManager.flush();
        entityManager.clear();

        Technique found = techniqueRepository.findById(technique.getId()).orElseThrow();

        Ability fireBall = found.getAbilities().getFirst();
        assertThat(fireBall.getEffectLines()).containsExactlyElementsOf(TechniqueFixture.FIRE_BALL_LINES);
        assertThat(fireBall.getStagePowers()).extracting(AbilityStagePower::stage)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(fireBall.getStagePowers()).extracting(power -> power.basePower().toString())
                .containsExactly("2.00", "2.20", "2.40", "2.60", "2.80", "3.00", "3.20", "3.40", "3.60");
        assertThat(fireBall.getStagePowers()).extracting(power -> power.coefficient().toString())
                .containsOnly("1.200");
        assertThat(found.getAbilities().get(1).getStagePowers()).isEmpty();
        assertThat(found.getAbilities().get(1).getEffectLines()).containsExactly("천안술 기운이 퍼진다.");
    }

    @Test
    void replaceAbilitiesReplacesCollections() {
        Technique technique = techniqueRepository.save(TechniqueFixture.changchun());
        entityManager.flush();
        entityManager.clear();

        Technique loaded = techniqueRepository.findById(technique.getId()).orElseThrow();
        loaded.update(TechniqueFixture.changchunWith(List.of(withEffectLines(fireBall(), List.of("새 불꽃이 튄다!")))));
        entityManager.flush();
        entityManager.clear();

        Technique found = techniqueRepository.findById(technique.getId()).orElseThrow();
        assertThat(found.getAbilities()).singleElement()
                .satisfies(ability -> assertThat(ability.getEffectLines()).containsExactly("새 불꽃이 튄다!"));
        assertThat(count("ability_effect_line")).isEqualTo(1L);
        assertThat(count("ability_stage_power")).isEqualTo(9L);
    }

    @Test
    void duplicateNameFail() {
        techniqueRepository.save(TechniqueFixture.changchun());

        assertThatThrownBy(() -> {
            techniqueRepository.save(TechniqueFixture.changchun());
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByOrderByNameAsc() {
        techniqueRepository.save(TechniqueFixture.greenWood());
        techniqueRepository.save(TechniqueFixture.ironBody());
        techniqueRepository.save(TechniqueFixture.changchun());
        entityManager.flush();
        entityManager.clear();

        List<Technique> found = techniqueRepository.findAllByOrderByNameAsc();

        assertThat(found).extracting(Technique::getName).containsExactly("강체결", "장춘공", "청목결");
        assertThat(found).extracting(technique -> technique.getAbilities().size()).containsExactly(1, 3, 2);
        assertThat(found.get(1).getAbilities().getFirst().getElements()).containsExactly(Element.FIRE);
    }

    @Test
    void existsByNameIgnoreCase() {
        Technique technique = techniqueRepository.save(Technique.create(TechniqueFixture.changchunInfo("Spring")));

        assertThat(techniqueRepository.existsByNameIgnoreCase("sPRING")).isTrue();
        assertThat(techniqueRepository.existsByNameIgnoreCase("Sprin")).isFalse();
        assertThat(techniqueRepository.existsByNameIgnoreCaseAndIdNot("spring", technique.getId())).isFalse();
        assertThat(techniqueRepository.existsByNameIgnoreCaseAndIdNot("spring", technique.getId() + 1)).isTrue();
    }

    @Test
    void delete() {
        Technique technique = techniqueRepository.save(TechniqueFixture.changchun());
        entityManager.flush();

        techniqueRepository.delete(technique);
        entityManager.flush();

        assertThat(techniqueRepository.findById(technique.getId())).isEmpty();
        assertThat(count("ability")).isEqualTo(0L);
        assertThat(count("ability_element")).isEqualTo(0L);
        assertThat(count("technique_stage")).isEqualTo(0L);
        assertThat(count("technique_element")).isEqualTo(0L);
        assertThat(count("ability_effect_line")).isEqualTo(0L);
        assertThat(count("ability_stage_power")).isEqualTo(0L);
    }

    private Object count(String table) {
        return entityManager.createNativeQuery("select count(*) from " + table).getSingleResult();
    }

    private static AbilitySaveInfo mixed() {
        return new AbilitySaveInfo("토화술", 2, AbilityKind.AREA_ATTACK, List.of(Element.EARTH, Element.FIRE), false,
                AbilityTarget.ALL_ENEMIES, CostStat.SENSE, new java.math.BigDecimal("1.5"), true,
                new java.math.BigDecimal("2.5"), new java.math.BigDecimal("3600"), UsableWhen.ANYTIME,
                List.of("땅과 불이 함께 일어난다."), List.of());
    }

    private static AbilitySaveInfo windRidingAt(int stage) {
        return ability("어풍결", stage, AbilityKind.ESCAPE, List.of(Element.WOOD), AbilityTarget.SELF, CostStat.MANA,
                "10", "0", "10", UsableWhen.ANYTIME);
    }
}
