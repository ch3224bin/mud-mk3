package mudmk3.application.character.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.character.CharacterFixture.spiritRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 이동·경지 쓰기는 열 하나만 쓰므로, 먼저 읽어 둔 옛 엔티티가 있는 트랜잭션이 커밋돼도 다른 트랜잭션이 쓴 열을 되돌리지 않는다.
 * 실제 커밋이 필요해 테스트 트랜잭션을 쓰지 않고, 만든 캐릭터는 끝에 지운다
 */
@ApplicationServiceTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
record CharacterRowWriteTest(CharacterMover characterMover, CharacterRealmEditor characterRealmEditor,
                             CharacterFinder characterFinder, CharacterVitalsEditor characterVitalsEditor,
                             EntityManager entityManager, PlatformTransactionManager transactionManager) {

    private static final Long START = 10L;
    private static final Long NORTH_ROAD = 20L;
    private static final Long HILL = 30L;

    /** 바깥이 읽어 둔 뒤 안쪽이 옮겨 커밋하고, 바깥이 옛 방을 든 엔티티로 경지를 지정해 커밋해도 둘 다 남는다 */
    @Test
    void designateAndMoveBothSurvive() {
        Long han = prepareCharacter(9001L, "한백", START, Realm.QI_1);
        try {
            tx().executeWithoutResult(status -> {
                characterFinder.find(han);
                requiresNew().executeWithoutResult(inner -> characterMover.moveTo(han, NORTH_ROAD));
                characterRealmEditor.designate(han, Realm.QI_3);
            });

            assertCommitted(han, NORTH_ROAD, Realm.QI_3);
        } finally {
            delete(han);
        }
    }

    /** 바깥이 읽어 둔 뒤 안쪽이 경지를 지정해 커밋하고, 바깥이 옛 경지를 든 엔티티로 옮겨 커밋해도 둘 다 남는다 */
    @Test
    void moveAndDesignateBothSurvive() {
        Long han = prepareCharacter(9001L, "한백", START, Realm.QI_1);
        try {
            tx().executeWithoutResult(status -> {
                characterFinder.find(han);
                requiresNew().executeWithoutResult(inner -> characterRealmEditor.designate(han, Realm.QI_3));
                characterMover.moveTo(han, NORTH_ROAD);
            });

            assertCommitted(han, NORTH_ROAD, Realm.QI_3);
        } finally {
            delete(han);
        }
    }

    @Test
    void sameTransactionSeesNewRoom() {
        Long han = prepareCharacter(9001L, "한백", START, Realm.QI_1);
        try {
            tx().executeWithoutResult(status -> {
                characterFinder.find(han);

                characterMover.moveTo(han, NORTH_ROAD);
                assertThat(characterFinder.find(han).getRoomId()).isEqualTo(NORTH_ROAD);

                characterRealmEditor.designate(han, Realm.QI_3);
                assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_3);

                characterRealmEditor.initiate(han);
                assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_3);
            });

            assertCommitted(han, NORTH_ROAD, Realm.QI_3);
        } finally {
            delete(han);
        }
    }

    /** 방을 지워 옮기는 동안 다른 트랜잭션이 경지를 지정해 커밋해도 둘 다 남고, 다른 방의 캐릭터는 그대로다 */
    @Test
    void relocateAllKeepsRealm() {
        Long han = prepareCharacter(9001L, "한백", NORTH_ROAD, Realm.QI_1);
        Long stayer = prepareCharacter(9002L, "한립", HILL, Realm.QI_1);
        try {
            List<Long> moved = requireNonNull(tx().execute(status -> {
                characterFinder.find(han);
                requiresNew().executeWithoutResult(inner -> characterRealmEditor.designate(han, Realm.QI_3));
                return characterMover.relocateAll(NORTH_ROAD, START);
            }));

            assertThat(moved).containsExactly(han);
            assertCommitted(han, START, Realm.QI_3);
            assertCommitted(stayer, HILL, Realm.QI_1);
        } finally {
            delete(han);
            delete(stayer);
        }
    }

    /** 연기 7층에서 기혈 100·법력 40 깎임 → 옮겨도 그대로, 연기 1층으로 내리면 경지 몫이 준 만큼만 준다(기혈 40·법력 10) */
    @Test
    void moveDoesNotTouchVitals() {
        Long han = prepareCharacter(9001L, "한백", START, Realm.QI_7);
        try {
            tx().executeWithoutResult(status -> {
                characterFinder.find(han);
                characterVitalsEditor.takeDamage(han, 100, 120);
                characterVitalsEditor.spendMana(han, 40, 55);
                characterMover.moveTo(han, NORTH_ROAD);
            });
            assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(100);
            assertThat(characterVitalsEditor.manaLost(han)).isEqualTo(40);

            tx().executeWithoutResult(status -> {
                characterFinder.find(han);
                characterRealmEditor.designate(han, Realm.QI_1);
            });

            assertCommitted(han, NORTH_ROAD, Realm.QI_1);
            assertThat(characterVitalsEditor.qiBloodLost(han)).isEqualTo(40);
            assertThat(characterVitalsEditor.manaLost(han)).isEqualTo(10);
        } finally {
            delete(han);
        }
    }

    @Test
    void writeToMissingCharacterFails() {
        assertThatThrownBy(() -> characterMover.moveTo(999999L, NORTH_ROAD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: 999999");
        assertThatThrownBy(() -> characterRealmEditor.designate(999999L, Realm.QI_3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: 999999");
    }

    private void assertCommitted(Long characterId, Long roomId, Realm realm) {
        tx().executeWithoutResult(status -> {
            PlayerCharacter found = characterFinder.find(characterId);
            assertThat(found.getRoomId()).isEqualTo(roomId);
            assertThat(found.getRealm()).isEqualTo(realm);
        });
    }

    private Long prepareCharacter(Long accountId, String name, Long roomId, Realm realm) {
        return requireNonNull(tx().execute(status -> {
            PlayerCharacter character = createCharacter(accountId, name, roomId, spiritRoot());
            character.changeRealm(realm);
            entityManager.persist(character);
            return character.getId();
        }));
    }

    private void delete(Long characterId) {
        tx().executeWithoutResult(status -> entityManager.remove(entityManager.find(PlayerCharacter.class, characterId)));
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private TransactionTemplate requiresNew() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
