package mudmk3.application.world.required;

import jakarta.persistence.EntityManager;
import mudmk3.application.room.required.RoomRepository;
import mudmk3.application.zone.required.ZoneRepository;
import mudmk3.domain.room.Room;
import mudmk3.domain.room.RoomFixture;
import mudmk3.domain.world.WorldSettings;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
record WorldSettingsRepositoryTest(
        WorldSettingsRepository worldSettingsRepository,
        RoomRepository roomRepository,
        ZoneRepository zoneRepository,
        EntityManager entityManager
) {

    @Test
    void findFirstWhenEmpty() {
        assertThat(worldSettingsRepository.findFirstByOrderByIdAsc()).isEmpty();
    }

    @Test
    void startRoomPersisted() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone());
        Room room = roomRepository.save(RoomFixture.createRoom(zone, "청석숲 오솔길"));
        WorldSettings settings = WorldSettings.create();
        settings.designateStartRoom(room);
        worldSettingsRepository.save(settings);
        entityManager.flush();
        entityManager.clear();

        WorldSettings found = worldSettingsRepository.findFirstByOrderByIdAsc().orElseThrow();

        assertThat(found.findStartRoom()).hasValueSatisfying(start -> {
            assertThat(start).isEqualTo(room);
            assertThat(start.getName()).isEqualTo("청석숲 오솔길");
        });
        assertThat(found.isStartRoom(roomRepository.findById(room.getId()).orElseThrow())).isTrue();
        // 분리된(detached) 원본으로 물어도 id로 같은 방이다
        assertThat(found.isStartRoom(room)).isTrue();
    }
}
