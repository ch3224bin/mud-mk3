package mudmk3.application.account.required;

import jakarta.persistence.EntityManager;
import mudmk3.domain.account.Account;
import mudmk3.domain.account.AccountFixture;
import mudmk3.domain.shared.Email;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
record AccountRepositoryTest(AccountRepository accountRepository, EntityManager entityManager) {

    @Test
    void save() {
        Account account = accountRepository.save(AccountFixture.createAccount("google-sub-1", "hanli@example.com"));
        entityManager.flush();
        entityManager.clear();

        Account found = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(found.getGoogleSubject()).isEqualTo("google-sub-1");
        assertThat(found.getEmail()).isEqualTo(new Email("hanli@example.com"));
    }

    @Test
    void findByGoogleSubject() {
        accountRepository.save(AccountFixture.createAccount("google-sub-1", "hanli@example.com"));
        accountRepository.save(AccountFixture.createAccount("google-sub-2", "hanbaek@example.com"));
        entityManager.flush();
        entityManager.clear();

        assertThat(accountRepository.findByGoogleSubject("google-sub-2"))
                .map(account -> account.getEmail().address())
                .contains("hanbaek@example.com");
        assertThat(accountRepository.findByGoogleSubject("google-sub-3")).isEmpty();
    }

    @Test
    void duplicateGoogleSubjectFail() {
        accountRepository.save(AccountFixture.createAccount("google-sub-1", "hanli@example.com"));

        assertThatThrownBy(() -> {
            accountRepository.save(AccountFixture.createAccount("google-sub-1", "other@example.com"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
