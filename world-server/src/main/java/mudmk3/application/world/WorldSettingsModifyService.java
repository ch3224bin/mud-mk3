package mudmk3.application.world;

import lombok.RequiredArgsConstructor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.world.required.WorldSettingsRepository;
import mudmk3.domain.room.Room;
import mudmk3.domain.world.WorldSettings;
import mudmk3.support.stereotype.ApplicationService;

@ApplicationService
@RequiredArgsConstructor
public class WorldSettingsModifyService implements WorldSettingsEditor {

    private final WorldSettingsRepository worldSettingsRepository;
    private final RoomFinder roomFinder;

    @Override
    public WorldSettings designateStartRoom(Long roomId) {
        Room room = roomFinder.find(roomId);
        WorldSettings settings = worldSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(WorldSettings::create);
        settings.designateStartRoom(room);
        return worldSettingsRepository.save(settings);
    }
}
