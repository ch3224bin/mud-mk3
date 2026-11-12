package mudmk3.application.account.provided;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import mudmk3.ApplicationServiceTest;
import mudmk3.domain.account.Account;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.account.AccountFixture.createAccountLoginRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 테스트 설정의 관리자 이메일은 admin@example.com 이다 */
@ApplicationServiceTest
record AccountLoginTest(AccountLogin accountLogin, AccountFinder accountFinder, EntityManager entityManager) {

    @Test
    void loginCreatesAccount() {
        AccountPrincipal principal = accountLogin.login(createAccountLoginRequest("google-sub-1", "Hanli@Example.com"));
        entityManager.flush();
        entityManager.clear();

        assertThat(principal.accountId()).isNotNull();
        assertThat(principal.email()).isEqualTo("hanli@example.com");
        assertThat(principal.getName()).isEqualTo(String.valueOf(principal.accountId()));
        Account account = accountFinder.find(principal.accountId());
        assertThat(account.getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(account.getEmail().address()).isEqualTo("hanli@example.com");
    }

    @Test
    void loginReusesAccountAndUpdatesEmail() {
        AccountPrincipal first = accountLogin.login(createAccountLoginRequest("google-sub-1", "hanli@example.com"));

        AccountPrincipal second = accountLogin.login(createAccountLoginRequest("google-sub-1", "hanbaek@example.com"));
        entityManager.flush();
        entityManager.clear();

        assertThat(second.accountId()).isEqualTo(first.accountId());
        assertThat(second.email()).isEqualTo("hanbaek@example.com");
        assertThat(accountFinder.find(first.accountId()).getEmail().address()).isEqualTo("hanbaek@example.com");
    }

    @Test
    void adminEmailGetsAdmin() {
        assertThat(accountLogin.login(createAccountLoginRequest("google-sub-1", "admin@example.com")).admin()).isTrue();
        assertThat(accountLogin.login(createAccountLoginRequest("google-sub-2", "ADMIN@example.com")).admin()).isTrue();
    }

    @Test
    void nonAdminEmail() {
        assertThat(accountLogin.login(createAccountLoginRequest("google-sub-1", "hanli@example.com")).admin()).isFalse();
    }

    @Test
    void devSubjectAccount() {
        AccountPrincipal principal = accountLogin.login(createAccountLoginRequest("dev:a@example.com", "a@example.com"));

        assertThat(accountFinder.find(principal.accountId()).getGoogleSubject()).isEqualTo("dev:a@example.com");
        assertThat(principal.admin()).isFalse();
    }

    @Test
    void invalidRequestFail() {
        assertThatThrownBy(() -> accountLogin.login(createAccountLoginRequest(" ", "hanli@example.com")))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> accountLogin.login(createAccountLoginRequest("google-sub-1", "not-an-email")))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void findMissingAccountFail() {
        assertThatThrownBy(() -> accountFinder.find(999L)).isInstanceOf(IllegalArgumentException.class);
    }
}
