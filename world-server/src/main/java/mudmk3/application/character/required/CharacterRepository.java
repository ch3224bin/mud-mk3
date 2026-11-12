package mudmk3.application.character.required;

import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** 캐릭터를 저장하고 찾는다 */
public interface CharacterRepository extends Repository<PlayerCharacter, Long> {

    PlayerCharacter save(PlayerCharacter character);

    Optional<PlayerCharacter> findById(Long id);

    Optional<PlayerCharacter> findByAccountId(Long accountId);

    boolean existsByName(CharacterName name);

    List<PlayerCharacter> findAllByRoomId(Long roomId);
}
