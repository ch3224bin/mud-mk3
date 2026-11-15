package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.MobDetailResponse;
import mudmk3.adapter.webapi.dto.MobSummaryResponse;
import mudmk3.application.mob.provided.MobTypeEditor;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.application.mob.provided.MobTypeSaveRequest;
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

/** 관리: 몹 유형과 드롭 표 */
@WebApiAdapter
@RequestMapping("/api/admin/mobs")
@RequiredArgsConstructor
public class MobApi {

    private final MobTypeFinder mobTypeFinder;
    private final MobTypeEditor mobTypeEditor;

    @GetMapping
    public List<MobSummaryResponse> list() {
        return mobTypeFinder.viewAll().stream().map(MobSummaryResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MobDetailResponse create(@RequestBody @Valid MobTypeSaveRequest request) {
        return MobDetailResponse.of(mobTypeEditor.create(request));
    }

    @GetMapping("/{mobId}")
    public MobDetailResponse find(@PathVariable Long mobId) {
        return MobDetailResponse.of(mobTypeFinder.view(mobId));
    }

    @PutMapping("/{mobId}")
    public MobDetailResponse update(@PathVariable Long mobId, @RequestBody @Valid MobTypeSaveRequest request) {
        return MobDetailResponse.of(mobTypeEditor.update(mobId, request));
    }

    @DeleteMapping("/{mobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long mobId) {
        mobTypeEditor.delete(mobId);
    }
}
