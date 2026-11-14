package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.FloorPlacementResponse;
import mudmk3.application.floor.provided.FloorPlacementEditor;
import mudmk3.application.floor.provided.FloorPlacementSaveRequest;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Clock;

/** 관리: 방 바닥에 둔 물건 */
@WebApiAdapter
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class FloorItemApi {

    private final FloorPlacementEditor floorPlacementEditor;
    private final Clock clock;

    @PostMapping("/rooms/{roomId}/floor-items")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorPlacementResponse create(@PathVariable Long roomId, @RequestBody @Valid FloorPlacementSaveRequest request) {
        return FloorPlacementResponse.of(floorPlacementEditor.create(roomId, request), clock.instant());
    }

    @PutMapping("/floor-items/{floorItemId}")
    public FloorPlacementResponse update(@PathVariable Long floorItemId, @RequestBody @Valid FloorPlacementSaveRequest request) {
        return FloorPlacementResponse.of(floorPlacementEditor.update(floorItemId, request), clock.instant());
    }

    @DeleteMapping("/floor-items/{floorItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long floorItemId) {
        floorPlacementEditor.delete(floorItemId);
    }
}
