package mudmk3.domain.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.domain.shared.Email;

/** 계정을 만드는 입력. googleSubject 는 Google의 sub(개발용 로그인은 {@code dev:<email>}) */
public record AccountRegisterInfo(
        @NotBlank @Size(max = Account.GOOGLE_SUBJECT_MAX_LENGTH) String googleSubject,
        @NotNull Email email
) {
}
