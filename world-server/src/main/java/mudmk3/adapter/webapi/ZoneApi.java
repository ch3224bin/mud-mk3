package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.RoomSummaryResponse;
import mudmk3.adapter.webapi.dto.ZoneResponse;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.application.zone.provided.ZoneFinder;
import mudmk3.application.zone.provided.ZoneSaveRequest;
import mudmk3.domain.zone.Zone;
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

/** 관리: 지역 */
@WebApiAdapter
@RequestMapping("/api/admin/zones")
@RequiredArgsConstructor
public class ZoneApi {

    private final ZoneFinder zoneFinder;
    private final ZoneEditor zoneEditor;
    private final RoomFinder roomFinder;

    @GetMapping
    public List<ZoneResponse> list() {
        return zoneFinder.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse create(@RequestBody @Valid ZoneSaveRequest request) {
        return toResponse(zoneEditor.create(request));
    }

    @PutMapping("/{zoneId}")
    public ZoneResponse update(@PathVariable Long zoneId, @RequestBody @Valid ZoneSaveRequest request) {
        return toResponse(zoneEditor.update(zoneId, request));
    }

    @DeleteMapping("/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long zoneId) {
        zoneEditor.delete(zoneId);
    }

    @GetMapping("/{zoneId}/rooms")
    public List<RoomSummaryResponse> rooms(@PathVariable Long zoneId) {
        return roomFinder.findAllInZone(zoneId).stream().map(RoomSummaryResponse::of).toList();
    }

    private ZoneResponse toResponse(Zone zone) {
        return ZoneResponse.of(zone, zoneFinder.countRooms(zone.getId()));
    }
}
