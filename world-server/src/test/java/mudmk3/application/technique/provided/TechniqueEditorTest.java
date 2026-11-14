package mudmk3.application.technique.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.DuplicateTechniqueNameException;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.domain.technique.UsableWhen;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static mudmk3.domain.technique.TechniqueFixture.changchunRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record TechniqueEditorTest(TechniqueEditor techniqueEditor, TechniqueFinder techniqueFinder,
                           EntityManager entityManager) {

    @Test
    void create() {
        TechniqueView created = techniqueEditor.create(changchunRequest(" 장춘공 "));
        flushAndClear();

        TechniqueView view = techniqueFinder.view(created.id());
        assertThat(view.name()).isEqualTo("장춘공");
        assertThat(view.description()).isEqualTo("목 속성 입문 공법.");
        assertThat(view.elements()).containsExactly(Element.WOOD);
        assertThat(view.elementNames()).isEqualTo("목");
        assertThat(view.lineName()).isEqualTo("오행");
        assertThat(view.startRealm()).isEqualTo(Realm.MORTAL);
        assertThat(view.startRealmName()).isEqualTo("범인");
        assertThat(view.stageCount()).isEqualTo(9);
        assertThat(view.stages().get(1)).isEqualTo(new TechniqueStage(120, 0, 2, 1, 0, 0, 0, 0, 1));
        assertThat(view.abilityCount()).isEqualTo(3);
        TechniqueView.AbilityView fireBall = view.abilities().getFirst();
        assertThat(fireBall.name()).isEqualTo("화탄술");
        assertThat(fireBall.kindName()).isEqualTo("공격");
        assertThat(fireBall.targetName()).isEqualTo("한 대상");
        assertThat(fireBall.elements()).containsExactly(Element.FIRE);
        assertThat(view.abilities()).extracting(TechniqueView.AbilityView::name)
                .containsExactly("화탄술", "천안술", "어풍결");
        assertThat(view.learnerCount()).isZero();
        assertThat(view.scrollItems()).isEmpty();
        assertThat(created).isEqualTo(view);
    }

    @Test
    void createWithMissingValues() {
        // 빠진 숫자는 0, 빠진 목록은 빈 목록, 빠진 묘사는 빈 글
        TechniqueSaveRequest request = new TechniqueSaveRequest("강체결", null, null, Realm.MORTAL,
                List.of(new StageRequest(null, null, null, null, null, null, null, null, null)),
                List.of(new AbilityRequest("철권", 1, AbilityKind.ATTACK, null, true, AbilityTarget.SINGLE,
                        CostStat.NONE, null, false, null, new BigDecimal("4"), UsableWhen.COMBAT_ONLY)));

        TechniqueView view = techniqueEditor.create(request);

        assertThat(view.description()).isEmpty();
        assertThat(view.elements()).isEmpty();
        assertThat(view.elementNames()).isNull();
        assertThat(view.lineName()).isEqualTo("연체술");
        assertThat(view.stages()).containsExactly(new TechniqueStage(0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(view.abilities().getFirst().costAmount()).isEqualByComparingTo("0");
        assertThat(view.abilities().getFirst().castSeconds()).isEqualByComparingTo("0");
    }

    @Test
    void missingRequiredEnumFail() {
        assertThatThrownBy(() -> new TechniqueSaveRequest("장춘공", null, null, null, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startRealm 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, null, null, false, AbilityTarget.SINGLE,
                CostStat.MANA, null, false, null, null, UsableWhen.COMBAT_ONLY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("kind 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, AbilityKind.ATTACK, null, false, null,
                CostStat.MANA, null, false, null, null, UsableWhen.COMBAT_ONLY))
                .hasMessage("target 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, AbilityKind.ATTACK, null, false,
                AbilityTarget.SINGLE, null, null, false, null, null, UsableWhen.COMBAT_ONLY))
                .hasMessage("costStat 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, AbilityKind.ATTACK, null, false,
                AbilityTarget.SINGLE, CostStat.MANA, null, false, null, null, null))
                .hasMessage("usableWhen 는 꼭 보내야 합니다");
    }

    @Test
    void domainRuleFail() {
        TechniqueSaveRequest request = new TechniqueSaveRequest("장 춘공", null, List.of(Element.WOOD), Realm.MORTAL,
                changchunRequest("장춘공").stages(), changchunRequest("장춘공").abilities());

        assertThatThrownBy(() -> techniqueEditor.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공법 이름은 띄어쓰기 없이 1~12자로 정하십시오.");
    }

    @Test
    void duplicateNameIgnoringCaseFail() {
        techniqueEditor.create(changchunRequest("Spring"));

        assertThatThrownBy(() -> techniqueEditor.create(changchunRequest(" spring ")))
                .isInstanceOf(DuplicateTechniqueNameException.class)
                .hasMessage("이미 있는 공법 이름입니다: spring");
    }

    @Test
    void updateToOtherTechniqueNameFail() {
        techniqueEditor.create(changchunRequest("장춘공"));
        TechniqueView other = techniqueEditor.create(changchunRequest("청목결"));

        assertThatThrownBy(() -> techniqueEditor.update(other.id(), changchunRequest("장춘공")))
                .isInstanceOf(DuplicateTechniqueNameException.class)
                .hasMessage("이미 있는 공법 이름입니다: 장춘공");
    }

    @Test
    void updateKeepsOwnName() {
        TechniqueView created = techniqueEditor.create(changchunRequest("spring"));
        flushAndClear();

        TechniqueSaveRequest request = new TechniqueSaveRequest("Spring", "새 묘사", List.of(Element.WOOD),
                Realm.QI_1, changchunRequest("x").stages().subList(0, 2),
                changchunRequest("x").abilities().subList(0, 2));
        TechniqueView updated = techniqueEditor.update(created.id(), request);
        flushAndClear();

        TechniqueView view = techniqueFinder.view(created.id());
        assertThat(updated.name()).isEqualTo("Spring");
        assertThat(view.name()).isEqualTo("Spring");
        assertThat(view.description()).isEqualTo("새 묘사");
        assertThat(view.startRealm()).isEqualTo(Realm.QI_1);
        assertThat(view.stageCount()).isEqualTo(2);
        assertThat(view.abilities()).extracting(TechniqueView.AbilityView::name).containsExactly("화탄술", "천안술");
    }

    /** 서비스가 트랜잭션 안에서 보기 값을 만들어, 트랜잭션 밖(open-in-view 꺼짐)에서 모든 칸을 읽을 수 있다 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void viewOutsideTransaction() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        try {
            TechniqueView view = techniqueFinder.view(created.id());
            List<TechniqueView> all = techniqueFinder.viewAll();

            assertThat(view.elementNames()).isEqualTo("목");
            assertThat(view.stages()).hasSize(9);
            assertThat(view.abilities()).extracting(TechniqueView.AbilityView::elements)
                    .containsExactly(List.of(Element.FIRE), List.of(Element.WOOD), List.of(Element.WOOD));
            assertThat(all).singleElement().isEqualTo(view);
            assertThat(created).isEqualTo(view);
        } finally {
            techniqueEditor.delete(created.id());
        }
    }

    @Test
    void viewAllByName() {
        techniqueEditor.create(changchunRequest("청목결"));
        techniqueEditor.create(changchunRequest("강체결"));
        techniqueEditor.create(changchunRequest("장춘공"));
        flushAndClear();

        assertThat(techniqueFinder.viewAll()).extracting(TechniqueView::name)
                .containsExactly("강체결", "장춘공", "청목결");
    }

    @Test
    void delete() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        flushAndClear();

        techniqueEditor.delete(created.id());
        flushAndClear();

        assertThatThrownBy(() -> techniqueFinder.find(created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공법을 찾을 수 없습니다. id: " + created.id());
    }

    @Test
    void unknownIdFail() {
        assertThatThrownBy(() -> techniqueFinder.find(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공법을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> techniqueFinder.view(999L)).hasMessage("공법을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> techniqueEditor.update(999L, changchunRequest("장춘공")))
                .hasMessage("공법을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> techniqueEditor.delete(999L)).hasMessage("공법을 찾을 수 없습니다. id: 999");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
