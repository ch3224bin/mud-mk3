package mudmk3.adapter.webapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 개발용 로그인 요청 */
public record DevLoginRequest(@NotBlank @Email String email) {
}
