package mudmk3.domain.item;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.Assert.state;

/**
 * 아이템 종류: 이름·묘사·종류·기준가(하품 영석)·거래 가능·저물대 등급. 이름은 영문 대소문자를 무시하고 하나뿐이다.
 * 영석 네 등급은 엔진이 {@link #spiritStone} 으로 심고 고치지 않는다
 */
@Entity
@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemType extends AbstractEntity {

    public static final int NAME_MAX_LENGTH = 20;
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final long BASE_VALUE_MAX = 100_000_000;

    private static final Pattern NUMBER = Pattern.compile("^\\d+$");
    private static final Pattern ORDINAL_PREFIX = Pattern.compile("^\\d+\\..*$");
    private static final String ALL_WORD = "전부";

    private String name;

    private String description;

    private ItemCategory category;

    private long baseValue;

    private boolean tradable;

    @Getter(AccessLevel.NONE)
    private @Nullable BagGrade bagGrade;

    @Getter(AccessLevel.NONE)
    private @Nullable SpiritStoneGrade spiritStoneGrade;

    public static ItemType create(ItemTypeSaveInfo info) {
        ItemType itemType = new ItemType();
        itemType.apply(info);
        return itemType;
    }

    /** 엔진이 심는 영석 종류: 이름 {@code 하품 영석}, 기준가는 등급 값, 거래 가능 */
    public static ItemType spiritStone(SpiritStoneGrade grade) {
        ItemType itemType = new ItemType();
        itemType.name = grade.itemName();
        itemType.description = grade.description();
        itemType.category = ItemCategory.SPIRIT_STONE;
        itemType.baseValue = grade.value();
        itemType.tradable = true;
        itemType.spiritStoneGrade = requireNonNull(grade);
        return itemType;
    }

    public void update(ItemTypeSaveInfo info) {
        state(!isSpiritStone(), "영석은 고칠 수 없습니다.");
        apply(info);
    }

    public Optional<BagGrade> findBagGrade() {
        return Optional.ofNullable(bagGrade);
    }

    public Optional<SpiritStoneGrade> findSpiritStoneGrade() {
        return Optional.ofNullable(spiritStoneGrade);
    }

    /** 한 칸에 겹치는 수(종류가 정함). 저물대는 0 */
    public int stackSize() {
        return category.stackSize();
    }

    /** 저물대가 늘려 주는 칸. 저물대가 아니면 0 */
    public int bagSlots() {
        return bagGrade == null ? 0 : bagGrade.slots();
    }

    public boolean isBag() {
        return category == ItemCategory.BAG;
    }

    public boolean isSpiritStone() {
        return spiritStoneGrade != null;
    }

    public boolean isQuest() {
        return category == ItemCategory.QUEST;
    }

    /** {@code 귀속} 명령으로 직접 귀속할 수 있는 종류인지 */
    public boolean isBindable() {
        return category.bindable();
    }

    /** 이름 전체가 같은지(영문 대소문자 무시) */
    public boolean isCalled(String input) {
        return name.equalsIgnoreCase(input);
    }

    private void apply(ItemTypeSaveInfo info) {
        requireNonNull(info);
        String newName = ItemNames.normalize(requireNonNull(info.name()));
        ensureValidName(newName);
        String newDescription = requireNonNullElse(info.description(), "");
        if (newDescription.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("묘사는 500자까지 쓸 수 있습니다.");
        }
        ItemCategory newCategory = requireNonNull(info.category());
        if (!newCategory.creatableByAdmin()) {
            throw new IllegalArgumentException("영석 종류는 만들 수 없습니다.");
        }
        if (info.baseValue() < 0 || info.baseValue() > BASE_VALUE_MAX) {
            throw new IllegalArgumentException("기준가는 0 이상 100,000,000 이하로 정하십시오.");
        }
        if (newCategory == ItemCategory.BAG && info.bagGrade() == null) {
            throw new IllegalArgumentException("저물대 등급을 고르십시오.");
        }
        if (newCategory != ItemCategory.BAG && info.bagGrade() != null) {
            throw new IllegalArgumentException("저물대 등급은 저물대에만 정합니다.");
        }

        this.name = newName;
        this.description = newDescription;
        this.category = newCategory;
        this.baseValue = info.baseValue();
        this.tradable = info.tradable();
        this.bagGrade = info.bagGrade();
    }

    /** 명령 문법(수량·{@code 전부}·번호·영석 등급 낱말)과 헷갈리지 않는 이름인지 본다 */
    private static void ensureValidName(String name) {
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("물건 이름은 1~20자로 정하십시오: " + name);
        }
        if (SpiritStoneGrade.fromWord(name).isPresent()) {
            throw new IllegalArgumentException("영석 등급 이름은 물건 이름으로 쓸 수 없습니다: " + name);
        }
        String lastWord = name.substring(name.lastIndexOf(' ') + 1);
        if (NUMBER.matcher(lastWord).matches() || lastWord.equals(ALL_WORD)) {
            throw new IllegalArgumentException("물건 이름의 마지막 낱말은 수나 '전부'일 수 없습니다: " + name);
        }
        if (ORDINAL_PREFIX.matcher(name).matches()) {
            throw new IllegalArgumentException("물건 이름은 '숫자.' 로 시작할 수 없습니다: " + name);
        }
    }
}
