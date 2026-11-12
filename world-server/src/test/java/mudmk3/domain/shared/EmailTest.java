package mudmk3.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizeToLowerCase() {
        assertThat(new Email(" Admin@Example.COM ").address()).isEqualTo("admin@example.com");
    }

    @Test
    void sameAddressIgnoringCaseIsEqual() {
        assertThat(new Email("Admin@Example.COM")).isEqualTo(new Email("admin@example.com"));
    }

    @Test
    void invalidFormatFail() {
        assertThatThrownBy(() -> new Email("not-an-email")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("a b@example.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Email("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidFormatMessageHidesAddressFail() {
        assertThatThrownBy(() -> new Email("secret mail@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이메일 형식이 바르지 않습니다");
    }

    @Test
    void tooLongFail() {
        String local = "a".repeat(64);
        String domain = "b".repeat(260) + ".com";

        assertThatThrownBy(() -> new Email(local + "@" + domain)).isInstanceOf(IllegalArgumentException.class);
    }
}
