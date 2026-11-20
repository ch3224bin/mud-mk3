package mudmk3.application.character.provided;

/**
 * 캐릭터를 지운다. 캐릭터 행을 지우기 <b>전에</b>, 지우는 트랜잭션 안에서 동기로 알린다(FK 때문에 캐릭터를 가리키는 행을
 * 먼저 지우게 — 익힌 공법·깨달음)
 */
public record CharacterDeletedEvent(Long characterId) {
}
