package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.RoomDetailResponse;
import mudmk3.adapter.webapi.dto.RoomNpcResponse;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.room.provided.ExitSaveRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.room.provided.RoomSaveRequest;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.room.Direction;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 관리: 방과 출구 */
@WebApiAdapter
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class RoomApi {

    private final RoomFinder roomFinder;
    private final RoomEditor roomEditor;
    private final WorldSettingsFinder worldSettingsFinder;
    private final NpcFinder npcFinder;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDetailResponse create(@RequestBody @Valid RoomSaveRequest request) {
        return toResponse(roomEditor.create(request));
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse get(@PathVariable Long roomId) {
        return toResponse(roomFinder.find(roomId));
    }

    @PutMapping("/{roomId}")
    public RoomDetailResponse update(@PathVariable Long roomId, @RequestBody @Valid RoomSaveRequest request) {
        return toResponse(roomEditor.update(roomId, request));
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long roomId) {
        roomEditor.delete(roomId);
    }

    @PutMapping("/{roomId}/exits/{direction}")
    public RoomDetailResponse putExit(@PathVariable Long roomId, @PathVariable Direction direction,
                                      @RequestBody @Valid ExitSaveRequest request) {
        return toResponse(roomEditor.putExit(roomId, direction, request));
    }

    @DeleteMapping("/{roomId}/exits/{direction}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExit(@PathVariable Long roomId, @PathVariable Direction direction,
                           @RequestParam(defaultValue = "false") boolean twoWay) {
        roomEditor.deleteExit(roomId, direction, twoWay);
    }

    private RoomDetailResponse toResponse(Room room) {
        return RoomDetailResponse.of(room, worldSettingsFinder.isStartRoom(room.getId()),
                npcFinder.findAllInRoom(room.getId()).stream().map(RoomNpcResponse::of).toList());
    }
}
