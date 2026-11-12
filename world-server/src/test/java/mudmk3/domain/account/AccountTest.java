package mudmk3.domain.account;

import mudmk3.domain.shared.Email;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.account.AccountFixture.createAccount;
import static mudmk3.domain.account.AccountFixture.createAccountRegisterInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void register() {
        Account account = Account.register(createAccountRegisterInfo("google-sub-1", "Hanli@Example.com"));

        assertThat(account.getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(account.getEmail()).isEqualTo(new Email("hanli@example.com"));
        assertThat(account.getId()).isNull();
    }

    @Test
    void registerBlankSubjectFail() {
        assertThatThrownBy(() -> Account.register(createAccountRegisterInfo(" ", "hanli@example.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Account.register(new AccountRegisterInfo("google-sub-1", null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeEmail() {
        Account account = createAccount();

        account.changeEmail(new Email("hanbaek@example.com"));

        assertThat(account.getEmail().address()).isEqualTo("hanbaek@example.com");
    }

    @Test
    void changeEmailNullFail() {
        Account account = createAccount();

        assertThatThrownBy(() -> account.changeEmail(null)).isInstanceOf(NullPointerException.class);
    }
}
