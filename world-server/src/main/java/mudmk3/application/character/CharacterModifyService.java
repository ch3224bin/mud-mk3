package mudmk3.application.character;

import lombok.RequiredArgsConstructor;
import mudmk3.application.account.provided.AccountFinder;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.character.provided.WorldNotReadyException;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.character.CharacterAlreadyExistsException;
import mudmk3.domain.character.CharacterCreateInfo;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.DuplicateCharacterNameException;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.room.Room;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;

import static java.util.Objects.requireNonNull;

@ApplicationService
@RequiredArgsConstructor
public class CharacterModifyService implements CharacterCreator, CharacterMover {

    private final CharacterFinder characterFinder;
    private final CharacterRepository characterRepository;
    private final AccountFinder accountFinder;
    private final WorldSettingsFinder worldSettingsFinder;

    @Override
    public PlayerCharacter create(Long accountId, String name) {
        CharacterName characterName = new CharacterName(name);
        accountFinder.find(accountId);
        if (characterFinder.findByAccountId(accountId).isPresent()) {
            throw new CharacterAlreadyExistsException("이미 캐릭터가 있습니다");
        }
        if (characterRepository.existsByName(characterName)) {
            throw new DuplicateCharacterNameException("이미 있는 이름입니다. 다른 이름을 정하십시오.");
        }
        Room startRoom = worldSettingsFinder.findStartRoom()
                .orElseThrow(() -> new WorldNotReadyException(
                        "아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다."));
        return characterRepository.save(PlayerCharacter.create(
                new CharacterCreateInfo(accountId, characterName, requireNonNull(startRoom.getId()))));
    }

    @Override
    public void moveTo(Long characterId, Long roomId) {
        PlayerCharacter character = find(characterId);
        character.moveTo(roomId);
        characterRepository.save(character);
    }

    @Override
    public List<Long> relocateAll(Long fromRoomId, Long toRoomId) {
        return characterRepository.findAllByRoomId(fromRoomId).stream()
                .map(character -> {
                    character.moveTo(toRoomId);
                    return requireNonNull(characterRepository.save(character).getId());
                })
                .toList();
    }

    private PlayerCharacter find(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + characterId));
    }
}
