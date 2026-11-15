package mudmk3.application.mob.provided;

import mudmk3.domain.mob.MobKind;
import mudmk3.domain.mob.MobTier;
import mudmk3.domain.mob.MobTypeSaveInfo;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * 몹 유형을 만들거나 고치는 요청. 빠진 묘사는 빈 글, 빠진 목록은 빈 목록, 빠진 이름·공격 이름은 빈 글(도메인이 이름 규칙으로
 * 막는다)이다. 규칙 검사와 문구는 도메인이 맡는다. 종류·기준 경지·체급이 없거나 목록 안에 빈 값이 있으면 만들 때
 * {@code IllegalArgumentException} 을 던져, JSON 을 읽는 중이면 공통 400 {@code 요청 본문을 읽을 수 없습니다} 가 되게 한다
 * (4차 공법 요청과 같은 방식)
 */
public record MobTypeSaveRequest(
        String name,
        @Nullable String description,
        MobKind kind,
        Realm realm,
        MobTier tier,
        String attackName,
        @Nullable BigDecimal attackCoefficient,
        @Nullable Integer respawnSeconds,
        @Nullable List<MobFixedDropRequest> fixedDrops,
        @Nullable List<MobTierDropRequest> tierDrops
) {
    public MobTypeSaveRequest {
        name = name == null ? "" : name;
        required(kind, "kind");
        required(realm, "realm");
        required(tier, "tier");
        attackName = attackName == null ? "" : attackName;
        fixedDrops = list(fixedDrops, "fixedDrops");
        tierDrops = list(tierDrops, "tierDrops");
    }

    /** 컴팩트 생성자가 이미 채웠지만 컴포넌트가 @Nullable 타입이라 다시 채운다 */
    public MobTypeSaveInfo toInfo() {
        return new MobTypeSaveInfo(name, description, kind, realm, tier, attackName, attackCoefficient, respawnSeconds,
                list(fixedDrops, "fixedDrops").stream().map(MobFixedDropRequest::toDrop).toList(),
                list(tierDrops, "tierDrops").stream().map(MobTierDropRequest::toDrop).toList());
    }

    /** 필수 값이 없으면 {@code IllegalArgumentException("<필드> 는 꼭 보내야 합니다")} */
    static void required(@Nullable Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 는 꼭 보내야 합니다");
        }
    }

    private static <T> List<T> list(@Nullable List<T> values, String fieldName) {
        if (values == null) return List.of();
        if (values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(fieldName + " 에 빈 값이 있습니다");
        }
        return List.copyOf(values);
    }
}
