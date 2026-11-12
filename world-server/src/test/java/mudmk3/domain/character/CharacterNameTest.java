package mudmk3.domain.character;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CharacterNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"한백", "여섯글자이름"})
    void create(String value) {
        assertThat(new CharacterName(value).value()).isEqualTo(value);
        assertThat(CharacterName.isValid(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"한", "일곱글자인이름", "한 백", "Han", "한백1", "ㅎㅂ", "", " 한백"})
    void invalidFail(String value) {
        assertThatThrownBy(() -> new CharacterName(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.");
        assertThat(CharacterName.isValid(value)).isFalse();
    }

    @Test
    void nullFail() {
        assertThatThrownBy(() -> new CharacterName(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(CharacterName.isValid(null)).isFalse();
    }
}
