package mudmk3.application.faction;

import lombok.RequiredArgsConstructor;
import mudmk3.application.faction.provided.FactionEditor;
import mudmk3.application.faction.provided.FactionFinder;
import mudmk3.application.faction.provided.FactionInUseException;
import mudmk3.application.faction.provided.FactionSaveRequest;
import mudmk3.application.faction.required.FactionRepository;
import mudmk3.domain.faction.DuplicateFactionNameException;
import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionSaveInfo;
import mudmk3.support.stereotype.ValidatedApplicationService;

@ValidatedApplicationService
@RequiredArgsConstructor
public class FactionModifyService implements FactionEditor {

    private final FactionFinder factionFinder;
    private final FactionRepository factionRepository;

    @Override
    public Faction create(FactionSaveRequest request) {
        FactionSaveInfo info = request.toInfo();
        if (factionRepository.existsByName(info.name())) {
            throw duplicateName(info.name());
        }
        return factionRepository.save(Faction.create(info));
    }

    @Override
    public Faction update(Long factionId, FactionSaveRequest request) {
        Faction faction = factionFinder.find(factionId);
        FactionSaveInfo info = request.toInfo();
        if (factionRepository.existsByNameAndIdNot(info.name(), factionId)) {
            throw duplicateName(info.name());
        }
        faction.update(info);
        return factionRepository.save(faction);
    }

    @Override
    public void delete(Long factionId) {
        Faction faction = factionFinder.find(factionId);
        if (factionFinder.countZones(factionId) > 0) {
            throw new FactionInUseException("소유한 지역이 있는 성·종문은 지울 수 없습니다");
        }
        factionRepository.delete(faction);
    }

    private static DuplicateFactionNameException duplicateName(String name) {
        return new DuplicateFactionNameException("이미 있는 성·종문 이름입니다: " + name);
    }
}
