package mudmk3.adapter.persistence;

import lombok.RequiredArgsConstructor;
import mudmk3.application.room.required.StartRoomChecker;
import mudmk3.application.world.required.WorldSettingsRepository;
import mudmk3.domain.world.WorldSettings;
import mudmk3.support.stereotype.Adapter;

/** 세계 설정 저장소로 시작 방인지 답한다(room → world 슬라이스 의존을 피하려고 어댑터에 둔다) */
@Adapter
@RequiredArgsConstructor
public class StartRoomCheckerAdapter implements StartRoomChecker {

    private final WorldSettingsRepository worldSettingsRepository;

    @Override
    public boolean isStartRoom(Long roomId) {
        return worldSettingsRepository.findFirstByOrderByIdAsc()
                .flatMap(WorldSettings::findStartRoom)
                .map(room -> roomId.equals(room.getId()))
                .orElse(false);
    }
}
