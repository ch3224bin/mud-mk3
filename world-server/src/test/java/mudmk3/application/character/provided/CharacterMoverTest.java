package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.world.provided.WorldSettingsEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.room.Room;
import mudmk3.domain.zone.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record CharacterMoverTest(
        CharacterMover characterMover,
        CharacterCreator characterCreator,
        CharacterFinder characterFinder,
        AccountLogin accountLogin,
        ZoneEditor zoneEditor,
        RoomEditor roomEditor,
        WorldSettingsEditor worldSettingsEditor,
        EntityManager entityManager
) {

    @Test
    void moveTo() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room start = prepareStartRoom(zone);
        Room spring = roomEditor.create(createRoomSaveRequest(zone.getId(), "샘터"));
        Long accountId = prepareAccount("hanbaek@example.com");
        PlayerCharacter character = characterCreator.create(accountId, "한백");
        flushAndClear();

        characterMover.moveTo(character.getId(), spring.getId());
        flushAndClear();

        assertThat(characterFinder.findByAccountId(accountId).orElseThrow().getRoomId())
                .isEqualTo(spring.getId())
                .isNotEqualTo(start.getId());
    }

    @Test
    void moveToUnknownCharacterFail() {
        assertThatThrownBy(() -> characterMover.moveTo(-1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: -1");
    }

    @Test
    void relocateAll() {
        Zone zone = zoneEditor.create(createZoneSaveRequest("청석숲"));
        Room start = prepareStartRoom(zone);
        Room spring = roomEditor.create(createRoomSaveRequest(zone.getId(), "샘터"));
        Room hill = roomEditor.create(createRoomSaveRequest(zone.getId(), "언덕"));
        PlayerCharacter hanbaek = characterCreator.create(prepareAccount("hanbaek@example.com"), "한백");
        PlayerCharacter hanli = characterCreator.create(prepareAccount("hanli@example.com"), "한립");
        PlayerCharacter stayer = characterCreator.create(prepareAccount("stayer@example.com"), "남궁완");
        characterMover.moveTo(hanbaek.getId(), spring.getId());
        characterMover.moveTo(hanli.getId(), spring.getId());
        characterMover.moveTo(stayer.getId(), hill.getId());
        flushAndClear();

        List<Long> moved = characterMover.relocateAll(spring.getId(), start.getId());
        flushAndClear();

        assertThat(moved).containsExactlyInAnyOrder(hanbaek.getId(), hanli.getId());
        assertThat(roomIdOf("hanbaek@example.com")).isEqualTo(start.getId());
        assertThat(roomIdOf("hanli@example.com")).isEqualTo(start.getId());
        assertThat(roomIdOf("stayer@example.com")).isEqualTo(hill.getId());
        assertThat(characterMover.relocateAll(spring.getId(), start.getId())).isEmpty();
    }

    private Long roomIdOf(String email) {
        return characterFinder.findByAccountId(prepareAccount(email)).orElseThrow().getRoomId();
    }

    private Room prepareStartRoom(Zone zone) {
        Room room = roomEditor.create(createRoomSaveRequest(zone.getId(), "청석숲 오솔길"));
        worldSettingsEditor.designateStartRoom(room.getId());
        return room;
    }

    private Long prepareAccount(String email) {
        return accountLogin.login(new AccountLoginRequest("google-" + email, email)).accountId();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
