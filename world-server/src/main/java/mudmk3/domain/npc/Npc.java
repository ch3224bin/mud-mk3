package mudmk3.domain.npc;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.Pricing;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Numbers;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

/**
 * NPC: 2차에서는 모두 유니크 NPC다. 인사말·낱말 대답·기본 대답으로 말하고, 방 하나에 배치되거나 배치되지 않는다.
 * 방은 id로만 갖는다(방을 지우면 배치만 풀리므로 FK를 두지 않는다).
 * 상점 역할은 없거나 하나다: 상점 종류와 파는 물건 목록(입력 순서, 50개까지). 물건 종류는 id로만 갖는다
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"topics", "shopItems"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Npc extends AbstractEntity {

    public static final int TEXT_MAX_LENGTH = 500;
    public static final int TOPICS_MAX = 30;
    public static final int SHOP_ITEMS_MAX = 50;

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\S{1,12}$");

    private String name;

    private String greeting;

    private String defaultAnswer;

    private boolean llmEnabled;

    @Getter(AccessLevel.NONE)
    private @Nullable Long roomId;

    @Getter(AccessLevel.NONE)
    private List<NpcTopic> topics = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private @Nullable ShopKind shopKind;

    @Getter(AccessLevel.NONE)
    private List<ShopItem> shopItems = new ArrayList<>();

    public static Npc create(NpcSaveInfo info) {
        Npc npc = new Npc();
        npc.apply(info);
        return npc;
    }

    /** 이름·대사·LLM 켬을 바꾸고 낱말 목록을 통째로 바꾼다. 배치·상점은 그대로다 */
    public void update(NpcSaveInfo info) {
        apply(info);
    }

    public void placeIn(Long roomId) {
        this.roomId = requireNonNull(roomId);
    }

    public void unplace() {
        this.roomId = null;
    }

    public Optional<Long> findRoomId() {
        return Optional.ofNullable(roomId);
    }

    /** 입력 순서의 낱말 목록(바꿀 수 없음) */
    public List<NpcTopic> getTopics() {
        return Collections.unmodifiableList(topics);
    }

    public Optional<NpcTopic> findTopic(String word) {
        return topics.stream()
                .filter(topic -> topic.matches(word))
                .findFirst();
    }

    /** 맞는 낱말의 대답. 없으면 기본 대답 */
    public String answerTo(String word) {
        return findTopic(word).map(NpcTopic::answer).orElse(defaultAnswer);
    }

    /** 이름 전체가 같은지(영문 대소문자 무시) */
    public boolean isCalled(String input) {
        return name.equalsIgnoreCase(input);
    }

    /** 이름이 입력으로 시작하는지(영문 대소문자 무시) */
    public boolean nameStartsWith(String input) {
        return name.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT));
    }

    /**
     * 상점 역할을 정한다(있던 상점은 통째로 바뀐다). 파는 물건은 그 상점 종류가 파는 종류여야 하고, 영석·같은 물건 두 번은 안 된다.
     * 판매가(정하지 않았으면 기준가)는 매입가의 2배보다 커야 하고, 기준가가 0이면 판매가를 정해야 한다
     */
    public void openShop(ShopKind kind, List<ShopOffer> offers) {
        requireNonNull(kind);
        List<ShopOffer> newOffers = List.copyOf(requireNonNull(offers));
        if (newOffers.size() > SHOP_ITEMS_MAX) {
            throw new IllegalArgumentException("상점에 둘 수 있는 물건은 50개까지입니다.");
        }
        List<ShopItem> newItems = new ArrayList<>();
        Set<Long> itemTypeIds = new HashSet<>();
        for (ShopOffer offer : newOffers) {
            ItemType itemType = offer.itemType();
            ensureSellable(kind, itemType);
            ShopItem item = new ShopItem(requireNonNull(itemType.getId()), offer.price());
            if (!itemTypeIds.add(item.itemTypeId())) {
                throw new IllegalArgumentException("같은 물건이 두 번 있습니다: " + itemType.getName());
            }
            ensureFairPrice(itemType, item);
            newItems.add(item);
        }

        this.shopKind = kind;
        this.shopItems.clear();
        this.shopItems.addAll(newItems);
    }

    /** 상점 역할을 없앤다. 상점이 없어도 된다 */
    public void closeShop() {
        this.shopKind = null;
        this.shopItems.clear();
    }

    public Optional<ShopKind> findShopKind() {
        return Optional.ofNullable(shopKind);
    }

    public boolean hasShop() {
        return shopKind != null;
    }

    /** 파는 물건(입력 순서, 바꿀 수 없음). 상점이 없으면 비어 있다 */
    public List<ShopItem> getShopItems() {
        return Collections.unmodifiableList(shopItems);
    }

    /** 상점 이름 {@code 왕씨 단약방}. 상점이 없으면 {@code IllegalStateException} */
    public String shopName() {
        state(shopKind != null, "상점이 아닙니다.");
        return name + " " + requireNonNull(shopKind).koreanName();
    }

    private static void ensureSellable(ShopKind kind, ItemType itemType) {
        if (itemType.isSpiritStone()) {
            throw new IllegalArgumentException("영석은 상점에서 팔 수 없습니다.");
        }
        if (!kind.sells(itemType.getCategory())) {
            throw new IllegalArgumentException(Josa.withEunNeun(kind.koreanName()) + " "
                    + Josa.withEulReul(itemType.getCategory().koreanName()) + " 팔 수 없습니다.");
        }
    }

    /** 룰북 item.md 불변식 1: 매입가 &lt; 판매가 × 0.5 */
    private static void ensureFairPrice(ItemType itemType, ShopItem item) {
        if (item.price() == null && itemType.getBaseValue() == 0) {
            throw new IllegalArgumentException("기준가가 0인 물건은 값을 정해야 합니다: " + itemType.getName());
        }
        long price = item.effectivePrice(itemType);
        if (!Pricing.isFairSellPrice(itemType, price)) {
            throw new IllegalArgumentException("판매가는 매입가의 2배보다 커야 합니다: " + itemType.getName()
                    + " (매입가 " + Numbers.decimal(Pricing.unitBuyPrice(itemType))
                    + ", 판매가 " + Numbers.count(price) + ")");
        }
    }

    private void apply(NpcSaveInfo info) {
        requireNonNull(info);
        String newName = requireNonNull(info.name()).strip();
        if (!NAME_PATTERN.matcher(newName).matches()) {
            throw new IllegalArgumentException("NPC 이름은 띄어쓰기 없이 1~12자로 정하십시오: " + newName);
        }
        String newGreeting = requireNonNull(info.greeting()).strip();
        if (newGreeting.isEmpty() || newGreeting.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException("인사말은 1~500자로 정하십시오.");
        }
        String newDefaultAnswer = requireNonNull(info.defaultAnswer()).strip();
        if (newDefaultAnswer.isEmpty() || newDefaultAnswer.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException("기본 대답은 1~500자로 정하십시오.");
        }
        List<NpcTopic> newTopics = List.copyOf(requireNonNull(info.topics()));
        if (newTopics.size() > TOPICS_MAX) {
            throw new IllegalArgumentException("낱말은 30개까지 넣을 수 있습니다.");
        }
        ensureNoDuplicateWord(newTopics);

        this.name = newName;
        this.greeting = newGreeting;
        this.defaultAnswer = newDefaultAnswer;
        this.llmEnabled = info.llmEnabled();
        this.topics.clear();
        this.topics.addAll(newTopics);
    }

    /** 같은 낱말인지는 대화에서 낱말을 찾을 때와 같은 규칙({@link NpcTopic#matches})으로 본다 */
    private static void ensureNoDuplicateWord(List<NpcTopic> topics) {
        for (int i = 0; i < topics.size(); i++) {
            NpcTopic topic = topics.get(i);
            if (topics.subList(0, i).stream().anyMatch(earlier -> earlier.matches(topic.word()))) {
                throw new IllegalArgumentException("같은 낱말이 두 번 있습니다: " + topic.word());
            }
        }
    }
}
