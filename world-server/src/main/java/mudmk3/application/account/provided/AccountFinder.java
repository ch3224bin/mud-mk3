package mudmk3.application.account.provided;

import mudmk3.domain.account.Account;

/** 계정을 찾는 기능을 제공한다 */
public interface AccountFinder {

    Account find(Long accountId);
}
