package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.NpcDetailResponse;
import mudmk3.adapter.webapi.dto.NpcSummaryResponse;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.npc.provided.ShopSaveRequest;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.ShopItem;
import mudmk3.domain.room.Room;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 관리: NPC */
@WebApiAdapter
@RequestMapping("/api/admin/npcs")
@RequiredArgsConstructor
public class NpcApi {

    private final NpcFinder npcFinder;
    private final NpcEditor npcEditor;
    private final RoomFinder roomFinder;
    private final ItemTypeFinder itemTypeFinder;

    @GetMapping
    public List<NpcSummaryResponse> list() {
        return npcFinder.findAll().stream()
                .map(npc -> NpcSummaryResponse.of(npc, placedRoom(npc)))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NpcDetailResponse create(@RequestBody @Valid NpcSaveRequest request) {
        return toDetail(npcEditor.create(request));
    }

    @GetMapping("/{npcId}")
    public NpcDetailResponse get(@PathVariable Long npcId) {
        return toDetail(npcFinder.find(npcId));
    }

    @PutMapping("/{npcId}")
    public NpcDetailResponse update(@PathVariable Long npcId, @RequestBody @Valid NpcSaveRequest request) {
        return toDetail(npcEditor.update(npcId, request));
    }

    @DeleteMapping("/{npcId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long npcId) {
        npcEditor.delete(npcId);
    }

    @PutMapping("/{npcId}/shop")
    public NpcDetailResponse openShop(@PathVariable Long npcId, @RequestBody @Valid ShopSaveRequest request) {
        return toDetail(npcEditor.openShop(npcId, request));
    }

    @DeleteMapping("/{npcId}/shop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeShop(@PathVariable Long npcId) {
        npcEditor.closeShop(npcId);
    }

    /** 상점 물건은 물건 종류를 id로만 가지므로 이름·기준가를 따로 찾는다 */
    private NpcDetailResponse toDetail(Npc npc) {
        List<Long> itemTypeIds = npc.getShopItems().stream().map(ShopItem::itemTypeId).toList();
        Map<Long, ItemType> itemTypes = itemTypeIds.isEmpty() ? Map.of() : itemTypeFinder.findAllById(itemTypeIds);
        return NpcDetailResponse.of(npc, placedRoom(npc), itemTypes);
    }

    private Optional<Room> placedRoom(Npc npc) {
        return npc.findRoomId().flatMap(roomFinder::findOptional);
    }
}
