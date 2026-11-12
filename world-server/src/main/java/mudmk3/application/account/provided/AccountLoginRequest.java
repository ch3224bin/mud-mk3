package mudmk3.application.account.provided;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mudmk3.domain.account.Account;

/** Google(또는 개발용) 로그인으로 확인한 계정 정보 */
public record AccountLoginRequest(
        @NotBlank @Size(max = Account.GOOGLE_SUBJECT_MAX_LENGTH) String googleSubject,
        @NotBlank @Email @Size(max = mudmk3.domain.shared.Email.MAX_LENGTH) String email
) {
}
