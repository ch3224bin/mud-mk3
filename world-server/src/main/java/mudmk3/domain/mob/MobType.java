package mudmk3.domain.mob;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 몹 유형: 이름(몹끼리 유일, 영문 대소문자 무시 — 서비스가 본다), 묘사, 종류, 기준 경지, 체급, 공격 이름, 공격 계수(두목만),
 * 재등장(초, 비면 체급 기준), 드롭 표(고정 재료·계층 드롭, 입력 순서). 스탯은 저장하지 않고 계산한다.
 * 만들기·고치기는 admin-tool.md 검사 순서(이름 → 묘사 → 기준 경지 → 공격 이름 → 공격 계수 → 재등장 → 고정 재료 → 계층 드롭)로
 * 검사해 처음 걸린 규칙 하나만 {@code IllegalArgumentException} 으로 알린다
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = {"fixedDrops", "tierDrops"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobType extends AbstractEntity {

    public static final int NAME_MAX_LENGTH = 20;
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final int ATTACK_NAME_MAX_LENGTH = 12;
    public static final int FIXED_DROPS_MAX = 5;
    public static final int FIXED_DROP_COUNT_MAX = 99;
    public static final int TIER_DROPS_MAX = 10;
    public static final BigDecimal BOSS_COEFFICIENT_MIN = new BigDecimal("0.4");
    public static final BigDecimal BOSS_COEFFICIENT_MAX = new BigDecimal("0.8");
    public static final int BOSS_RESPAWN_MIN = 1_800;
    public static final int BOSS_RESPAWN_MAX = 3_600;
    public static final int RESPAWN_MIN = 10;
    public static final int RESPAWN_MAX = 86_400;

    /** {@code 2.청모랑} 처럼 번호로 가리키는 문법과 헷갈리는 이름 */
    private static final Pattern ORDINAL_PREFIX = Pattern.compile("^\\d+\\..*", Pattern.DOTALL);

    private String name;

    private String description;

    private MobKind kind;

    private Realm realm;

    private MobTier tier;

    private String attackName;

    private @Nullable BigDecimal attackCoefficient;

    private @Nullable Integer respawnSeconds;

    @Getter(AccessLevel.NONE)
    private List<MobFixedDrop> fixedDrops = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private List<MobTierDrop> tierDrops = new ArrayList<>();

    /** {@code itemNames} 는 드롭 표 물건의 이름(문구용, 서비스가 만든다). 빠진 id 는 3차 없는 물건 문구로 막는다 */
    public static MobType create(MobTypeSaveInfo info, Map<Long, String> itemNames) {
        MobType mobType = new MobType();
        mobType.apply(info, itemNames);
        return mobType;
    }

    /** 모든 칸을 바꾸고 드롭 표를 통째로 바꾼다 */
    public void update(MobTypeSaveInfo info, Map<Long, String> itemNames) {
        apply(info, itemNames);
    }

    /** 기준 경지·종류·체급(두목은 공격 계수)으로 계산한 스탯 */
    public MobStats stats() {
        return MobStats.of(kind, realm, tier, attackCoefficient);
    }

    /** 급/경지 이름: 요수는 {@code 1급}, 나머지는 {@code 연기 6층} */
    public String rankName() {
        return kind == MobKind.BEAST ? realm.beastGrade() + "급" : realm.koreanName();
    }

    /** 실제 재등장(초): 정한 값, 없으면 체급 기준 */
    public int effectiveRespawnSeconds() {
        if (respawnSeconds != null) return respawnSeconds;
        return tier.defaultRespawnSeconds()
                .orElseThrow(() -> new IllegalStateException("두목의 재등장이 비어 있습니다: " + name));
    }

    /** 고정 재료(입력 순서, 바꿀 수 없음) */
    public List<MobFixedDrop> getFixedDrops() {
        return Collections.unmodifiableList(fixedDrops);
    }

    /** 계층 드롭 전부(입력 순서, 바꿀 수 없음) */
    public List<MobTierDrop> getTierDrops() {
        return Collections.unmodifiableList(tierDrops);
    }

    /** 그 계층 목록의 물건 종류 id(입력 순서) */
    public List<Long> tierItemIds(DropTier dropTier) {
        return tierDrops.stream()
                .filter(drop -> drop.tier() == dropTier)
                .map(MobTierDrop::itemTypeId)
                .toList();
    }

    private void apply(MobTypeSaveInfo info, Map<Long, String> itemNames) {
        requireNonNull(info);
        requireNonNull(itemNames);
        ensureValidName(info.name());
        String newDescription = requireNonNull(info.description());
        if (newDescription.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("몹 묘사는 500자 이내로 적으십시오.");
        }
        if (info.realm() == Realm.MORTAL) {
            throw new IllegalArgumentException("몹의 기준 경지는 연기 1층 이상으로 정하십시오.");
        }
        if (info.attackName().isEmpty() || info.attackName().length() > ATTACK_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("공격 이름은 1~12자로 정하십시오.");
        }
        ensureValidCoefficient(info.tier(), info.attackCoefficient());
        ensureValidRespawn(info.tier(), info.respawnSeconds());
        ensureValidFixedDrops(info.fixedDrops(), itemNames);
        ensureValidTierDrops(info.tierDrops(), itemNames);

        this.name = info.name();
        this.description = newDescription;
        this.kind = info.kind();
        this.realm = info.realm();
        this.tier = info.tier();
        this.attackName = info.attackName();
        BigDecimal coefficient = info.attackCoefficient();
        this.attackCoefficient = coefficient == null ? null : coefficient.setScale(1);
        this.respawnSeconds = info.respawnSeconds();
        this.fixedDrops.clear();
        this.fixedDrops.addAll(info.fixedDrops());
        this.tierDrops.clear();
        this.tierDrops.addAll(info.tierDrops());
    }

    private static void ensureValidName(String name) {
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("몹 이름은 1~20자로 정하십시오.");
        }
        if (ORDINAL_PREFIX.matcher(name).matches()) {
            throw new IllegalArgumentException("몹 이름은 '2.' 처럼 수와 점으로 시작할 수 없습니다.");
        }
    }

    /** 두목이 아님 → 소수 자리 → 범위·빠짐 */
    private static void ensureValidCoefficient(MobTier tier, @Nullable BigDecimal coefficient) {
        if (tier != MobTier.BOSS) {
            if (coefficient != null) {
                throw new IllegalArgumentException("공격 계수는 두목만 정합니다.");
            }
            return;
        }
        if (coefficient != null && coefficient.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("공격 계수는 소수 첫째 자리까지 적으십시오.");
        }
        if (coefficient == null
                || coefficient.compareTo(BOSS_COEFFICIENT_MIN) < 0 || coefficient.compareTo(BOSS_COEFFICIENT_MAX) > 0) {
            throw new IllegalArgumentException("두목의 공격 계수는 0.4~0.8로 정하십시오.");
        }
    }

    private static void ensureValidRespawn(MobTier tier, @Nullable Integer seconds) {
        if (tier == MobTier.BOSS) {
            if (seconds == null || seconds < BOSS_RESPAWN_MIN || seconds > BOSS_RESPAWN_MAX) {
                throw new IllegalArgumentException("두목의 재등장은 1,800~3,600초로 정하십시오.");
            }
            return;
        }
        if (seconds != null && (seconds < RESPAWN_MIN || seconds > RESPAWN_MAX)) {
            throw new IllegalArgumentException("재등장은 10~86,400초로 정하십시오.");
        }
    }

    private static void ensureValidFixedDrops(List<MobFixedDrop> drops, Map<Long, String> itemNames) {
        if (drops.size() > FIXED_DROPS_MAX) {
            throw new IllegalArgumentException("고정 재료는 5종까지 둘 수 있습니다.");
        }
        Set<Long> seen = new HashSet<>();
        for (MobFixedDrop drop : drops) {
            String itemName = itemName(itemNames, drop.itemTypeId());
            if (drop.count() < 1 || drop.count() > FIXED_DROP_COUNT_MAX) {
                throw new IllegalArgumentException("고정 재료의 수는 1~99로 정하십시오: " + itemName);
            }
            if (!seen.add(drop.itemTypeId())) {
                throw duplicateItem(itemName);
            }
        }
    }

    /** 계층마다(선언 순) 물건 수 → 같은 계층 안 같은 물건 */
    private static void ensureValidTierDrops(List<MobTierDrop> drops, Map<Long, String> itemNames) {
        Map<DropTier, Set<Long>> seen = new EnumMap<>(DropTier.class);
        for (DropTier dropTier : DropTier.values()) {
            seen.put(dropTier, new HashSet<>());
            long count = drops.stream().filter(drop -> drop.tier() == dropTier).count();
            if (count > TIER_DROPS_MAX) {
                throw new IllegalArgumentException("계층마다 물건은 10개까지 둘 수 있습니다. (" + dropTier.koreanName() + ")");
            }
        }
        for (MobTierDrop drop : drops) {
            String itemName = itemName(itemNames, drop.itemTypeId());
            if (!requireNonNull(seen.get(drop.tier())).add(drop.itemTypeId())) {
                throw duplicateItem(itemName);
            }
        }
    }

    private static String itemName(Map<Long, String> itemNames, Long itemTypeId) {
        String itemName = itemNames.get(itemTypeId);
        if (itemName == null) {
            throw new IllegalArgumentException("물건 종류를 찾을 수 없습니다. id: " + itemTypeId);
        }
        return itemName;
    }

    private static IllegalArgumentException duplicateItem(String itemName) {
        return new IllegalArgumentException("같은 물건이 두 번 있습니다: " + itemName);
    }
}
