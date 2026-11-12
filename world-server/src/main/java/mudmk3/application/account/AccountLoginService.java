package mudmk3.application.account;

import lombok.RequiredArgsConstructor;
import mudmk3.application.account.provided.AccountLogin;
import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.application.account.provided.AccountPrincipal;
import mudmk3.application.account.required.AccountRepository;
import mudmk3.domain.account.Account;
import mudmk3.domain.account.AccountRegisterInfo;
import mudmk3.domain.shared.Email;
import mudmk3.support.config.AuthProperties;
import mudmk3.support.stereotype.ValidatedApplicationService;

import static java.util.Objects.requireNonNull;

@ValidatedApplicationService
@RequiredArgsConstructor
public class AccountLoginService implements AccountLogin {

    private final AccountRepository accountRepository;
    private final AuthProperties authProperties;

    @Override
    public AccountPrincipal login(AccountLoginRequest request) {
        Email email = new Email(request.email());
        Account account = accountRepository.findByGoogleSubject(request.googleSubject())
                .map(found -> {
                    found.changeEmail(email);
                    return found;
                })
                .orElseGet(() -> Account.register(new AccountRegisterInfo(request.googleSubject(), email)));
        Account saved = accountRepository.save(account);
        return new AccountPrincipal(requireNonNull(saved.getId()), email.address(), authProperties.isAdmin(email.address()));
    }
}
