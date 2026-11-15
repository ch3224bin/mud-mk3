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
}
