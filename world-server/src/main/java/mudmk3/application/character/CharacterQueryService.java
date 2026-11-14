package mudmk3.application.character;

import lombok.RequiredArgsConstructor;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.support.stereotype.ApplicationService;

import java.util.Optional;

@ApplicationService
@RequiredArgsConstructor
public class CharacterQueryService implements CharacterFinder {

    private final CharacterRepository characterRepository;
    private final NpcFinder npcFinder;

    @Override
    public Optional<PlayerCharacter> findByAccountId(Long accountId) {
        return characterRepository.findByAccountId(accountId);
    }

    @Override
    public boolean isNameTaken(String name) {
        return CharacterName.isValid(name)
                && (characterRepository.existsByName(new CharacterName(name)) || npcFinder.existsByName(name));
    }
}
