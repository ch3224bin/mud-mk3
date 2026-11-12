package mudmk3.application.account.required;

import mudmk3.domain.account.Account;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** 계정을 저장하고 찾는다 */
public interface AccountRepository extends Repository<Account, Long> {

    Account save(Account account);

    Optional<Account> findById(Long id);

    Optional<Account> findByGoogleSubject(String googleSubject);
}
