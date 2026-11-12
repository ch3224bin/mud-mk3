package mudmk3.application.account.provided;

import jakarta.validation.Valid;

/** Google 또는 개발용 로그인으로 계정을 찾거나 만든다 */
public interface AccountLogin {

    /** subject로 계정을 찾고 없으면 만든다. 이메일이 바뀌었으면 고친다 */
    AccountPrincipal login(@Valid AccountLoginRequest request);
}
