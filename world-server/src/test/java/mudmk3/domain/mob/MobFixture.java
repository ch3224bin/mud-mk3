package mudmk3.domain.mob;

import mudmk3.application.mob.provided.MobFixedDropRequest;
import mudmk3.application.mob.provided.MobTierDropRequest;
import mudmk3.application.mob.provided.MobTypeSaveRequest;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 룰북 첫 지역 몹(들토끼·청모랑·쌍두 흑망)으로 만든 몹 입력·요청 */
public final class MobFixture {

    private MobFixture() {
    }

    /** 들토끼: 요수 일반 연기 1층, 뒷발차기, 재등장 체급 기준, 드롭 표 없음 */
    public static MobTypeSaveInfo rabbit() {
        return info("들토끼", MobKind.BEAST, Realm.QI_1, MobTier.NORMAL, "뒷발차기", null, null);
    }

    /** 청모랑: 요수 일반 연기 2층, 물어뜯기, 재등장 체급 기준, 드롭 표 없음 */
    public static MobTypeSaveInfo wolf() {
        return info("청모랑", MobKind.BEAST, Realm.QI_2, MobTier.NORMAL, "물어뜯기", null, null);
    }

    /** 쌍두 흑망: 요수 두목 연기 12층, 두 머리 물기, 공격 계수 0.6, 재등장 2,700초, 드롭 표 없음 */
    public static MobTypeSaveInfo boss() {
        return info("쌍두 흑망", MobKind.BEAST, Realm.QI_12, MobTier.BOSS, "두 머리 물기", new BigDecimal("0.6"), 2_700);
    }

    /** 적안 독사: 요수 강적 연기 3층, 독니, 재등장 체급 기준, 드롭 표 없음 */
    public static MobTypeSaveInfo snake() {
        return info("적안 독사", MobKind.BEAST, Realm.QI_3, MobTier.STRONG, "독니", null, null);
    }

    /** 철갑 거북: 요수 일반 축기 초기(3급), 박치기, 재등장 체급 기준, 드롭 표 없음 */
    public static MobTypeSaveInfo turtle() {
        return info("철갑 거북", MobKind.BEAST, Realm.FOUNDATION_EARLY, MobTier.NORMAL, "박치기", null, null);
    }

    /** 드롭 표 없는 몹 입력. 묘사는 {@code {이름}의 묘사다.} */
    public static MobTypeSaveInfo info(String name, MobKind kind, Realm realm, MobTier tier, String attackName,
                                       @Nullable BigDecimal attackCoefficient, @Nullable Integer respawnSeconds) {
        return new MobTypeSaveInfo(name, name + "의 묘사다.", kind, realm, tier, attackName, attackCoefficient,
                respawnSeconds, List.of(), List.of());
    }

    /** 같은 몹에 드롭 표를 바꿔 끼운다 */
    public static MobTypeSaveInfo withDrops(MobTypeSaveInfo info, List<MobFixedDrop> fixedDrops,
                                            List<MobTierDrop> tierDrops) {
        return new MobTypeSaveInfo(info.name(), info.description(), info.kind(), info.realm(), info.tier(),
                info.attackName(), info.attackCoefficient(), info.respawnSeconds(), fixedDrops, tierDrops);
    }

    /** 같은 몹의 묘사만 바꾼다 */
    public static MobTypeSaveInfo described(MobTypeSaveInfo info, String description) {
        return new MobTypeSaveInfo(info.name(), description, info.kind(), info.realm(), info.tier(),
                info.attackName(), info.attackCoefficient(), info.respawnSeconds(), info.fixedDrops(),
                info.tierDrops());
    }

    /** 같은 몹의 기준 경지만 바꾼다 */
    public static MobTypeSaveInfo atRealm(MobTypeSaveInfo info, Realm realm) {
        return new MobTypeSaveInfo(info.name(), info.description(), info.kind(), realm, info.tier(),
                info.attackName(), info.attackCoefficient(), info.respawnSeconds(), info.fixedDrops(),
                info.tierDrops());
    }

    /** 같은 몹의 이름만 바꾼다 */
    public static MobTypeSaveInfo named(MobTypeSaveInfo info, String name) {
        return new MobTypeSaveInfo(name, info.description(), info.kind(), info.realm(), info.tier(),
                info.attackName(), info.attackCoefficient(), info.respawnSeconds(), info.fixedDrops(),
                info.tierDrops());
    }

    /** 드롭 표가 없거나 물건 이름이 필요 없는 몹 */
    public static MobType create(MobTypeSaveInfo info) {
        return MobType.create(info, Map.of());
    }

    /** 관리 REST 모양의 요청(값은 입력과 같음) */
    public static MobTypeSaveRequest request(MobTypeSaveInfo info) {
        return new MobTypeSaveRequest(info.name(), info.description(), info.kind(), info.realm(), info.tier(),
                info.attackName(), info.attackCoefficient(), info.respawnSeconds(),
                info.fixedDrops().stream().map(drop -> new MobFixedDropRequest(drop.itemTypeId(), drop.count())).toList(),
                info.tierDrops().stream().map(drop -> new MobTierDropRequest(drop.tier(), drop.itemTypeId())).toList());
    }
}
