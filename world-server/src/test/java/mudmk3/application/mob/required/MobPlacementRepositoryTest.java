package mudmk3.application.mob.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobPlacement;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Map;

import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.withDrops;
import static mudmk3.domain.mob.MobFixture.wolf;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
record MobPlacementRepositoryTest(MobPlacementRepository mobPlacementRepository, MobTypeRepository mobTypeRepository,
                                  EntityManager entityManager) {

    @Test
    void save() {
        MobType wolf = mobTypeRepository.save(MobType.create(wolf(), Map.of()));
        MobPlacement placement = mobPlacementRepository.save(MobPlacement.create(10L, wolf, 3));
        flushAndClear();

        MobPlacement found = mobPlacementRepository.findById(placement.getId()).orElseThrow();
        entityManager.clear();

        assertThat(found.getRoomId()).isEqualTo(10L);
        assertThat(found.getMobType().getName()).isEqualTo("청모랑");
        assertThat(found.getCount()).isEqualTo(3);
    }

    /** 틱이 트랜잭션 밖에서 몹 정의를 복사할 수 있게 몹 유형과 드롭 표 둘을 함께 읽는다 */
    @Test
    void findAllWithMobTypeAndDrops() {
        MobType rabbit = mobTypeRepository.save(MobType.create(withDrops(rabbit(),
                List.of(new MobFixedDrop(1L, 1), new MobFixedDrop(2L, 2)),
                List.of(new MobTierDrop(DropTier.ADVANCED, 3L), new MobTierDrop(DropTier.RARE, 1L))),
                Map.of(1L, "토끼 가죽", 2L, "토끼 고기", 3L, "회춘단")));
        MobType wolf = mobTypeRepository.save(MobType.create(wolf(), Map.of()));
        MobPlacement first = mobPlacementRepository.save(MobPlacement.create(11L, rabbit, 2));
        MobPlacement second = mobPlacementRepository.save(MobPlacement.create(10L, wolf, 1));
        MobPlacement third = mobPlacementRepository.save(MobPlacement.create(10L, rabbit, 4));
        flushAndClear();

        List<MobPlacement> all = mobPlacementRepository.findAllByOrderByIdAsc();
        entityManager.clear();

        assertThat(all).extracting(MobPlacement::getId).containsExactly(first.getId(), second.getId(), third.getId());
        MobType loaded = all.getFirst().getMobType();
        assertThat(loaded.getFixedDrops()).containsExactly(new MobFixedDrop(1L, 1), new MobFixedDrop(2L, 2));
        assertThat(loaded.getTierDrops()).containsExactly(new MobTierDrop(DropTier.ADVANCED, 3L),
                new MobTierDrop(DropTier.RARE, 1L));
        assertThat(all.get(1).getMobType().getFixedDrops()).isEmpty();
    }

    @Test
    void findAllInRoomAndCount() {
        MobType rabbit = mobTypeRepository.save(MobType.create(rabbit(), Map.of()));
        MobType wolf = mobTypeRepository.save(MobType.create(wolf(), Map.of()));
        MobPlacement first = mobPlacementRepository.save(MobPlacement.create(10L, wolf, 1));
        mobPlacementRepository.save(MobPlacement.create(11L, wolf, 2));
        MobPlacement third = mobPlacementRepository.save(MobPlacement.create(10L, rabbit, 3));
        flushAndClear();

        assertThat(mobPlacementRepository.findAllByRoomIdOrderByIdAsc(10L)).extracting(MobPlacement::getId)
                .containsExactly(first.getId(), third.getId());
        assertThat(mobPlacementRepository.findAllByMobTypeIdInOrderByIdAsc(List.of(rabbit.getId())))
                .extracting(MobPlacement::getId).containsExactly(third.getId());
        assertThat(mobPlacementRepository.countByMobTypeId(wolf.getId())).isEqualTo(2);
        assertThat(mobPlacementRepository.countByMobTypeId(rabbit.getId())).isEqualTo(1);
    }

    @Test
    void deleteByRoomId() {
        MobType wolf = mobTypeRepository.save(MobType.create(wolf(), Map.of()));
        mobPlacementRepository.save(MobPlacement.create(10L, wolf, 1));
        mobPlacementRepository.save(MobPlacement.create(10L, wolf, 2));
        MobPlacement kept = mobPlacementRepository.save(MobPlacement.create(11L, wolf, 3));
        flushAndClear();

        mobPlacementRepository.deleteByRoomId(10L);
        flushAndClear();

        assertThat(mobPlacementRepository.findAllByOrderByIdAsc()).extracting(MobPlacement::getId)
                .containsExactly(kept.getId());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
