package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.NpcDetailResponse;
import mudmk3.adapter.webapi.dto.NpcSummaryResponse;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.npc.Npc;
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
import java.util.Optional;

/** 관리: NPC */
@WebApiAdapter
@RequestMapping("/api/admin/npcs")
@RequiredArgsConstructor
public class NpcApi {

    private final NpcFinder npcFinder;
    private final NpcEditor npcEditor;
    private final RoomFinder roomFinder;

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

    private NpcDetailResponse toDetail(Npc npc) {
        return NpcDetailResponse.of(npc, placedRoom(npc));
    }

    private Optional<Room> placedRoom(Npc npc) {
        return npc.findRoomId().flatMap(roomFinder::findOptional);
    }
}
