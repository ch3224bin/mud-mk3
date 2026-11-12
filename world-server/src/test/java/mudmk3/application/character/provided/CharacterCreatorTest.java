package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.character.CharacterAlreadyExistsException;
import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.DuplicateCharacterNameException;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record CharacterCreatorTest(
        CharacterCreator characterCreator,
        CharacterFinder characterFinder,
        AccountLogin accountLogin,
        ZoneEditor zoneEditor,
        RoomEditor roomEditor,
        WorldSettingsEditor worldSettingsEditor,
        EntityManager entityManager
) {

    @Test
    void create() {
        Long startRoomId = prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");

        PlayerCharacter character = characterCreator.create(accountId, "한백");
        flushAndClear();

        PlayerCharacter found = characterFinder.findByAccountId(accountId).orElseThrow();
        assertThat(found.getId()).isEqualTo(character.getId());
        assertThat(found.getName()).isEqualTo(new CharacterName("한백"));
        assertThat(found.getRoomId()).isEqualTo(startRoomId);
        assertThat(characterFinder.isNameTaken("한백")).isTrue();
        assertThat(characterFinder.isNameTaken("한립")).isFalse();
        assertThat(characterFinder.isNameTaken("한 백")).isFalse();
    }

    @Test
    void findByAccountIdWithoutCharacter() {
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void duplicateNameFail() {
        prepareStartRoom();
        characterCreator.create(prepareAccount("hanbaek@example.com"), "한백");
        Long otherAccountId = prepareAccount("hanli@example.com");

        assertThatThrownBy(() -> characterCreator.create(otherAccountId, "한백"))
                .isInstanceOf(DuplicateCharacterNameException.class)
                .hasMessage("이미 있는 이름입니다. 다른 이름을 정하십시오.");
    }

    @Test
    void secondCharacterForAccountFail() {
        prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");
        characterCreator.create(accountId, "한백");

        assertThatThrownBy(() -> characterCreator.create(accountId, "한립"))
                .isInstanceOf(CharacterAlreadyExistsException.class)
                .hasMessage("이미 캐릭터가 있습니다");
        // 계정 검사가 이름 중복 검사보다 먼저다
        assertThatThrownBy(() -> characterCreator.create(accountId, "한백"))
                .isInstanceOf(CharacterAlreadyExistsException.class);
    }

    @Test
    void invalidNameFail() {
        prepareStartRoom();
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThatThrownBy(() -> characterCreator.create(accountId, "한 백"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.");
        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void noStartRoomFail() {
        Long accountId = prepareAccount("hanbaek@example.com");

        assertThatThrownBy(() -> characterCreator.create(accountId, "한백"))
                .isInstanceOf(WorldNotReadyException.class)
                .hasMessage("아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.");
        assertThat(characterFinder.findByAccountId(accountId)).isEmpty();
    }

    @Test
    void unknownAccountFail() {
        prepareStartRoom();

        assertThatThrownBy(() -> characterCreator.create(-1L, "한백"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("계정을 찾을 수 없습니다. id: -1");
    }

    private Long prepareStartRoom() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        worldSettingsEditor.designateStartRoom(room.getId());
        return room.getId();
    }

    private Long prepareAccount(String email) {
        return accountLogin.login(new AccountLoginRequest("google-" + email, email)).accountId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
