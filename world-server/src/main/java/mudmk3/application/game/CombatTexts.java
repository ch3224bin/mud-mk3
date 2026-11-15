package mudmk3.application.game;

import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.MobProfile;
import mudmk3.domain.shared.Josa;
import mudmk3.domain.shared.Realm;
import org.springframework.stereotype.Component;

/**
 * 교전 줄과 몹 보기 줄을 그린다(command.md {@code ### 교전 메시지}). 문구만 맡고 상태를 바꾸지 않는다
 */
@Component
class CombatTexts {

    static final String UNKNOWABLE_RANK = "헤아릴 수 없음";
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
}
