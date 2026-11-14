package mudmk3.application.cultivation.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.character.provided.CharacterFinder;
import mudmk3.application.character.provided.RealmChangedEvent;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.cultivation.CharacterTechnique;
import mudmk3.domain.inventory.InventoryEntry;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import mudmk3.domain.technique.Technique;
import mudmk3.domain.technique.TechniqueSaveInfo;
import mudmk3.domain.technique.TechniqueStage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.Objects;

import static mudmk3.domain.character.CharacterFixture.createCharacter;
import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.technique.TechniqueFixture.changchun;
import static mudmk3.domain.technique.TechniqueFixture.fireBall;
import static mudmk3.domain.technique.TechniqueFixture.greenWood;
import static mudmk3.domain.technique.TechniqueFixture.ironBody;
import static mudmk3.domain.technique.TechniqueFixture.stage;
import static org.assertj.core.api.Assertions.assertThat;

@ApplicationServiceTest
@RecordApplicationEvents
record TechniqueReaderTest(TechniqueReader techniqueReader, CultivationFinder cultivationFinder,
                           CharacterFinder characterFinder, ItemTypeEditor itemTypeEditor,
                           InventoryEditor inventoryEditor, InventoryFinder inventoryFinder,
                           EntityManager entityManager) {

    @Test
    void notScroll() {
        Long han = prepareCharacter("목 화", Realm.QI_1);
        ItemType pill = itemTypeEditor.create(createItemTypeSaveRequest("회기단", ItemCategory.PILL, 10));
        inventoryEditor.addUpTo(han, pill.getId(), 1, false);

        assertThat(techniqueReader.read(han, pill.getId())).isEqualTo(new ReadResult.NotScroll());
        assertThat(countOf(han, pill)).isEqualTo(1);
    }

    @Test
    void noTechnique() {
        Long han = prepareCharacter("목 화", Realm.QI_1);
        ItemType blank = prepareScroll("빈 옥간", null, 1, han);

        assertThat(techniqueReader.read(han, blank.getId())).isEqualTo(new ReadResult.NoTechnique());
        assertThat(countOf(han, blank)).isEqualTo(1);
    }

    @Test
    void alreadyLearned() {
        Long han = prepareCharacter("목 화", Realm.QI_1);
        ItemType jade = prepareScroll("장춘공 옥간", persist(changchun()), 2, han);
        techniqueReader.read(han, jade.getId());

        assertThat(techniqueReader.read(han, jade.getId())).isEqualTo(new ReadResult.AlreadyLearned("장춘공", 1, 9));
        assertThat(countOf(han, jade)).isEqualTo(1);
    }

    @Test
    void realmTooLow() {
        Long han = prepareCharacter("목 화", Realm.MORTAL);
        ItemType jade = prepareScroll("청목결 옥간", persist(greenWood()), 1, han);

        assertThat(techniqueReader.read(han, jade.getId())).isEqualTo(new ReadResult.RealmTooLow(Realm.QI_7));
        assertThat(countOf(han, jade)).isEqualTo(1);
        assertThat(cultivationFinder.findLearned(han)).isEmpty();
    }

    @Test
    void missingRoot() {
        Long han = prepareCharacter("화", Realm.QI_1);
        ItemType jade = prepareScroll("장춘공 옥간", persist(changchun()), 1, han);

        assertThat(techniqueReader.read(han, jade.getId())).isEqualTo(new ReadResult.MissingRoot(Element.WOOD));
        assertThat(countOf(han, jade)).isEqualTo(1);
    }

    @Test
    void aptitudeComparesFraction() {
        Long han = prepareCharacter("금 목 화", Realm.QI_1);
        ItemType ok = prepareScroll("목령공 옥간", persist(woodTechnique("목령공", 33)), 1, han);
        ItemType high = prepareScroll("목심공 옥간", persist(woodTechnique("목심공", 34)), 1, han);

        assertThat(techniqueReader.read(han, ok.getId())).isInstanceOf(ReadResult.Learned.class);
        assertThat(techniqueReader.read(han, high.getId()))
                .isEqualTo(new ReadResult.LowAptitude(Element.WOOD, 34, 33));
        assertThat(countOf(han, high)).isEqualTo(1);
    }

    @Test
    void bodyRefiningSkipsRootChecks() {
        Long han = prepareCharacter("화", Realm.MORTAL);
        ItemType jade = prepareScroll("강체결 옥간", persist(ironBody()), 1, han);

        ReadResult result = techniqueReader.read(han, jade.getId());

        assertThat(result).isEqualTo(new ReadResult.Learned("강체결", List.of(), new TechniqueStage(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of("철권"), true));
        assertThat(cultivationFinder.findLearned(han)).extracting(learned -> learned.getTechnique().getName())
                .containsExactly("강체결");
    }

    @Test
    void learnConsumesOneScroll() {
        Long han = prepareCharacter("목 화", Realm.QI_1);
        ItemType jade = prepareScroll("장춘공 옥간", persist(changchun()), 2, han);

        ReadResult result = techniqueReader.read(han, jade.getId());
        flushAndClear();

        assertThat(result).isEqualTo(new ReadResult.Learned("장춘공", List.of(Element.WOOD), changchun().stage(1),
                List.of("화탄술"), false));
        assertThat(countOf(han, jade)).isEqualTo(1);
        assertThat(cultivationFinder.findLearned(han)).extracting(CharacterTechnique::getStage).containsExactly(1);
    }

    @Test
    void mortalInitiates(ApplicationEvents events) {
        Long han = prepareCharacter("목 화", Realm.MORTAL);
        ItemType jade = prepareScroll("장춘공 옥간", persist(changchun()), 1, han);

        ReadResult result = techniqueReader.read(han, jade.getId());
        flushAndClear();

        assertThat(result).isInstanceOfSatisfying(ReadResult.Learned.class,
                learned -> assertThat(learned.initiated()).isTrue());
        assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_1);
        assertThat(countOf(han, jade)).isZero();
        assertThat(events.stream(RealmChangedEvent.class)).isEmpty();
    }

    @Test
    void nonMortalDoesNotInitiate() {
        Long han = prepareCharacter("목 화", Realm.QI_3);
        ItemType jade = prepareScroll("장춘공 옥간", persist(changchun()), 1, han);

        ReadResult result = techniqueReader.read(han, jade.getId());
        flushAndClear();

        assertThat(result).isInstanceOfSatisfying(ReadResult.Learned.class,
                learned -> assertThat(learned.initiated()).isFalse());
        assertThat(characterFinder.find(han).getRealm()).isEqualTo(Realm.QI_3);
    }

    private static TechniqueSaveInfo woodTechnique(String name, int aptitudeRequired) {
        return new TechniqueSaveInfo(name, "", List.of(Element.WOOD), Realm.MORTAL,
                List.of(stage(0, aptitudeRequired, 0, 0, 0, 0)), List.of(fireBall()));
    }

    private Technique persist(TechniqueSaveInfo info) {
        return persist(Technique.create(info));
    }

    private Technique persist(Technique technique) {
        entityManager.persist(technique);
        return technique;
    }

    private Long prepareCharacter(String roots, Realm realm) {
        PlayerCharacter character = createCharacter(1L, "한백", 10L, SpiritRoot.parse(roots).orElseThrow());
        character.changeRealm(realm);
        entityManager.persist(character);
        return Objects.requireNonNull(character.getId());
    }

    private ItemType prepareScroll(String name, @Nullable Technique technique, int count, Long characterId) {
        ItemType scroll = itemTypeEditor.create(new ItemTypeSaveRequest(name, null, ItemCategory.SCROLL, 30, null,
                technique == null ? null : technique.getId()));
        inventoryEditor.addUpTo(characterId, scroll.getId(), count, false);
        flushAndClear();
        return scroll;
    }

    private int countOf(Long characterId, ItemType itemType) {
        return inventoryFinder.find(characterId).getEntries().stream()
                .filter(entry -> Objects.equals(entry.getItemType().getId(), itemType.getId()))
                .mapToInt(InventoryEntry::getCount)
                .sum();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
