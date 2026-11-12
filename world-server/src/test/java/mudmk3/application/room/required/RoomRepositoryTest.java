package mudmk3.application.room.required;

import jakarta.persistence.EntityManager;
import mudmk3.application.zone.required.ZoneRepository;
import mudmk3.domain.room.Direction;
import mudmk3.domain.room.Exit;
import mudmk3.domain.room.Room;
import mudmk3.domain.room.RoomFixture;
import mudmk3.domain.zone.Zone;
import mudmk3.domain.zone.ZoneFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
record RoomRepositoryTest(RoomRepository roomRepository, ZoneRepository zoneRepository, EntityManager entityManager) {

    @Test
    void saveWithExits() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone());
        Room room = roomRepository.save(RoomFixture.createRoom(zone, "청석숲 오솔길"));
        Room north = roomRepository.save(RoomFixture.createRoom(zone, "샘터"));
        Room up = roomRepository.save(RoomFixture.createRoom(zone, "바위 언덕"));
        room.setExit(Direction.UP, up);
        room.setExit(Direction.NORTH, north);
        entityManager.flush();
        entityManager.clear();

        Room found = roomRepository.findById(room.getId()).orElseThrow();

        assertThat(found.getZone().getName()).isEqualTo("청석숲");
        assertThat(found.getExits()).extracting(Exit::getDirection)
                .containsExactly(Direction.NORTH, Direction.UP);
        assertThat(found.findExit(Direction.NORTH).orElseThrow().getTarget()).isEqualTo(north);
        assertThat(found.findExit(Direction.NORTH).orElseThrow().getTarget().getName()).isEqualTo("샘터");
        assertThat(found.findExit(Direction.UP).orElseThrow().getTarget().getName()).isEqualTo("바위 언덕");
    }

    @Test
    void removeExitDeletesOrphan() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone());
        Room room = roomRepository.save(RoomFixture.createRoom(zone, "청석숲 오솔길"));
        Room north = roomRepository.save(RoomFixture.createRoom(zone, "샘터"));
        room.setExit(Direction.NORTH, north);
        entityManager.flush();

        room.removeExit(Direction.NORTH);
        entityManager.flush();
        entityManager.clear();

        assertThat(roomRepository.findById(room.getId()).orElseThrow().getExits()).isEmpty();
        assertThat(entityManager.createQuery("select count(e) from Exit e", Long.class).getSingleResult())
                .isZero();
    }

    @Test
    void findAllWithExitTo() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone());
        Room a = roomRepository.save(RoomFixture.createRoom(zone, "가"));
        Room b = roomRepository.save(RoomFixture.createRoom(zone, "나"));
        Room c = roomRepository.save(RoomFixture.createRoom(zone, "다"));
        Room d = roomRepository.save(RoomFixture.createRoom(zone, "라"));
        a.setExit(Direction.EAST, b);
        a.setExit(Direction.UP, b);
        c.setExit(Direction.WEST, b);
        b.setExit(Direction.NORTH, d);
        entityManager.flush();
        entityManager.clear();

        Room target = roomRepository.findById(b.getId()).orElseThrow();

        assertThat(roomRepository.findAllWithExitTo(target)).containsExactly(a, c);
    }

    @Test
    void findAllByZoneOrderByIdAsc() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone("청석숲"));
        Zone other = zoneRepository.save(ZoneFixture.createZone("황풍곡"));
        Room first = roomRepository.save(RoomFixture.createRoom(zone, "샘터"));
        roomRepository.save(RoomFixture.createRoom(other, "바람 골짜기"));
        Room second = roomRepository.save(RoomFixture.createRoom(zone, "가람 오솔길"));

        assertThat(roomRepository.findAllByZoneOrderByIdAsc(zone)).containsExactly(first, second);
    }

    @Test
    void countByZone() {
        Zone zone = zoneRepository.save(ZoneFixture.createZone("청석숲"));
        Zone other = zoneRepository.save(ZoneFixture.createZone("황풍곡"));
        roomRepository.save(RoomFixture.createRoom(zone, "샘터"));
        roomRepository.save(RoomFixture.createRoom(zone, "가람 오솔길"));
        roomRepository.save(RoomFixture.createRoom(other, "바람 골짜기"));

        assertThat(roomRepository.countByZone(zone)).isEqualTo(2);
        assertThat(roomRepository.countByZone(other)).isEqualTo(1);
    }
}
