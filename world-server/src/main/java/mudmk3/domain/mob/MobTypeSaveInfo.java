package mudmk3.domain.mob;

import mudmk3.domain.shared.Names;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * 몹을 만들거나 고칠 때 받는 값. 이름·묘사·공격 이름은 앞뒤 공백을 자르고, 빠진 묘사는 빈 글이다.
 * 공격 계수는 두목만, 재등장(초)은 비우면 체급 기준이다. 드롭 표는 입력 순서 그대로다. 규칙 검사는 {@link MobType} 이 한다
 */
public record MobTypeSaveInfo(
        String name,
        @Nullable String description,
        MobKind kind,
        Realm realm,
        MobTier tier,
        String attackName,
        @Nullable BigDecimal attackCoefficient,
        @Nullable Integer respawnSeconds,
        List<MobFixedDrop> fixedDrops,
        List<MobTierDrop> tierDrops
) {
    public MobTypeSaveInfo {
        name = requireNonNull(Names.strip(name));
        description = description == null ? "" : description.strip();
        requireNonNull(kind);
        requireNonNull(realm);
        requireNonNull(tier);
        attackName = requireNonNull(Names.strip(attackName));
        fixedDrops = List.copyOf(fixedDrops);
        tierDrops = List.copyOf(tierDrops);
    }
}
