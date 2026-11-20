package mudmk3.application.character.provided;

/** 지운 캐릭터(삭제 기록용) */
public record DeletedCharacter(Long id, Long accountId, String name) {
}
