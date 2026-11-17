package mudmk3.application.technique;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import mudmk3.ApplicationServiceTest;
import mudmk3.QueryCounter;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.application.technique.provided.ContainedTechniqueView;
import mudmk3.application.technique.provided.TechniqueEditor;
import mudmk3.application.technique.provided.TechniqueFinder;
import mudmk3.application.technique.provided.TechniqueView;
import mudmk3.application.technique.provided.TechniqueView.AbilityView;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.technique.TechniqueFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static mudmk3.application.technique.provided.TechniqueRequestFixture.changchunRequest;
import static mudmk3.application.technique.provided.TechniqueRequestFixture.requestOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공법 조회가 내는 쿼리 수. 공법은 속성과 함께 한 번 읽고, 단계·신통과 신통의 값 컬렉션은 묶음(batch)으로 읽는다.
 * 익힌 수·담긴 물건은 공법 수와 상관없이 한 번씩 읽는다
 */
@ApplicationServiceTest
class TechniqueQueryCountTest {

    /** 공법·속성 1, 단계 1, 신통 1, 신통 속성·이펙트 문장·단계별 값 3, 익힌 수 1, 담긴 물건 1 */
    private static final long VIEW_QUERIES = 8;

    /** 공법·속성 1, 단계 1 */
    private static final long VIEW_CONTAINED_QUERIES = 2;

    private final TechniqueEditor techniqueEditor;
    private final TechniqueFinder techniqueFinder;
    private final ItemTypeEditor itemTypeEditor;
    private final EntityManager entityManager;
    private final QueryCounter queryCounter;

    TechniqueQueryCountTest(TechniqueEditor techniqueEditor, TechniqueFinder techniqueFinder,
                            ItemTypeEditor itemTypeEditor, EntityManager entityManager,
                            EntityManagerFactory entityManagerFactory) {
        this.techniqueEditor = techniqueEditor;
        this.techniqueFinder = techniqueFinder;
        this.itemTypeEditor = itemTypeEditor;
        this.entityManager = entityManager;
        this.queryCounter = new QueryCounter(entityManagerFactory);
    }

    @Test
    void techniqueViewAllQueryCountConstant() {
        createWithScroll("가공");
        createWithScroll("나공");
        createWithScroll("다공");
        long threeTechniques = countViewAll();

        createWithScroll("라공");
        createWithScroll("마공");
        long fiveTechniques = countViewAll();

        assertThat(fiveTechniques).isEqualTo(threeTechniques).isEqualTo(VIEW_QUERIES);
        flushAndClear();
        List<TechniqueView> views = techniqueFinder.viewAll();
        assertThat(views).extracting(TechniqueView::name).containsExactly("가공", "나공", "다공", "라공", "마공");
        assertThat(views).allSatisfy(view -> {
            assertThat(view.stageCount()).isEqualTo(9);
            assertThat(view.abilities()).extracting(AbilityView::name).containsExactly("화탄술", "천안술", "어풍결");
            assertThat(view.scrollItems()).singleElement()
                    .satisfies(item -> assertThat(item.name()).isEqualTo(view.name() + " 옥간"));
            assertThat(view.learnerCount()).isZero();
        });
    }

    @Test
    void viewDoesNotMultiplyRows() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        techniqueEditor.create(requestOf(TechniqueFixture.ironBodyInfo()));
        flushAndClear();

        queryCounter.reset();
        TechniqueView view = techniqueFinder.view(created.id());

        assertThat(queryCounter.prepared()).isLessThanOrEqualTo(VIEW_QUERIES);
        assertThat(view.elements()).hasSize(1);
        assertThat(view.stages()).hasSize(9);
        assertThat(view.stages()).containsExactlyElementsOf(TechniqueFixture.changchunStages());
        assertThat(view.abilities()).extracting(AbilityView::name).containsExactly("화탄술", "천안술", "어풍결");
        AbilityView fireBall = view.abilities().getFirst();
        assertThat(fireBall.elements()).hasSize(1);
        assertThat(fireBall.effectLines()).containsExactlyElementsOf(TechniqueFixture.FIRE_BALL_LINES);
        assertThat(fireBall.stagePowers()).hasSize(9);
    }

    @Test
    void viewContained() {
        TechniqueView created = techniqueEditor.create(changchunRequest("장춘공"));
        flushAndClear();

        queryCounter.reset();
        ContainedTechniqueView contained = techniqueFinder.viewContained(created.id());

        assertThat(queryCounter.prepared()).isEqualTo(VIEW_CONTAINED_QUERIES);
        assertThat(contained).isEqualTo(new ContainedTechniqueView("장춘공", "목", "오행", 9, "범인"));
    }

    private void createWithScroll(String name) {
        TechniqueView technique = techniqueEditor.create(changchunRequest(name));
        itemTypeEditor.create(new ItemTypeSaveRequest(name + " 옥간", "", ItemCategory.SCROLL, 10, null,
                technique.id()));
    }

    private long countViewAll() {
        flushAndClear();
        queryCounter.reset();
        techniqueFinder.viewAll();
        return queryCounter.prepared();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
