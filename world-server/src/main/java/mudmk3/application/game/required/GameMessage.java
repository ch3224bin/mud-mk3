package mudmk3.application.game.required;

import mudmk3.domain.character.PlayerCharacter;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.combat.Damage;
import mudmk3.domain.cultivation.Insight;
import mudmk3.domain.cultivation.InsightKind;
import mudmk3.domain.cultivation.StatSheet;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

/** 서버가 플레이어에게 보내는 메시지. record 필드 이름이 JSON 필드 이름이고 {@link #type()} 이 JSON {@code type} 이다 */
public sealed interface GameMessage {

    String type();

    /** 접속 상태가 바뀜. state: {@code creating} | {@code playing} */
    record SessionMessage(String state, @Nullable String characterName) implements GameMessage {
        public static final String CREATING = "creating";
        public static final String PLAYING = "playing";

        @Override
        public String type() {
            return "session";
        }
    }

    /** 캐릭터 생성 중 묻는 말 */
    record PromptMessage(String text) implements GameMessage {
        @Override
        public String type() {
            return "prompt";
        }
    }

    /**
     * 방 보기. owner는 지역을 소유한 성·종문(없으면 null), exits는 방위 한글 이름(방위 순서),
     * occupants는 나를 뺀 사람(들어온 순서) 다음 그 방의 NPC(만든 순서), mobs는 살아 있는 몹(같은 유형·같은 교전 상태를 묶은 한 줄씩,
     * 묶음이 처음 나온 순서), corpses는 시체(쓰러진 순), items는 바닥 물건(같은 종류를 묶은 한 줄씩, D40 순서).
     * quiet 는 부채 정리 2차의 조용한 방 상태(화면에 그리지 않고 자동완성 후보만 바꿈, zoneEntered 는 늘 false)면 true, 방 보기면 false
     */
    record RoomMessage(
            ZoneView zone,
            boolean zoneEntered,
            RoomView room,
            @Nullable OwnerView owner,
            List<String> exits,
            List<OccupantView> occupants,
            List<MobView> mobs,
            List<CorpseView> corpses,
            List<FloorItemView> items,
            boolean quiet
    ) implements GameMessage {
        public RoomMessage {
            exits = List.copyOf(exits);
            occupants = List.copyOf(occupants);
            mobs = List.copyOf(mobs);
            corpses = List.copyOf(corpses);
            items = List.copyOf(items);
        }

        @Override
        public String type() {
            return "room";
        }

        public record ZoneView(Long id, String name) {
        }

        public record RoomView(Long id, String name, String description) {
        }

        /** kind: {@code castle} | {@code sect} */
        public record OwnerView(String kind, String name) {
        }

        /** kind: {@code player} | {@code npc} */
        public record OccupantView(String name, String kind) {
            public static OccupantView player(String name) {
                return new OccupantView(name, "player");
            }

            public static OccupantView npc(String name) {
                return new OccupantView(name, "npc");
            }
        }

        /**
         * 몹 한 줄. rank 는 {@code 1급}·{@code 연기 10층}·{@code 헤아릴 수 없음}, tier 는 체급 표시(일반이면 null, {@code 강적}·{@code 정예}·{@code 두목}),
         * count 는 묶인 수, fighting 은 교전 중인지
         */
        public record MobView(String name, String rank, @Nullable String tier, int count, boolean fighting) {
        }

        /** 시체 한 줄. collectible 은 보는 사람이 지금 수습할 수 있는지 */
        public record CorpseView(String name, boolean collectible) {
        }

        /** 바닥 물건 한 줄: 이름과 합친 수 */
        public record FloorItemView(String name, int count) {
        }
    }

    /**
     * 소지품(묻는 사람에게만). stones는 가진 영석 등급만(하품부터), items는 영석 밖 묶음(표시 순서),
     * text는 그린 줄들({@code \n} 으로 나뉨)
     */
    record InventoryMessage(
            int usedSlots,
            int totalSlots,
            List<StoneView> stones,
            List<ItemView> items,
            String text,
            String tone
    ) implements GameMessage {
        public InventoryMessage {
            stones = List.copyOf(stones);
            items = List.copyOf(items);
        }

        public static InventoryMessage of(int usedSlots, int totalSlots, List<StoneView> stones, List<ItemView> items, String text) {
            return new InventoryMessage(usedSlots, totalSlots, stones, items, requireNonNull(text), "item");
        }

        @Override
        public String type() {
            return "inventory";
        }

        public record StoneView(String name, long count) {
        }

        public record ItemView(String name, int count) {
        }
    }

    /**
     * 상태(묻는 사람에게만). realm·spiritRoot·stats 는 구조, cultivation 은 수위(범인이면 null), text 는 그린 줄들
     * ({@code \n} 으로 나뉨), insight 는 깨달음 여섯 갈래(금·목·수·화·토·연체 순, 소수점을 버린 양).
     * 기혈·법력 현재 값은 깎인 양을 뺀 값을 {@link Damage#gauge} 로 보이고, 나머지는 현재 수치 = 최대치다
     */
    record StatusMessage(
            String characterName,
            RealmView realm,
            SpiritRootView spiritRoot,
            StatsView stats,
            @Nullable CultivationView cultivation,
            List<InsightView> insight,
            String text,
            String tone
    ) implements GameMessage {

        /** 영근을 정한 캐릭터의 상태. 수위는 4차에 쌓는 길이 없어 늘 0 이다 */
        public static StatusMessage of(PlayerCharacter character, StatSheet stats, Insight insight, String text) {
            Realm realm = character.getRealm();
            SpiritRoot root = character.findSpiritRoot()
                    .orElseThrow(() -> new IllegalStateException("영근을 아직 정하지 않은 캐릭터입니다. id: " + character.getId()));
            List<RootView> roots = root.entries().stream()
                    .map(entry -> new RootView(entry.element().name(), entry.element().koreanName(),
                            stats.aptitudeShown(entry.element())))
                    .toList();
            OptionalInt required = realm.requiredCultivation();
            return new StatusMessage(
                    character.getName().value(),
                    new RealmView(realm.name(), realm.koreanName()),
                    new SpiritRootView(root.name(), roots),
                    new StatsView(new GaugeView(Damage.gauge(stats.qiBloodCurrent()), stats.qiBlood()),
                            new GaugeView(Damage.gauge(stats.manaCurrent()), stats.mana()),
                            GaugeView.full(stats.sense()), GaugeView.full(stats.essence()),
                            GaugeView.full(stats.spirit()), stats.body(), stats.strength()),
                    required.isPresent() ? new CultivationView(0, required.getAsInt()) : null,
                    Arrays.stream(InsightKind.values())
                            .map(kind -> new InsightView(kind.name(), kind.koreanName(), insight.floor(kind)))
                            .toList(),
                    requireNonNull(text),
                    "status");
        }

        @Override
        public String type() {
            return "status";
        }

        /** code 는 enum 이름({@code QI_1}), name 은 한글 이름 */
        public record RealmView(String code, String name) {
        }

        /** roots 는 오행 순 */
        public record SpiritRootView(String name, List<RootView> roots) {
            public SpiritRootView {
                roots = List.copyOf(roots);
            }
        }

        /** element 는 enum 이름({@code WOOD}), aptitude 는 소수점을 버린 자질 */
        public record RootView(String element, String elementName, int aptitude) {
        }

        public record StatsView(GaugeView qiBlood, GaugeView mana, GaugeView sense, GaugeView essence, GaugeView spirit,
                                int body, int strength) {
        }

        public record GaugeView(int current, int max) {
            static GaugeView full(int max) {
                return new GaugeView(max, max);
            }
        }

        public record CultivationView(int current, int required) {
        }

        /** kind 는 갈래 enum 이름({@code FIRE}), name 은 한글 이름({@code 화}), amount 는 소수점을 버린 양 */
        public record InsightView(String kind, String name, int amount) {
        }
    }

    /**
     * 안내·결과·거절. tone: {@code info} | {@code error} | {@code item}(물건 명령의 결과) | {@code growth}(성장) |
     * {@code combat}(교전)
     */
    record SystemMessage(String text, String tone) implements GameMessage {
        public static SystemMessage info(String text) {
            return new SystemMessage(text, "info");
        }

        public static SystemMessage error(String text) {
            return new SystemMessage(text, "error");
        }

        public static SystemMessage item(String text) {
            return new SystemMessage(text, "item");
        }

        /** 공법을 익힘·경지가 바뀜 */
        public static SystemMessage growth(String text) {
            return new SystemMessage(text, "growth");
        }

        /** 교전 시작·침·맞음·처치·질 때 */
        public static SystemMessage combat(String text) {
            return new SystemMessage(text, "combat");
        }

        @Override
        public String type() {
            return "system";
        }
    }

    /**
     * 같은 방에서 일어난 일. tone: {@code movement} | {@code item} | {@code combat},
     * event: {@code arrive} | {@code leave} | {@code appear} | {@code disappear} | {@code pick} | {@code drop} | {@code give} |
     * {@code engage} | {@code kill} | {@code defeated} | {@code respawn} | {@code collect}
     */
    record NoticeMessage(String text, String tone, String actor, String event) implements GameMessage {
        public static NoticeMessage movement(String text, String actor, String event) {
            return new NoticeMessage(requireNonNull(text), "movement", actor, event);
        }

        public static NoticeMessage item(String text, String actor, String event) {
            return new NoticeMessage(requireNonNull(text), "item", actor, event);
        }

        /** 교전 시작·처치·질 때·몹이 나타남 */
        public static NoticeMessage combat(String text, String actor, String event) {
            return new NoticeMessage(requireNonNull(text), "combat", actor, event);
        }

        @Override
        public String type() {
            return "notice";
        }
    }

    /** 말하기. text는 그린 한 줄 */
    record SayMessage(String speaker, String message, boolean self, String text, String tone) implements GameMessage {
        public static SayMessage speech(String speaker, String message, boolean self, String text) {
            return new SayMessage(speaker, message, self, text, "speech");
        }

        @Override
        public String type() {
            return "say";
        }
    }

    /** NPC 대답(묻는 사람에게만). word는 물은 낱말(인사면 null), text는 그린 줄들({@code \n} 으로 나뉨) */
    record TalkMessage(String npc, @Nullable String word, String text, String tone) implements GameMessage {
        public static TalkMessage talk(String npc, @Nullable String word, String text) {
            return new TalkMessage(npc, word, text, "talk");
        }

        @Override
        public String type() {
            return "talk";
        }
    }
}
