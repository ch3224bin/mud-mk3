package mudmk3.application.character;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mudmk3.application.account.provided.AccountFinder;
import mudmk3.application.character.provided.CharacterCreator;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.CharacterMover;
import mudmk3.application.character.provided.CharacterRealmEditor;
import mudmk3.application.character.provided.RealmChangedEvent;
import mudmk3.application.character.provided.WorldNotReadyException;
import mudmk3.application.character.required.CharacterRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.world.provided.WorldSettingsFinder;
import mudmk3.domain.character.CharacterAlreadyExistsException;
import mudmk3.domain.character.CharacterCreateInfo;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.DuplicateCharacterNameException;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Realm;
import mudmk3.support.stereotype.ApplicationService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class CharacterModifyService implements CharacterCreator, CharacterMover, CharacterRealmEditor {

    private final CharacterFinder characterFinder;
    private final CharacterRepository characterRepository;
    private final AccountFinder accountFinder;
    private final WorldSettingsFinder worldSettingsFinder;
    private final InventoryEditor inventoryEditor;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PlayerCharacter create(Long accountId, String name, SpiritRoot spiritRoot) {
        requireNonNull(spiritRoot);
        CharacterName characterName = new CharacterName(name);
        accountFinder.find(accountId);
        if (characterFinder.findByAccountId(accountId).isPresent()) {
            throw new CharacterAlreadyExistsException("이미 캐릭터가 있습니다");
        }
        if (characterFinder.isNameTaken(characterName.value())) {
            throw new DuplicateCharacterNameException("이미 있는 이름입니다. 다른 이름을 정하십시오.");
        }
        Room startRoom = worldSettingsFinder.findStartRoom()
                .orElseThrow(() -> new WorldNotReadyException(
                        "아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다."));
        PlayerCharacter character = characterRepository.save(PlayerCharacter.create(
                new CharacterCreateInfo(accountId, characterName, requireNonNull(startRoom.getId()), spiritRoot)));
        // 같은 트랜잭션에서 시작 소지품을 준다(바꾼 시작 소지품은 그 뒤 만드는 캐릭터부터 받는다)
        inventoryEditor.createWithStartingItems(requireNonNull(character.getId()));
        return character;
    }

    @Override
    public void chooseSpiritRoot(Long characterId, SpiritRoot spiritRoot) {
        PlayerCharacter character = characterFinder.find(characterId);
        character.chooseSpiritRoot(spiritRoot);
        characterRepository.save(character);
    }

    @Override
    public void designate(Long characterId, Realm realm) {
        requireNonNull(realm);
        PlayerCharacter character = characterFinder.find(characterId);
        Realm from = character.getRealm();
        if (from == realm) {
            return;
        }
        character.changeRealm(realm);
        characterRepository.save(character);
        eventPublisher.publishEvent(new RealmChangedEvent(characterId, from, realm));
    }

    @Override
    public void initiate(Long characterId) {
        PlayerCharacter character = characterFinder.find(characterId);
        if (character.getRealm() != Realm.MORTAL) {
            return;
        }
        character.changeRealm(Realm.QI_1);
        characterRepository.save(character);
    }

    /** 서버가 뜰 때 경지 열이 비어 있는 기존 캐릭터를 연기 1층으로 채운다(영석 심기보다 먼저) */
    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    @Transactional
    public void fillMissingRealms() {
        int filled = characterRepository.fillMissingRealms(Realm.QI_1);
        if (filled > 0) {
            log.info("경지가 비어 있던 캐릭터 {}명을 {} 채웠습니다", filled, Josa.withEuroRo(Realm.QI_1.koreanName()));
        }
    }

    @Override
    public void moveTo(Long characterId, Long roomId) {
        PlayerCharacter character = characterFinder.find(characterId);
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
}
