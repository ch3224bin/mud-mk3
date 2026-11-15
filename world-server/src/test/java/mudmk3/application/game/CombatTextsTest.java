package mudmk3.application.game;

import mudmk3.domain.combat.CombatFixture;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.combat.MobProfile;
import mudmk3.domain.mob.MobFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CombatTextsTest {

    private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");

    private final CombatTexts texts = new CombatTexts();

    @Test
    void particles() {
        assertThat(texts.engaged("들토끼3")).isEqualTo("들토끼3와 싸움이 벌어진다.");
        assertThat(texts.engaged("청모랑")).isEqualTo("청모랑과 싸움이 벌어진다.");
        assertThat(texts.engagedNotice("한백", "들토끼")).isEqualTo("한백이 들토끼와 싸우기 시작한다.");
        assertThat(texts.alreadyFighting("청모랑")).isEqualTo("이미 청모랑과 싸우고 있습니다.");
        assertThat(texts.notAttackable("왕씨")).isEqualTo("왕씨는 공격할 수 있는 상대가 아닙니다.");
        assertThat(texts.killed("청모랑")).isEqualTo("청모랑을 쓰러뜨렸습니다.");
        assertThat(texts.killed("들토끼")).isEqualTo("들토끼를 쓰러뜨렸습니다.");
        assertThat(texts.fell("청모랑")).isEqualTo("청모랑이 쓰러졌다.");
        assertThat(texts.killedNotice("한백", "청모랑")).isEqualTo("한백이 청모랑을 쓰러뜨렸다.");
        assertThat(texts.defeatedNotice("여소")).isEqualTo("여소가 쓰러진다.");
    }

    @Test
    void mobHitAttackNameParticle() {
        assertThat(texts.mobHit(CombatFixture.wolfProfile(), 5, 55, 60))
                .isEqualTo("청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 55/60)");
        MobProfile boss = CombatFixture.profile(MobFixture.boss(), 3L);
        assertThat(texts.mobHit(boss, 16, 44, 60))
                .isEqualTo("쌍두 흑망이 두 머리 물기로 덮친다! (피해 16, 기혈 44/60)");
        MobProfile snake = CombatFixture.profile(MobFixture.snake(), 4L);
        assertThat(texts.mobHit(snake, 1, 59, 60)).isEqualTo("적안 독사가 독니로 덮친다! (피해 1, 기혈 59/60)");
    }

    @Test
    void damageAndQiBloodDisplay() {
        assertThat(texts.playerHit("청모랑", 8)).isEqualTo("청모랑에게 일격을 날린다. (피해 8)");
        assertThat(texts.playerHit("청모랑", 0.4)).isEqualTo("청모랑에게 일격을 날린다. (피해 1 미만)");
        assertThat(texts.playerHit("청모랑", 0)).isEqualTo("청모랑에게 일격을 날린다. (피해 0)");
        assertThat(texts.playerHit("청모랑", 8.5)).isEqualTo("청모랑에게 일격을 날린다. (피해 9)");
        // 기혈은 버린 정수, 0 초과 1 미만은 1
        assertThat(texts.mobHit(CombatFixture.wolfProfile(), 0.4, 59.6, 60))
                .isEqualTo("청모랑이 물어뜯기로 덮친다! (피해 1 미만, 기혈 59/60)");
        assertThat(texts.mobHit(CombatFixture.wolfProfile(), 7.5, 0.5, 10))
                .isEqualTo("청모랑이 물어뜯기로 덮친다! (피해 8, 기혈 1/10)");
        assertThat(texts.mobHit(CombatFixture.wolfProfile(), 7.5, 0, 10))
                .isEqualTo("청모랑이 물어뜯기로 덮친다! (피해 8, 기혈 0/10)");
    }

    @Test
    void statusLineNumbersSameNamesAndShowsCondition() {
        MobInstance hurt = CombatFixture.wolf(NOW);
        hurt.engage(1L, NOW);
        hurt.receive(1L, 24, NOW); // 피해 20 → 29/49 = 59% 다침
        MobInstance healthy = MobInstance.spawn(2, 100L, 1, 10L, 2, CombatFixture.wolfProfile(), NOW);
        MobInstance rabbit = MobInstance.spawn(3, 101L, 0, 10L, 1, CombatFixture.rabbitProfile(), NOW);

        assertThat(texts.statusLine(List.of(hurt, healthy, rabbit))).isEqualTo("[전투 중] 상대: 청모랑(다침), 청모랑 2, 들토끼");
        assertThat(texts.statusLine(List.of(healthy, hurt))).isEqualTo("[전투 중] 상대: 청모랑, 청모랑 2(다침)");
        assertThat(texts.statusLine(List.of())).isEmpty();
    }

    @Test
    void statusLineDyingAndBadlyHurt() {
        MobInstance badly = CombatFixture.wolf(NOW);
        badly.engage(1L, NOW);
        badly.receive(1L, 38, NOW); // 피해 34 → 15/49 = 30.6% 크게 다침
        MobInstance nearDeath = MobInstance.spawn(2, 100L, 1, 10L, 2, CombatFixture.wolfProfile(), NOW);
        nearDeath.engage(1L, NOW);
        nearDeath.receive(1L, 47, NOW); // 피해 43 → 6/49 = 12% 빈사

        assertThat(texts.statusLine(List.of(badly, nearDeath))).isEqualTo("[전투 중] 상대: 청모랑(크게 다침), 청모랑 2(빈사)");
    }
}
