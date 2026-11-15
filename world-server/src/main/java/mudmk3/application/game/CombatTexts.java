package mudmk3.application.game;

import mudmk3.domain.combat.Condition;
import mudmk3.domain.combat.Damage;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.MobProfile;
import mudmk3.domain.shared.Durations;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Numbers;
import mudmk3.domain.shared.Realm;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 교전 줄과 몹 보기 줄을 그린다(command.md {@code ### 교전 메시지}). 문구만 맡고 상태를 바꾸지 않는다
 */
@Component
class CombatTexts {

    static final String UNKNOWABLE_RANK = "헤아릴 수 없음";
    static final String ATTACK_NO_ARGUMENT = "누구를 공격하시겠습니까? (예: 공격 늑대)";
    static final String ATTACK_PLAYER = "다른 사람과는 아직 싸울 수 없습니다.";
    static final String CANNOT_MOVE = "싸우는 중에는 움직일 수 없습니다.";
    static final String CANNOT_QUIT = "싸우는 중에는 떠날 수 없습니다.";
    static final String CANNOT_DO = "싸우는 중에는 할 수 없습니다.";
    static final String DEFEATED = "기혈이 다했습니다. 눈앞이 캄캄해진다…";
    static final String COLLECT_NO_ARGUMENT = "무엇을 수습하시겠습니까? (예: 수습 늑대)";
    private static final String STATUS_PREFIX = "[전투 중] 상대: ";
    private static final int UNKNOWABLE_MAJOR_GAP = 2;

    /**
     * 보는 사람에게 보이는 급·경지: 몹의 대경지가 보는 사람보다 2단계 이상 높으면 {@code 헤아릴 수 없음},
     * 아니면 요수는 {@code 1급}, 나머지는 경지 이름
     */
    static String rank(MobProfile profile, Realm viewerRealm) {
        if (profile.realm().majorIndex() - viewerRealm.majorIndex() >= UNKNOWABLE_MAJOR_GAP) {
            return UNKNOWABLE_RANK;
        }
        return profile.rankName();
    }

    /** 몹이 나타남(재등장·새 배치): {@code 청모랑이 나타난다.} */
    String appears(String mobName) {
        return Josa.withIGa(mobName) + " 나타난다.";
    }

    /**
     * {@code 보기 <몹>}: {@code 청모랑 (1급) — 푸른 털의 늑대. 멀쩡하다.}, 체급 표시는 괄호 뒤({@code 적안 독사 (1급) [강적] — …}),
     * 묘사가 비면 {@code —} 뒤에 상태 문장만
     */
    String lookMob(MobInstance mob, Realm viewerRealm) {
        MobProfile profile = mob.getProfile();
        StringBuilder text = new StringBuilder(profile.name())
                .append(" (").append(rank(profile, viewerRealm)).append(')');
        String badge = profile.tier().badge();
        if (badge != null) {
            text.append(" [").append(badge).append(']');
        }
        text.append(" — ");
        if (!profile.description().isEmpty()) {
            text.append(profile.description()).append(' ');
        }
        return text.append(mob.condition().sentence()).toString();
    }

    /** {@code 왕씨는 공격할 수 있는 상대가 아닙니다.} */
    String notAttackable(String npcName) {
        return Josa.withEunNeun(npcName) + " 공격할 수 있는 상대가 아닙니다.";
    }

    /** {@code 이미 청모랑과 싸우고 있습니다.} */
    String alreadyFighting(String mobName) {
        return "이미 " + Josa.withGwaWa(mobName) + " 싸우고 있습니다.";
    }

    /** 교전 시작, 공격한 사람: {@code 청모랑과 싸움이 벌어진다.} */
    String engaged(String mobName) {
        return Josa.withGwaWa(mobName) + " 싸움이 벌어진다.";
    }

    /** 교전 시작, 같은 방 다른 사람: {@code 한립이 청모랑과 싸우기 시작한다.} */
    String engagedNotice(String playerName, String mobName) {
        return Josa.withIGa(playerName) + " " + Josa.withGwaWa(mobName) + " 싸우기 시작한다.";
    }

    /** 내가 침: {@code 청모랑에게 일격을 날린다. (피해 9)} — 피해는 육신 층을 거친 값 */
    String playerHit(String mobName, double damage) {
        return mobName + "에게 일격을 날린다. (피해 " + Damage.display(damage) + ")";
    }

    /** 몹이 나를 침: {@code 청모랑이 물어뜯기로 덮친다! (피해 4, 기혈 56/60)} */
    String mobHit(MobProfile mob, double damage, double qiBloodCurrent, int qiBloodMax) {
        return Josa.withIGa(mob.name()) + " " + Josa.withEuroRo(mob.attackName()) + " 덮친다! (피해 "
                + Damage.display(damage) + ", 기혈 " + Damage.gauge(qiBloodCurrent) + "/" + qiBloodMax + ")";
    }

    /** 몹이 쓰러짐, 처치자: {@code 청모랑을 쓰러뜨렸습니다.} */
    String killed(String mobName) {
        return Josa.withEulReul(mobName) + " 쓰러뜨렸습니다.";
    }

    /** 몹이 쓰러짐, 그 몹과 교전하던 다른 사람: {@code 청모랑이 쓰러졌다.} */
    String fell(String mobName) {
        return Josa.withIGa(mobName) + " 쓰러졌다.";
    }

    /** 몹이 쓰러짐, 같은 방의 교전하지 않은 사람: {@code 한립이 청모랑을 쓰러뜨렸다.} */
    String killedNotice(String killerName, String mobName) {
        return Josa.withIGa(killerName) + " " + Josa.withEulReul(mobName) + " 쓰러뜨렸다.";
    }

    /** 질 때, 같은 방 사람: {@code 한립이 쓰러진다.} */
    String defeatedNotice(String playerName) {
        return Josa.withIGa(playerName) + " 쓰러진다.";
    }

    /**
     * {@code 상태} 의 전투 줄: {@code [전투 중] 상대: 청모랑(다침), 청모랑 2}. 교전한 순서, 같은 이름 둘째부터 {@code  2}·{@code  3},
     * 기혈 상태가 멀쩡함이 아니면 괄호에 구간 이름. 교전 상대가 없으면 빈 문자열
     */
    String statusLine(List<MobInstance> opponents) {
        if (opponents.isEmpty()) {
            return "";
        }
        Map<String, Integer> seen = new HashMap<>();
        List<String> names = new ArrayList<>();
        for (MobInstance mob : opponents) {
            String name = mob.getProfile().name();
            int nth = seen.merge(name, 1, Integer::sum);
            StringBuilder entry = new StringBuilder(name);
            if (nth >= 2) {
                entry.append(' ').append(nth);
            }
            Condition condition = mob.condition();
            if (condition != Condition.HEALTHY) {
                entry.append('(').append(condition.koreanName()).append(')');
            }
            names.add(entry.toString());
        }
        return STATUS_PREFIX + String.join(", ", names);
    }

    // ---- 수습

    /** 드롭 한 줄: {@code 늑대 이빨 1} */
    static String dropPart(String itemName, int count) {
        return itemName + " " + Numbers.count(count);
    }

    /** 다른 사람 몫: {@code 그 시체는 아직 다른 사람의 몫입니다. (34초 뒤 수습할 수 있습니다)} — 남은 시간은 올림한 초 */
    String otherShare(Duration lockedFor) {
        long seconds = Math.ceilDiv(lockedFor.toNanos(), 1_000_000_000L);
        return "그 시체는 아직 다른 사람의 몫입니다. (" + Durations.korean(BigDecimal.valueOf(seconds)) + " 뒤 수습할 수 있습니다)";
    }

    /** 거둠: {@code 청모랑의 시체를 수습했습니다: 청모랑 가죽 1, 늑대 이빨 1} */
    String collected(String corpseName, List<String> parts) {
        return Josa.withEulReul(corpseName) + " 수습했습니다: " + String.join(", ", parts);
    }

    /** 다 넣지 못함: {@code 저물대에 자리가 없습니다. 남은 것: 늑대 이빨 1} */
    String noRoomLeft(List<String> parts) {
        return "저물대에 자리가 없습니다. 남은 것: " + String.join(", ", parts);
    }

    /** 빈 시체: {@code 청모랑의 시체에는 거둘 것이 없습니다.} */
    String emptyCorpse(String corpseName) {
        return corpseName + "에는 거둘 것이 없습니다.";
    }

    /** 수습, 같은 방 다른 사람: {@code 한립이 청모랑의 시체를 수습한다.} */
    String collectNotice(String playerName, String corpseName) {
        return Josa.withIGa(playerName) + " " + Josa.withEulReul(corpseName) + " 수습한다.";
    }

    // ---- 질 때·교전 뒤 기혈

    /** 질 때 둘째 줄: {@code 태초 객잔에서 깨어났습니다. (기혈 1/120)} */
    static String wokeUp(String roomName, int maxQiBlood) {
        return roomName + "에서 깨어났습니다. (기혈 1/" + maxQiBlood + ")";
    }

    /** 교전 뒤 다시 참: {@code 숨을 고르자 기운이 돌아온다. (기혈 120/120)} */
    String refilled(int maxQiBlood) {
        return "숨을 고르자 기운이 돌아온다. (기혈 " + maxQiBlood + "/" + maxQiBlood + ")";
    }
}
