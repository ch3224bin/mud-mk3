package mudmk3.application.faction.provided;

import jakarta.validation.Valid;
import mudmk3.domain.faction.Faction;

/** 성·종문을 만들고 고치고 지운다 */
public interface FactionEditor {

    Faction create(@Valid FactionSaveRequest request);

    Faction update(Long factionId, @Valid FactionSaveRequest request);

    void delete(Long factionId);
}
