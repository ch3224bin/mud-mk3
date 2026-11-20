package mudmk3.application.mob.provided;

import jakarta.persistence.EntityManager;
import mudmk3.ApplicationServiceTest;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.room.provided.RoomEditor;
import mudmk3.application.zone.provided.ZoneEditor;
import mudmk3.domain.item.ItemCategory;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.mob.DropTier;
import mudmk3.domain.mob.DuplicateMobNameException;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobFixture;
import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobStats;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.room.Room;
import mudmk3.domain.shared.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static mudmk3.domain.item.ItemTypeFixture.createItemTypeSaveRequest;
import static mudmk3.domain.mob.MobFixture.boss;
import static mudmk3.domain.mob.MobFixture.named;
import static mudmk3.domain.mob.MobFixture.rabbit;
import static mudmk3.domain.mob.MobFixture.request;
import static mudmk3.domain.mob.MobFixture.withDrops;
import static mudmk3.domain.mob.MobFixture.wolf;
import static mudmk3.domain.room.RoomFixture.createRoomSaveRequest;
import static mudmk3.domain.zone.ZoneFixture.createZoneSaveRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@ApplicationServiceTest
record MobTypeEditorTest(MobTypeEditor mobTypeEditor, MobTypeFinder mobTypeFinder, ItemTypeEditor itemTypeEditor,
                         MobPlacementEditor mobPlacementEditor, MobPlacementFinder mobPlacementFinder,
                         ZoneEditor zoneEditor, RoomEditor roomEditor, EntityManager entityManager) {

    @Test
    void create() {
        ItemType hide = item("토끼 가죽");
        ItemType meat = item("토끼 고기");
        ItemType pill = item("회춘단");

        MobTypeView created = mobTypeEditor.create(request(withDrops(named(rabbit(), " 들토끼 "),
                List.of(new MobFixedDrop(hide.getId(), 1), new MobFixedDrop(meat.getId(), 2)),
                List.of(new MobTierDrop(DropTier.ADVANCED, pill.getId())))));
        flushAndClear();

        MobTypeView view = mobTypeFinder.view(created.id());
        assertThat(view.name()).isEqualTo("들토끼");
        assertThat(view.description()).isEqualTo("들토끼의 묘사다.");
        assertThat(view.kind()).isEqualTo(MobKind.BEAST);
        assertThat(view.kindName()).isEqualTo("요수");
        assertThat(view.realm()).isEqualTo(Realm.QI_1);
        assertThat(view.realmName()).isEqualTo("연기 1층");
        assertThat(view.tier()).isEqualTo(MobTier.NORMAL);
        assertThat(view.tierName()).isEqualTo("일반");
        assertThat(view.rankName()).isEqualTo("1급");
        assertThat(view.attackName()).isEqualTo("뒷발차기");
        assertThat(view.attackCoefficient()).isNull();
        assertThat(view.respawnSeconds()).isNull();
        assertThat(view.effectiveRespawnSeconds()).isEqualTo(90);
        assertThat(view.stats()).isEqualTo(new MobStats(42, 7, 7));
        assertThat(view.fixedDrops()).containsExactly(
                new MobTypeView.FixedDropView(hide.getId(), "토끼 가죽", 1),
                new MobTypeView.FixedDropView(meat.getId(), "토끼 고기", 2));
        assertThat(view.tierDrops()).containsExactly(
                new MobTypeView.TierDropView(DropTier.ADVANCED, "고급", pill.getId(), "회춘단"));
        assertThat(view.placements()).isEmpty();
        assertThat(view.placementCount()).isZero();
        assertThat(created).isEqualTo(view);
    }

    @Test
    void createWithMissingValues() {
        // 빠진 묘사는 빈 글, 빠진 목록은 빈 목록
        MobTypeView view = mobTypeEditor.create(new MobTypeSaveRequest("청모랑", null, MobKind.BEAST, Realm.QI_2,
                MobTier.NORMAL, "물어뜯기", null, null, null, null));

        assertThat(view.description()).isEmpty();
        assertThat(view.fixedDrops()).isEmpty();
        assertThat(view.tierDrops()).isEmpty();
    }

    @Test
    void createBoss() {
        MobTypeView view = mobTypeEditor.create(request(boss()));

        assertThat(view.rankName()).isEqualTo("2급");
        assertThat(view.tierName()).isEqualTo("두목");
        assertThat(view.attackCoefficient()).isEqualByComparingTo("0.6");
        assertThat(view.effectiveRespawnSeconds()).isEqualTo(2_700);
        assertThat(view.stats()).isEqualTo(new MobStats(1_700, 24, 19));
    }

    @Test
    void duplicateNameIgnoringCaseFail() {
        mobTypeEditor.create(request(named(wolf(), "Wolf")));

        assertThatThrownBy(() -> mobTypeEditor.create(request(named(wolf(), " wolf "))))
                .isInstanceOf(DuplicateMobNameException.class)
                .hasMessage("이미 있는 몹 이름입니다: wolf");
    }

    @Test
    void updateToOtherMobNameFail() {
        mobTypeEditor.create(request(wolf()));
        MobTypeView other = mobTypeEditor.create(request(rabbit()));

        assertThatThrownBy(() -> mobTypeEditor.update(other.id(), request(wolf())))
                .isInstanceOf(DuplicateMobNameException.class)
                .hasMessage("이미 있는 몹 이름입니다: 청모랑");
    }

    @Test
    void update() {
        ItemType hide = item("청모랑 가죽");
        ItemType tooth = item("늑대 이빨");
        MobTypeView created = mobTypeEditor.create(request(withDrops(named(wolf(), "wolf"),
                List.of(new MobFixedDrop(hide.getId(), 1)), List.of(new MobTierDrop(DropTier.RARE, hide.getId())))));
        flushAndClear();

        MobTypeView updated = mobTypeEditor.update(created.id(), request(withDrops(
                MobFixture.info("Wolf", MobKind.BEAST, Realm.QI_9, MobTier.STRONG, "물기", null, 30),
                List.of(new MobFixedDrop(tooth.getId(), 2), new MobFixedDrop(hide.getId(), 1)), List.of())));
        flushAndClear();

        MobTypeView view = mobTypeFinder.view(created.id());
        assertThat(updated).isEqualTo(view);
        assertThat(view.name()).isEqualTo("Wolf");
        assertThat(view.rankName()).isEqualTo("2급");
        assertThat(view.effectiveRespawnSeconds()).isEqualTo(30);
        assertThat(view.fixedDrops()).extracting(MobTypeView.FixedDropView::itemTypeName)
                .containsExactly("늑대 이빨", "청모랑 가죽");
        assertThat(view.tierDrops()).isEmpty();
    }

    @Test
    void unknownItemFail() {
        ItemType hide = item("토끼 가죽");

        assertThatThrownBy(() -> mobTypeEditor.create(request(withDrops(rabbit(),
                List.of(new MobFixedDrop(hide.getId(), 1)), List.of(new MobTierDrop(DropTier.RARE, 9L))))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 9");
    }

    /** 없는 물건 종류(400)가 이름 중복(409)과 도메인 규칙보다 먼저다 */
    @Test
    void unknownItemBeforeOtherChecksFail() {
        mobTypeEditor.create(request(wolf()));

        assertThatThrownBy(() -> mobTypeEditor.create(request(withDrops(wolf(),
                List.of(new MobFixedDrop(9L, 0)), List.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("물건 종류를 찾을 수 없습니다. id: 9");
        // 이름 중복이 도메인 규칙보다 먼저다
        assertThatThrownBy(() -> mobTypeEditor.create(request(MobFixture.info("청모랑", MobKind.BEAST, Realm.MORTAL,
                MobTier.NORMAL, "물기", null, null))))
                .isInstanceOf(DuplicateMobNameException.class);
    }

    @Test
    void domainRuleFail() {
        assertThatThrownBy(() -> mobTypeEditor.create(request(named(wolf(), "2.늑대"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹 이름은 '2.' 처럼 수와 점으로 시작할 수 없습니다.");
    }

    @Test
    void requiredEnumNullFail() {
        assertThatThrownBy(() -> new MobTypeSaveRequest("청모랑", null, null, Realm.QI_2, MobTier.NORMAL, "물기",
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("kind 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new MobTypeSaveRequest("청모랑", null, MobKind.BEAST, null, MobTier.NORMAL, "물기",
                null, null, null, null))
                .hasMessage("realm 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new MobTypeSaveRequest("청모랑", null, MobKind.BEAST, Realm.QI_2, null, "물기",
                null, null, null, null))
                .hasMessage("tier 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new MobTierDropRequest(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tier 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new MobFixedDropRequest(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("itemTypeId 는 꼭 보내야 합니다");
        assertThatThrownBy(() -> new MobTypeSaveRequest("청모랑", null, MobKind.BEAST, Realm.QI_2, MobTier.NORMAL,
                "물기", null, null, java.util.Collections.singletonList(null), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fixedDrops 에 빈 값이 있습니다");
    }

    /** 서비스가 트랜잭션 안에서 보기 값을 만들어, 트랜잭션 밖(open-in-view 꺼짐)에서 모든 칸을 읽을 수 있다 */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void viewOutsideTransaction() {
        ItemType hide = itemTypeEditor.create(createItemTypeSaveRequest("밖 토끼 가죽", ItemCategory.MATERIAL, 1));
        MobTypeView created = mobTypeEditor.create(request(withDrops(rabbit(),
                List.of(new MobFixedDrop(hide.getId(), 1)), List.of(new MobTierDrop(DropTier.RARE, hide.getId())))));
        try {
            MobTypeView view = mobTypeFinder.view(created.id());
            List<MobTypeView> all = mobTypeFinder.viewAll();

            assertThat(view.fixedDrops()).extracting(MobTypeView.FixedDropView::itemTypeName)
                    .containsExactly("밖 토끼 가죽");
            assertThat(view.tierDrops()).extracting(MobTypeView.TierDropView::tierName).containsExactly("희귀");
            assertThat(all).singleElement().isEqualTo(view);
            assertThat(created).isEqualTo(view);
        } finally {
            mobTypeEditor.delete(created.id());
            itemTypeEditor.delete(hide.getId());
        }
    }

    @Test
    void viewAllByName() {
        mobTypeEditor.create(request(wolf()));
        mobTypeEditor.create(request(boss()));
        mobTypeEditor.create(request(rabbit()));
        flushAndClear();

        assertThat(mobTypeFinder.viewAll()).extracting(MobTypeView::name)
                .containsExactly("들토끼", "쌍두 흑망", "청모랑");
    }

    @Test
    void delete() {
        MobTypeView created = mobTypeEditor.create(request(wolf()));
        flushAndClear();

        DeletedMobType deleted = mobTypeEditor.delete(created.id());
        flushAndClear();

        assertThat(deleted).isEqualTo(new DeletedMobType(created.id(), "청모랑", 0));
        assertThatThrownBy(() -> mobTypeFinder.find(created.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹을 찾을 수 없습니다. id: " + created.id());
    }

    /** 배치된 몹은 배치를 먼저 모두 지우고 지운다(드롭 표도 함께) */
    @Test
    void deletePlaced() {
        Room room = prepareRoom("샘터");
        ItemType hide = item("청모랑 가죽");
        MobTypeView wolf = mobTypeEditor.create(request(withDrops(wolf(),
                List.of(new MobFixedDrop(hide.getId(), 1)), List.of(new MobTierDrop(DropTier.RARE, hide.getId())))));
        mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 2));
        mobPlacementEditor.create(room.getId(), new MobPlacementSaveRequest(wolf.id(), 1));
        flushAndClear();

        DeletedMobType deleted = mobTypeEditor.delete(wolf.id());
        flushAndClear();

        assertThat(deleted).isEqualTo(new DeletedMobType(wolf.id(), "청모랑", 2));
        assertThatThrownBy(() -> mobTypeFinder.find(wolf.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹을 찾을 수 없습니다. id: " + wolf.id());
        assertThat(mobPlacementFinder.findAllInRoom(room.getId())).isEmpty();
    }

    @Test
    void viewListsPlacements() {
        Room spring = prepareRoom("샘터");
        Room path = prepareRoom("오솔길");
        MobTypeView wolf = mobTypeEditor.create(request(wolf()));
        MobTypeView rabbit = mobTypeEditor.create(request(rabbit()));
        MobPlacementView first = mobPlacementEditor.create(path.getId(), new MobPlacementSaveRequest(wolf.id(), 3));
        MobPlacementView second = mobPlacementEditor.create(spring.getId(), new MobPlacementSaveRequest(wolf.id(), 1));
        mobPlacementEditor.create(spring.getId(), new MobPlacementSaveRequest(rabbit.id(), 2));
        flushAndClear();

        // 배치 id 순, 방 이름까지
        MobTypeView view = mobTypeFinder.view(wolf.id());
        assertThat(view.placements()).containsExactly(
                new MobTypeView.PlacementView(first.id(), path.getId(), "오솔길", 3),
                new MobTypeView.PlacementView(second.id(), spring.getId(), "샘터", 1));
        assertThat(view.placementCount()).isEqualTo(2);
        assertThat(mobTypeFinder.viewAll()).extracting(MobTypeView::name, MobTypeView::placementCount)
                .containsExactly(tuple("들토끼", 1), tuple("청모랑", 2));
        assertThat(mobPlacementEditor.update(second.id(), new MobPlacementSaveRequest(rabbit.id(), 1)).mobName())
                .isEqualTo("들토끼");
        flushAndClear();
        assertThat(mobTypeFinder.view(wolf.id()).placements()).extracting(MobTypeView.PlacementView::roomName)
                .containsExactly("오솔길");
    }

    @Test
    void unknownIdFail() {
        assertThatThrownBy(() -> mobTypeFinder.find(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("몹을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> mobTypeFinder.view(999L)).hasMessage("몹을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> mobTypeEditor.update(999L, request(wolf())))
                .hasMessage("몹을 찾을 수 없습니다. id: 999");
        assertThatThrownBy(() -> mobTypeEditor.delete(999L)).hasMessage("몹을 찾을 수 없습니다. id: 999");
    }

    @Test
    void coefficientStoredAtOneDecimal() {
        MobTypeView view = mobTypeEditor.create(request(MobFixture.info("쌍두 흑망", MobKind.BEAST, Realm.QI_12,
                MobTier.BOSS, "두 머리 물기", new BigDecimal("0.60"), 2_700)));
        flushAndClear();

        assertThat(mobTypeFinder.view(view.id()).attackCoefficient()).isEqualTo(new BigDecimal("0.6"));
    }

    private Room prepareRoom(String name) {
        Long zoneId = zoneEditor.create(createZoneSaveRequest(name + " 지역")).getId();
        return roomEditor.create(createRoomSaveRequest(zoneId, name));
    }

    private ItemType item(String name) {
        return itemTypeEditor.create(createItemTypeSaveRequest(name, ItemCategory.MATERIAL, 1));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
