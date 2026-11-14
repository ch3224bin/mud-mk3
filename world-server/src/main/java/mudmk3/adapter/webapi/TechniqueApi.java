package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.TechniqueDetailResponse;
import mudmk3.adapter.webapi.dto.TechniqueSummaryResponse;
import mudmk3.application.technique.provided.TechniqueEditor;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueSaveRequest;
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

/** 관리: 공법·신통 */
@WebApiAdapter
@RequestMapping("/api/admin/techniques")
@RequiredArgsConstructor
public class TechniqueApi {

    private final TechniqueFinder techniqueFinder;
    private final TechniqueEditor techniqueEditor;

    @GetMapping
    public List<TechniqueSummaryResponse> list() {
        return techniqueFinder.viewAll().stream().map(TechniqueSummaryResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TechniqueDetailResponse create(@RequestBody @Valid TechniqueSaveRequest request) {
        return TechniqueDetailResponse.of(techniqueEditor.create(request));
    }

    @GetMapping("/{techniqueId}")
    public TechniqueDetailResponse find(@PathVariable Long techniqueId) {
        return TechniqueDetailResponse.of(techniqueFinder.view(techniqueId));
    }

    @PutMapping("/{techniqueId}")
    public TechniqueDetailResponse update(@PathVariable Long techniqueId,
                                          @RequestBody @Valid TechniqueSaveRequest request) {
        return TechniqueDetailResponse.of(techniqueEditor.update(techniqueId, request));
    }

    @DeleteMapping("/{techniqueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long techniqueId) {
        techniqueEditor.delete(techniqueId);
    }
}
