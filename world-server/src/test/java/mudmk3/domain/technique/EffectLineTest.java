package mudmk3.domain.technique;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EffectLineTest {

    @Test
    void firstBadToken() {
        assertThat(EffectLine.firstBadToken("{시전자}의 손에서 화탄이 {대상}에게 터진다!")).isEmpty();
        assertThat(EffectLine.firstBadToken("불꽃이 튄다!")).isEmpty();
        assertThat(EffectLine.firstBadToken("{적}에게")).contains("{적}");
        assertThat(EffectLine.firstBadToken("{{대상}")).contains("{{대상}");
        assertThat(EffectLine.firstBadToken("a}")).contains("}");
        assertThat(EffectLine.firstBadToken("{")).contains("{");
        // 처음 걸린 것 하나
        assertThat(EffectLine.firstBadToken("{대상}이 {적}과 } 를")).contains("{적}");
    }

    @Test
    void placeholders() {
        assertThat(EffectLine.TARGET).isEqualTo("{대상}");
        assertThat(EffectLine.CASTER).isEqualTo("{시전자}");
    }

    @Test
    void render() {
        assertThat(EffectLine.render("{대상}을", "청모랑", "한백")).isEqualTo("청모랑을");
        assertThat(EffectLine.render("{대상}을", "들토끼3", "한백")).isEqualTo("들토끼3를");
        assertThat(EffectLine.render("{대상}를", "청모랑", "한백")).isEqualTo("청모랑을");
        assertThat(EffectLine.render("{대상}로", "청모랑", "한백")).isEqualTo("청모랑으로");
        assertThat(EffectLine.render("{대상}로", "늑대굴", "한백")).isEqualTo("늑대굴로");
        assertThat(EffectLine.render("{대상}으로", "들토끼", "한백")).isEqualTo("들토끼로");
        assertThat(EffectLine.render("{대상}로부터", "청모랑", "한백")).isEqualTo("청모랑으로부터");
        assertThat(EffectLine.render("{시전자}는", "청모랑", "한백")).isEqualTo("한백은");
        assertThat(EffectLine.render("{대상}와", "청모랑", "한백")).isEqualTo("청모랑과");
        assertThat(EffectLine.render("{대상}가", "청모랑", "한백")).isEqualTo("청모랑이");
        assertThat(EffectLine.render("{대상}에게", "청모랑", "한백")).isEqualTo("청모랑에게");
        assertThat(EffectLine.render("{시전자}의 손바닥에서 화탄이 튀어 나가 {대상}에게 터진다!", "청모랑", "한백"))
                .isEqualTo("한백의 손바닥에서 화탄이 튀어 나가 청모랑에게 터진다!");
    }

    @Test
    void renderEveryPlaceholder() {
        assertThat(EffectLine.render("{시전자}가 {대상}을, 다시 {대상}을 친다", "들토끼", "한립"))
                .isEqualTo("한립이 들토끼를, 다시 들토끼를 친다");
        assertThat(EffectLine.render("불꽃이 튄다!", "청모랑", "한백")).isEqualTo("불꽃이 튄다!");
        assertThat(EffectLine.render("{대상}", "청모랑", "한백")).isEqualTo("청모랑");
    }

    @Test
    void renderWithoutTargetKeepsPlaceholder() {
        assertThat(EffectLine.render("{시전자}가 {대상}을 노린다", null, "한백")).isEqualTo("한백이 {대상}을 노린다");
    }

    @Test
    void particleOnlyBeforeBoundary() {
        assertThat(EffectLine.render("{대상}가슴을 벤다", "청모랑", "한백")).isEqualTo("청모랑가슴을 벤다");
        assertThat(EffectLine.render("{대상}이마를 스친다!", "들토끼", "한백")).isEqualTo("들토끼이마를 스친다!");
        assertThat(EffectLine.render("{대상}은빛 털", "들토끼", "한백")).isEqualTo("들토끼은빛 털");
        assertThat(EffectLine.render("{대상}이나 {시전자}", "청모랑", "한백")).isEqualTo("청모랑이나 한백");
        assertThat(EffectLine.render("{대상}을, 다시", "들토끼", "한백")).isEqualTo("들토끼를, 다시");
        assertThat(EffectLine.render("{대상}이!", "들토끼", "한백")).isEqualTo("들토끼가!");
        assertThat(EffectLine.render("{대상}는…", "청모랑", "한백")).isEqualTo("청모랑은…");
        assertThat(EffectLine.render("{대상}가3", "청모랑", "한백")).isEqualTo("청모랑가3");
    }

    @Test
    void allowedTails() {
        assertThat(EffectLine.render("{대상}로써 막는다", "청모랑", "한백")).isEqualTo("청모랑으로써 막는다");
        assertThat(EffectLine.render("{대상}으로서 선다", "들토끼", "한백")).isEqualTo("들토끼로서 선다");
        assertThat(EffectLine.render("{대상}로부터!", "청모랑", "한백")).isEqualTo("청모랑으로부터!");
        assertThat(EffectLine.render("{대상}와의 싸움", "청모랑", "한백")).isEqualTo("청모랑과의 싸움");
        assertThat(EffectLine.render("{시전자}과도 같다", "청모랑", "여소이")).isEqualTo("여소이와도 같다");
        assertThat(EffectLine.render("{대상}이도 운다", "청모랑", "한백")).isEqualTo("청모랑이도 운다"); // 이/가 꼬리 없음
    }
}
