package mudmk3.application.character.required;

import mudmk3.domain.character.CharacterName;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.shared.Realm;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    /** 모든 캐릭터(이름순, 영근 함께) */
    @EntityGraph(attributePaths = "spiritRoots")
    List<PlayerCharacter> findAllByOrderByNameValueAsc();

    /** 경지 열이 비어 있는 캐릭터를 모두 그 경지로 채우고 채운 행 수를 돌려준다 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PlayerCharacter c set c.realm = :realm where c.realm is null")
    int fillMissingRealms(Realm realm);
}
