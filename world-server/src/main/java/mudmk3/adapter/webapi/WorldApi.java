package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.StartRoomRequest;
import mudmk3.adapter.webapi.dto.StartingItemResponse;
import mudmk3.adapter.webapi.dto.StartingItemsSaveRequest;
import mudmk3.adapter.webapi.dto.WorldResponse;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.world.StartingItem;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/** 관리: 세계 설정(시작 방·시작 소지품) */
@WebApiAdapter
@RequestMapping("/api/admin/world")
@RequiredArgsConstructor
public class WorldApi {

    private final WorldSettingsFinder worldSettingsFinder;
    private final WorldSettingsEditor worldSettingsEditor;
    private final ItemTypeFinder itemTypeFinder;

    @GetMapping
    public WorldResponse get() {
        return WorldResponse.of(worldSettingsFinder.findStartRoom());
    }

    @PutMapping("/start-room")
    public WorldResponse designateStartRoom(@RequestBody @Valid StartRoomRequest request) {
        return WorldResponse.of(worldSettingsEditor.designateStartRoom(request.roomId()).findStartRoom());
    }

    @GetMapping("/starting-items")
    public List<StartingItemResponse> startingItems() {
        return toResponses(worldSettingsFinder.findStartingItems());
    }

    @PutMapping("/starting-items")
    public List<StartingItemResponse> replaceStartingItems(@RequestBody @Valid StartingItemsSaveRequest request) {
        return toResponses(worldSettingsEditor.replaceStartingItems(request.items()));
    }

    /** 시작 소지품은 물건 종류를 id로만 가지므로 이름을 따로 찾는다(쓰이는 종류는 지울 수 없어 늘 있다) */
    private List<StartingItemResponse> toResponses(List<StartingItem> startingItems) {
        Map<Long, ItemType> itemTypes = itemTypeFinder.findAllById(startingItems.stream().map(StartingItem::itemTypeId).toList());
        return startingItems.stream()
                .map(item -> StartingItemResponse.of(item, requireNonNull(itemTypes.get(item.itemTypeId()),
                        () -> "물건 종류를 찾을 수 없습니다. id: " + item.itemTypeId())))
                .toList();
    }
}
