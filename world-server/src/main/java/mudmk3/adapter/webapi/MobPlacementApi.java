package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.MobPlacementResponse;
import mudmk3.application.mob.provided.MobPlacementEditor;
import mudmk3.application.mob.provided.MobPlacementSaveRequest;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 관리: 방에 둔 몹 */
@WebApiAdapter
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class MobPlacementApi {

    private final MobPlacementEditor mobPlacementEditor;

    @PostMapping("/rooms/{roomId}/mobs")
    @ResponseStatus(HttpStatus.CREATED)
    public MobPlacementResponse create(@PathVariable Long roomId, @RequestBody @Valid MobPlacementSaveRequest request) {
        return MobPlacementResponse.of(mobPlacementEditor.create(roomId, request));
    }

    @PutMapping("/mob-placements/{placementId}")
    public MobPlacementResponse update(@PathVariable Long placementId,
                                       @RequestBody @Valid MobPlacementSaveRequest request) {
        return MobPlacementResponse.of(mobPlacementEditor.update(placementId, request));
    }

    @DeleteMapping("/mob-placements/{placementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long placementId) {
        mobPlacementEditor.delete(placementId);
    }
}
