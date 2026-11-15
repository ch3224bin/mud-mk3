package mudmk3.application.cultivation.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Technique;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.technique.TechniqueFixture.changchun;
import static mudmk3.domain.technique.TechniqueFixture.ironBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record CultivationFinderTest(CultivationFinder cultivationFinder, EntityManager entityManager,
                             PlatformTransactionManager transactionManager) {

    @Test
    void stats() {
        PlayerCharacter han = character(1L, "한백", "목 화", Realm.QI_1);
        learn(han, persist(changchun()));
        flushAndClear();

        assertThat(cultivationFinder.stats(han.getId()))
                .isEqualTo(new StatSheet(60, 60.0, 25, 20, 20, 5, 6, 3,
                        Map.of(Element.WOOD, 50.0, Element.FIRE, 50.0)));
    }

    /** 서비스가 트랜잭션 안에서 보기 값을 만들어, 트랜잭션 밖(open-in-view 꺼짐)에서 모든 칸을 읽을 수 있다 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void profileOutsideTransaction() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long[] ids = requireNonNull(tx.execute(status -> {
            PlayerCharacter han = character(1L, "한백", "목 화", Realm.QI_1);
            Technique changchun = persist(changchun());
            Technique ironBody = persist(ironBody());
            learn(han, ironBody);
            learn(han, changchun);
            return new Long[]{han.getId(), changchun.getId(), ironBody.getId()};
        }));
        try {
            CultivationProfile profile = cultivationFinder.profile(ids[0]);

            assertThat(profile.id()).isEqualTo(ids[0]);
            assertThat(profile.name()).isEqualTo("한백");
            assertThat(profile.realm()).isEqualTo(Realm.QI_1);
            assertThat(requireNonNull(profile.spiritRoot()).name()).isEqualTo("목·화 진영근");
            assertThat(profile.stats().qiBlood()).isEqualTo(60);
            assertThat(profile.techniques()).containsExactly(
                    new LearnedTechniqueView(ids[2], "강체결", 1, 9),
                    new LearnedTechniqueView(ids[1], "장춘공", 1, 9));
        } finally {
            tx.executeWithoutResult(status -> {
                entityManager.createQuery("delete from CharacterTechnique").executeUpdate();
                entityManager.remove(entityManager.find(PlayerCharacter.class, ids[0]));
                entityManager.remove(entityManager.find(Technique.class, ids[1]));
                entityManager.remove(entityManager.find(Technique.class, ids[2]));
            });
        }
    }

    @Test
    void summariesByName() {
        PlayerCharacter yeo = character(2L, "여림", null, Realm.MORTAL);
        PlayerCharacter han = character(1L, "한백", "화", Realm.QI_7);
        PlayerCharacter ga = character(3L, "가람", "목 화", Realm.QI_1);
        learn(han, persist(changchun()));
        learn(han, persist(ironBody()));
        flushAndClear();

        assertThat(cultivationFinder.summaries()).containsExactly(
                new CharacterSummary(ga.getId(), "가람", Realm.QI_1, "목·화 진영근", 0),
                new CharacterSummary(yeo.getId(), "여림", Realm.MORTAL, null, 0),
                new CharacterSummary(han.getId(), "한백", Realm.QI_7, "화 천영근", 2));
    }

    @Test
    void findLearnedInOrder() {
        PlayerCharacter han = character(1L, "한백", "목 화", Realm.QI_1);
        learn(han, persist(ironBody()));
        learn(han, persist(changchun()));
        flushAndClear();

        List<CharacterTechnique> learned = cultivationFinder.findLearned(han.getId());

        assertThat(learned).extracting(item -> item.getTechnique().getName()).containsExactly("강체결", "장춘공");
    }

    @Test
    void profileUnknownFail() {
        assertThatThrownBy(() -> cultivationFinder.profile(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> cultivationFinder.stats(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("캐릭터를 찾을 수 없습니다. id: 999");
    }

    private PlayerCharacter character(Long accountId, String name, @Nullable String roots, Realm realm) {
        PlayerCharacter character = roots == null
                ? createCharacter(accountId, name, 10L)
                : createCharacter(accountId, name, 10L, SpiritRoot.parse(roots).orElseThrow());
        character.changeRealm(realm);
        entityManager.persist(character);
        return character;
    }

    private Technique persist(Technique technique) {
        entityManager.persist(technique);
        return technique;
    }

    private void learn(PlayerCharacter character, Technique technique) {
        entityManager.persist(CharacterTechnique.learn(character, technique));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
