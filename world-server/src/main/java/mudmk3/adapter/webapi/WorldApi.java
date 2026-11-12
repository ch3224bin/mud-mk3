package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.StartRoomRequest;
import mudmk3.adapter.webapi.dto.WorldResponse;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** 관리: 세계 설정(시작 방) */
@WebApiAdapter
@RequestMapping("/api/admin/world")
@RequiredArgsConstructor
public class WorldApi {

    private final WorldSettingsFinder worldSettingsFinder;
    private final WorldSettingsEditor worldSettingsEditor;

    @GetMapping
    public WorldResponse get() {
        return WorldResponse.of(worldSettingsFinder.findStartRoom());
    }

    @PutMapping("/start-room")
    public WorldResponse designateStartRoom(@RequestBody @Valid StartRoomRequest request) {
        return WorldResponse.of(worldSettingsEditor.designateStartRoom(request.roomId()).findStartRoom());
    }
}
