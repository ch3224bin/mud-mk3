package mudmk3.application.world;

import lombok.RequiredArgsConstructor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.application.world.required.WorldSettingsRepository;
import mudmk3.domain.room.Room;
import mudmk3.domain.world.StartingItem;
import mudmk3.domain.world.WorldSettings;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationService
@RequiredArgsConstructor
public class WorldSettingsQueryService implements WorldSettingsFinder {

    private final WorldSettingsRepository worldSettingsRepository;
    private final RoomFinder roomFinder;

    @Override
    public Optional<Room> findStartRoom() {
        // 설정의 시작 방은 LAZY 프록시라 RoomFinder 로 지역까지 읽은 방을 돌려준다
        return worldSettingsRepository.findFirstByOrderByIdAsc()
                .flatMap(WorldSettings::findStartRoom)
                .map(room -> roomFinder.find(Objects.requireNonNull(room.getId())));
    }

    @Override
    public boolean isStartRoom(Long roomId) {
        return worldSettingsRepository.findFirstByOrderByIdAsc()
                .flatMap(WorldSettings::findStartRoom)
                .map(room -> roomId.equals(room.getId()))
                .orElse(false);
    }

    @Override
    public List<StartingItem> findStartingItems() {
        // 요소 컬렉션은 LAZY라 트랜잭션 안에서 복사해 돌려준다(REST 응답은 트랜잭션 밖에서 만든다)
        return worldSettingsRepository.findFirstByOrderByIdAsc()
                .map(settings -> List.copyOf(settings.getStartingItems()))
                .orElse(List.of());
    }
}
