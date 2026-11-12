package mudmk3.application.room.provided;

/** 방이 지워졌다(지우는 트랜잭션 안에서 동기로 알린다) */
public record RoomDeletedEvent(Long roomId) {
}
