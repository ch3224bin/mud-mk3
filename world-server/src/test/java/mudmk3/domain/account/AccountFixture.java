package mudmk3.domain.account;

import mudmk3.application.account.provided.AccountLoginRequest;
import mudmk3.domain.shared.Email;

public class AccountFixture {

    public static AccountRegisterInfo createAccountRegisterInfo(String googleSubject, String email) {
        return new AccountRegisterInfo(googleSubject, new Email(email));
    }

    public static Account createAccount() {
        return createAccount("google-sub-1", "hanli@example.com");
    }

    public static Account createAccount(String googleSubject, String email) {
        return Account.register(createAccountRegisterInfo(googleSubject, email));
    }

    public static AccountLoginRequest createAccountLoginRequest(String googleSubject, String email) {
        return new AccountLoginRequest(googleSubject, email);
    }
}
