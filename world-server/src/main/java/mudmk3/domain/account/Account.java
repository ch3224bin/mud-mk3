package mudmk3.domain.account;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import mudmk3.domain.shared.Email;

import static java.util.Objects.requireNonNull;

/** 계정: Google 계정(sub) 하나에 하나. 이메일은 로그인할 때마다 Google 값으로 맞춘다 */
@Entity
@Getter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends AbstractEntity {

    public static final int GOOGLE_SUBJECT_MAX_LENGTH = 255;

    private String googleSubject;

    private Email email;

    public static Account register(AccountRegisterInfo info) {
        requireNonNull(info);
        String googleSubject = requireNonNull(info.googleSubject()).strip();
        if (googleSubject.isEmpty() || googleSubject.length() > GOOGLE_SUBJECT_MAX_LENGTH) {
            throw new IllegalArgumentException("Google 계정 식별자가 바르지 않습니다");
        }
        Account account = new Account();
        account.googleSubject = googleSubject;
        account.email = requireNonNull(info.email());
        return account;
    }

    public void changeEmail(Email email) {
        this.email = requireNonNull(email);
    }
}
