package mudmk3.application.account;

import lombok.RequiredArgsConstructor;
import mudmk3.application.account.provided.AccountFinder;
import mudmk3.application.account.required.AccountRepository;
import mudmk3.domain.account.Account;
import mudmk3.support.stereotype.ApplicationService;

@ApplicationService
@RequiredArgsConstructor
public class AccountQueryService implements AccountFinder {

    private final AccountRepository accountRepository;

    @Override
    public Account find(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다. id: " + accountId));
    }
}
