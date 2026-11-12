package mudmk3.domain.character;

import jakarta.validation.constraints.NotNull;

/** 캐릭터를 만드는 입력: 주인 계정, 이름, 처음 설 방 */
public record CharacterCreateInfo(
        @NotNull Long accountId,
        @NotNull CharacterName name,
        @NotNull Long roomId
) {
}
