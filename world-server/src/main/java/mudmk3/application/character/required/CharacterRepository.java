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

    /** 방을 쓴다(그 열만, 영속성 컨텍스트는 비우지 않음). 쓴 행 수를 돌려준다 */
    @Modifying(flushAutomatically = true)
    @Query("update PlayerCharacter c set c.roomId = :roomId where c.id = :id")
    int writeRoomId(Long id, Long roomId);

    /** 한 방에 있는 캐릭터를 모두 다른 방으로 쓴다(그 열만, 영속성 컨텍스트는 비우지 않음). 쓴 행 수를 돌려준다 */
    @Modifying(flushAutomatically = true)
    @Query("update PlayerCharacter c set c.roomId = :toRoomId where c.roomId = :fromRoomId")
    int writeRoomIdForAll(Long fromRoomId, Long toRoomId);

    /** 경지를 쓴다(그 열만, 영속성 컨텍스트는 비우지 않음). 쓴 행 수를 돌려준다 */
    @Modifying(flushAutomatically = true)
    @Query("update PlayerCharacter c set c.realm = :realm where c.id = :id")
    int writeRealm(Long id, Realm realm);

    /** 깎인 기혈을 쓴다(그 열만, 영속성 컨텍스트는 비우지 않음). 쓴 행 수를 돌려준다 */
    @Modifying(flushAutomatically = true)
    @Query(value = "update player_character set qi_blood_lost = :lost where id = :id", nativeQuery = true)
    int writeQiBloodLost(Long id, double lost);

    /** 깎인 기혈(비어 있으면 0). 없는 캐릭터면 비어 있다 */
    @Query(value = "select coalesce(qi_blood_lost, 0) from player_character where id = :id", nativeQuery = true)
    Optional<Double> readQiBloodLost(Long id);

    /** 깎인 법력을 쓴다(그 열만, 영속성 컨텍스트는 비우지 않음). 쓴 행 수를 돌려준다 */
    @Modifying(flushAutomatically = true)
    @Query(value = "update player_character set mana_lost = :lost where id = :id", nativeQuery = true)
    int writeManaLost(Long id, double lost);

    /** 깎인 법력(비어 있으면 0). 없는 캐릭터면 비어 있다 */
    @Query(value = "select coalesce(mana_lost, 0) from player_character where id = :id", nativeQuery = true)
    Optional<Double> readManaLost(Long id);
}
