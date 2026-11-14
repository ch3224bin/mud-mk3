package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.FactionResponse;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.faction.provided.FactionFinder;
import mudmk3.application.faction.provided.FactionSaveRequest;
import mudmk3.domain.faction.Faction;
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

/** 관리: 성·종문 */
@WebApiAdapter
@RequestMapping("/api/admin/factions")
@RequiredArgsConstructor
public class FactionApi {

    private final FactionFinder factionFinder;
    private final FactionEditor factionEditor;

    @GetMapping
    public List<FactionResponse> list() {
        return factionFinder.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FactionResponse create(@RequestBody @Valid FactionSaveRequest request) {
        return toResponse(factionEditor.create(request));
    }

    @PutMapping("/{factionId}")
    public FactionResponse update(@PathVariable Long factionId, @RequestBody @Valid FactionSaveRequest request) {
        return toResponse(factionEditor.update(factionId, request));
    }

    @DeleteMapping("/{factionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long factionId) {
        factionEditor.delete(factionId);
    }

    private FactionResponse toResponse(Faction faction) {
        return FactionResponse.of(faction, factionFinder.countZones(faction.getId()));
    }
}
