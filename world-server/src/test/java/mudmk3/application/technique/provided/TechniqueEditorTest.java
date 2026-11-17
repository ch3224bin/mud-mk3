package mudmk3.application.technique.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.technique.provided.ScrollItem;
import mudmk3.domain.character.CharacterFixture;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.AbilityKind;
import mudmk3.domain.technique.AbilityTarget;
import mudmk3.domain.technique.CostStat;
import mudmk3.domain.technique.DuplicateTechniqueNameException;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueFixture;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.domain.technique.TechniqueStage;
import mudmk3.domain.technique.UsableWhen;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mudmk3.application.technique.provided.TechniqueRequestFixture.changchunRequest;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.withEffectLines;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.withStagePowers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationServiceTest
record TechniqueEditorTest(TechniqueEditor techniqueEditor, TechniqueFinder techniqueFinder,
                           ItemTypeEditor itemTypeEditor, EntityManager entityManager) {

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
                        CostStat.NONE, null, false, null, new BigDecimal("4"), UsableWhen.COMBAT_ONLY,
                        List.of("주먹을 내지른다."),
                        List.of(new StagePowerRequest(1, new BigDecimal("5"), new BigDecimal("2.2"))))));

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
                CostStat.MANA, null, false, null, null, UsableWhen.COMBAT_ONLY, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("kind 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, AbilityKind.ATTACK, null, false, null,
                CostStat.MANA, null, false, null, null, UsableWhen.COMBAT_ONLY, null, null))
                .hasMessage("target 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, AbilityKind.ATTACK, null, false,
                AbilityTarget.SINGLE, null, null, false, null, null, UsableWhen.COMBAT_ONLY, null, null))
                .hasMessage("costStat 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new AbilityRequest("화탄술", 1, AbilityKind.ATTACK, null, false,
                AbilityTarget.SINGLE, CostStat.MANA, null, false, null, null, null, null, null))
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

        TechniqueSaveRequest twoStages = changchunWithStageCount(2);
        TechniqueSaveRequest request = new TechniqueSaveRequest("Spring", "새 묘사", List.of(Element.WOOD),
                Realm.QI_1, twoStages.stages(), twoStages.abilities());
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

    @Test
    void deleteWithScrollItemFail() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        scroll("장춘공 옥간", created.id());
        scroll("장춘공 서책", created.id());
        flushAndClear();

        assertThatThrownBy(() -> techniqueEditor.delete(created.id()))
                .isInstanceOf(TechniqueInUseException.class)
                .hasMessage("이 공법이 담긴 물건이 있어 지울 수 없습니다: 장춘공 서책");
    }

    @Test
    void viewListsScrollItems() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        ItemType jade = scroll("장춘공 옥간", created.id());
        ItemType book = scroll("장춘공 서책", created.id());
        TechniqueView other = techniqueEditor.create(changchunRequest("청목결"));
        scroll("청목결 옥간", other.id());
        flushAndClear();

        assertThat(techniqueFinder.view(created.id()).scrollItems()).containsExactly(
                new ScrollItem(book.getId(), "장춘공 서책"), new ScrollItem(jade.getId(), "장춘공 옥간"));
        assertThat(techniqueFinder.viewAll()).extracting(view -> view.scrollItems().size()).containsExactly(2, 1);
    }

    @Test
    void shrinkBelowLearnedStageFail() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        learn(created.id(), "한백", 3);
        flushAndClear();

        assertThatThrownBy(() -> techniqueEditor.update(created.id(), changchunWithStageCount(2)))
                .isInstanceOf(TechniqueInUseException.class)
                .hasMessage("이 공법을 더 높은 단계까지 익힌 캐릭터가 있어 단계 수를 줄일 수 없습니다.");
        flushAndClear();
        assertThat(techniqueFinder.view(created.id()).stageCount()).isEqualTo(9);
    }

    @Test
    void shrinkAboveLearnedStageOk() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        learn(created.id(), "한백", 3);
        learn(created.id(), "여림", 1);
        flushAndClear();

        TechniqueView updated = techniqueEditor.update(created.id(), changchunWithStageCount(3));

        assertThat(updated.stageCount()).isEqualTo(3);
    }

    @Test
    void deleteLearnedFail() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        learn(created.id(), "한백", 1);
        // 익힌 캐릭터 검사가 담긴 물건 검사보다 먼저다
        scroll("장춘공 옥간", created.id());
        flushAndClear();

        assertThatThrownBy(() -> techniqueEditor.delete(created.id()))
                .isInstanceOf(TechniqueInUseException.class)
                .hasMessage("익힌 캐릭터가 있는 공법은 지울 수 없습니다: 장춘공");
    }

    @Test
    void learnerCountInView() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        TechniqueView other = techniqueEditor.create(changchunRequest("청목결"));
        learn(created.id(), "한백", 1);
        learn(created.id(), "여림", 1);
        flushAndClear();

        assertThat(techniqueFinder.view(created.id()).learnerCount()).isEqualTo(2);
        assertThat(techniqueFinder.view(other.id()).learnerCount()).isZero();
        assertThat(techniqueFinder.viewAll()).extracting(TechniqueView::learnerCount).containsExactly(2L, 0L);
    }

    // --- 6차: 이펙트 문장·단계별 값 ---

    /** 트랜잭션 밖에서도 이펙트 문장·단계별 값·usableInGame 을 읽을 수 있다 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void viewHasEffectLinesStagePowersAndUsable() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        try {
            TechniqueView view = techniqueFinder.view(created.id());

            TechniqueView.AbilityView fireBall = view.abilities().getFirst();
            assertThat(fireBall.effectLines()).containsExactlyElementsOf(TechniqueFixture.FIRE_BALL_LINES);
            assertThat(fireBall.stagePowers()).hasSize(9);
            TechniqueView.StagePowerView first = fireBall.stagePowers().getFirst();
            assertThat(first.stage()).isEqualTo(1);
            assertThat(first.basePower()).hasToString("2.00");
            assertThat(first.coefficient()).hasToString("1.200");
            assertThat(fireBall.stagePowers().getLast().basePower()).hasToString("3.60");
            assertThat(fireBall.usableInGame()).isTrue();
            TechniqueView.AbilityView heavenEye = view.abilities().get(1);
            assertThat(heavenEye.effectLines()).containsExactly("천안술 기운이 퍼진다.");
            assertThat(heavenEye.stagePowers()).isEmpty();
            assertThat(heavenEye.usableInGame()).isFalse();
            assertThat(techniqueFinder.viewAll()).singleElement().isEqualTo(view);
        } finally {
            techniqueEditor.delete(created.id());
        }
    }

    @Test
    void updateReplacesEffectLines() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        flushAndClear();

        TechniqueSaveRequest original = changchunRequest("장춘공");
        List<AbilityRequest> abilities = original.abilities().stream()
                .map(ability -> ability.name().equals("화탄술") ? withEffectLines(ability, List.of("  새 불꽃이 튄다!  ")) : ability)
                .toList();
        techniqueEditor.update(created.id(), new TechniqueSaveRequest(original.name(), original.description(),
                original.elements(), original.startRealm(), original.stages(), abilities));
        flushAndClear();

        TechniqueView view = techniqueFinder.view(created.id());
        assertThat(view.abilities().getFirst().effectLines()).containsExactly("새 불꽃이 튄다!");
        assertThat(view.abilities().getFirst().stagePowers()).hasSize(9);
        assertThat(entityManager.createNativeQuery("select count(*) from ability_effect_line").getSingleResult())
                .isEqualTo(3L);
    }

    @Test
    void effectLinesCountFail() {
        AbilityRequest fireBall = changchunRequest("장춘공").abilities().getFirst();

        for (List<String> lines : Arrays.asList(null, List.<String>of(),
                List.of("가", "나", "다", "라", "마", "바"))) {
            TechniqueSaveRequest request = changchunRequestWith(withEffectLines(fireBall, lines));
            assertThatThrownBy(() -> techniqueEditor.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이펙트 문장은 1~5개로 정하십시오: 화탄술");
        }
    }

    @Test
    void missingStagePowersFail() {
        AbilityRequest fireBall = changchunRequest("장춘공").abilities().getFirst();
        AbilityRequest noPowers = withStagePowers(fireBall, null);

        assertThatThrownBy(() -> techniqueEditor.create(changchunRequestWith(noPowers)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("단계별 값은 배우는 단계부터 마지막 단계까지 하나씩 정하십시오: 화탄술");
    }

    @Test
    void stagePowerRequestNullFail() {
        assertThatThrownBy(() -> new StagePowerRequest(null, new BigDecimal("2"), new BigDecimal("1.2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stage 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new StagePowerRequest(1, null, new BigDecimal("1.2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("basePower 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new StagePowerRequest(1, new BigDecimal("2"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("coefficient 는 꼭 보내야 합니다");
    }

    @Test
    void nullInListsFail() {
        AbilityRequest fireBall = changchunRequest("장춘공").abilities().getFirst();
        List<String> lines = new ArrayList<>();
        lines.add(null);

        assertThatThrownBy(() -> withEffectLines(fireBall, lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("effectLines 에 빈 값이 있습니다");
    }

    private static TechniqueSaveRequest changchunRequestWith(AbilityRequest fireBall) {
        TechniqueSaveRequest original = changchunRequest("장춘공");
        return new TechniqueSaveRequest(original.name(), original.description(), original.elements(),
                original.startRealm(), original.stages(), List.of(fireBall));
    }

    /** 공법 정의 쪽 테스트라 익힌 공법은 캐릭터·익힌 행을 직접 넣어 만든다 */
    private void learn(Long techniqueId, String characterName, int stage) {
        PlayerCharacter character = CharacterFixture.createCharacter(
                (long) characterName.hashCode(), characterName, 10L, CharacterFixture.spiritRoot());
        entityManager.persist(character);
        CharacterTechnique learned = CharacterTechnique.learn(character,
                entityManager.find(Technique.class, techniqueId));
        ReflectionTestUtils.setField(learned, "stage", stage);
        entityManager.persist(learned);
    }

    /** 앞 {@code count} 단계와 앞 {@code count} 신통(단계별 값은 그 단계까지)만 둔 장춘공 요청 */
    private static TechniqueSaveRequest changchunWithStageCount(int count) {
        TechniqueSaveInfo info = TechniqueFixture.changchunInfo("장춘공");
        return TechniqueRequestFixture.requestOf(new TechniqueSaveInfo(info.name(), info.description(),
                info.elements(), info.startRealm(), info.stages().subList(0, count),
                info.abilities().subList(0, count).stream()
                        .map(ability -> TechniqueFixture.upToStage(ability, count)).toList()));
    }

    private ItemType scroll(String name, Long techniqueId) {
        return itemTypeEditor.create(new ItemTypeSaveRequest(name, null, ItemCategory.SCROLL, 30, null, techniqueId));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
