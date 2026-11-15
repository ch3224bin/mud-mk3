package mudmk3.application.mob.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobFixture;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.withDrops;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record MobTypeRepositoryTest(MobTypeRepository mobTypeRepository, EntityManager entityManager) {

    private static final Map<Long, String> ITEM_NAMES = Map.of(1L, "토끼 가죽", 2L, "토끼 고기", 3L, "청모랑 가죽",
            4L, "늑대 이빨", 5L, "1급 요단", 6L, "회춘단");

    @Test
    void saveWithDrops() {
        MobType mob = mobTypeRepository.save(MobType.create(withDrops(boss(),
                List.of(new MobFixedDrop(2L, 3), new MobFixedDrop(1L, 1)),
                List.of(new MobTierDrop(DropTier.RARE, 6L), new MobTierDrop(DropTier.ADVANCED, 5L),
                        new MobTierDrop(DropTier.ADVANCED, 4L))), ITEM_NAMES));
        entityManager.flush();
        entityManager.clear();

        MobType found = mobTypeRepository.findById(mob.getId()).orElseThrow();
        entityManager.clear();

        // 트랜잭션 밖 응답이 읽을 수 있게 드롭 표 둘을 함께 읽는다
        assertThat(found.getName()).isEqualTo("쌍두 흑망");
        assertThat(found.getKind()).isEqualTo(MobKind.BEAST);
        assertThat(found.getRealm()).isEqualTo(Realm.QI_12);
        assertThat(found.getTier()).isEqualTo(MobTier.BOSS);
        assertThat(found.getAttackName()).isEqualTo("두 머리 물기");
        assertThat(found.getAttackCoefficient()).isEqualByComparingTo("0.6");
        assertThat(found.getRespawnSeconds()).isEqualTo(2_700);
        assertThat(found.getFixedDrops()).containsExactly(new MobFixedDrop(2L, 3), new MobFixedDrop(1L, 1));
        assertThat(found.getTierDrops()).containsExactly(new MobTierDrop(DropTier.RARE, 6L),
                new MobTierDrop(DropTier.ADVANCED, 5L), new MobTierDrop(DropTier.ADVANCED, 4L));
        assertThat(found.tierItemIds(DropTier.ADVANCED)).containsExactly(5L, 4L);
    }

    @Test
    void replaceDrops() {
        MobType mob = mobTypeRepository.save(MobType.create(withDrops(rabbit(),
                List.of(new MobFixedDrop(1L, 1), new MobFixedDrop(2L, 1)),
                List.of(new MobTierDrop(DropTier.ADVANCED, 6L))), ITEM_NAMES));
        entityManager.flush();
        entityManager.clear();

        MobType loaded = mobTypeRepository.findById(mob.getId()).orElseThrow();
        // 자리를 바꾸고 하나를 빼도 flush 가 막히지 않는다
        loaded.update(withDrops(rabbit(), List.of(new MobFixedDrop(2L, 4)),
                List.of(new MobTierDrop(DropTier.TREASURE, 6L), new MobTierDrop(DropTier.ADVANCED, 1L))), ITEM_NAMES);
        entityManager.flush();
        entityManager.clear();

        MobType found = mobTypeRepository.findById(mob.getId()).orElseThrow();
        assertThat(found.getFixedDrops()).containsExactly(new MobFixedDrop(2L, 4));
        assertThat(found.getTierDrops()).containsExactly(new MobTierDrop(DropTier.TREASURE, 6L),
                new MobTierDrop(DropTier.ADVANCED, 1L));
        assertThat(count("mob_fixed_drop")).isEqualTo(1L);
        assertThat(count("mob_tier_drop")).isEqualTo(2L);
    }

    @Test
    void duplicateNameFail() {
        mobTypeRepository.save(MobFixture.create(wolf()));

        assertThatThrownBy(() -> {
            mobTypeRepository.save(MobFixture.create(wolf()));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByOrderByNameAsc() {
        mobTypeRepository.save(MobFixture.create(wolf()));
        mobTypeRepository.save(MobFixture.create(boss()));
        mobTypeRepository.save(MobType.create(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 1)), List.of()),
                ITEM_NAMES));
        entityManager.flush();
        entityManager.clear();

        List<MobType> found = mobTypeRepository.findAllByOrderByNameAsc();
        entityManager.clear();

        assertThat(found).extracting(MobType::getName).containsExactly("들토끼", "쌍두 흑망", "청모랑");
        assertThat(found.getFirst().getFixedDrops()).containsExactly(new MobFixedDrop(1L, 1));
    }

    @Test
    void existsByNameIgnoreCase() {
        MobType mob = mobTypeRepository.save(MobFixture.create(MobFixture.named(wolf(), "Wolf")));

        assertThat(mobTypeRepository.existsByNameIgnoreCase("wOLF")).isTrue();
        assertThat(mobTypeRepository.existsByNameIgnoreCase("Wol")).isFalse();
        assertThat(mobTypeRepository.existsByNameIgnoreCaseAndIdNot("wolf", mob.getId())).isFalse();
        assertThat(mobTypeRepository.existsByNameIgnoreCaseAndIdNot("wolf", mob.getId() + 1)).isTrue();
    }

    @Test
    void delete() {
        MobType mob = mobTypeRepository.save(MobType.create(withDrops(rabbit(), List.of(new MobFixedDrop(1L, 1)),
                List.of(new MobTierDrop(DropTier.RARE, 6L))), ITEM_NAMES));
        entityManager.flush();

        mobTypeRepository.delete(mob);
        entityManager.flush();

        assertThat(mobTypeRepository.findById(mob.getId())).isEmpty();
        assertThat(count("mob_fixed_drop")).isEqualTo(0L);
        assertThat(count("mob_tier_drop")).isEqualTo(0L);
    }

    private Object count(String table) {
        return entityManager.createNativeQuery("select count(*) from " + table).getSingleResult();
    }
}
