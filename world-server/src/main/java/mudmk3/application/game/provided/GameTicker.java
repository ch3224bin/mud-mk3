package mudmk3.application.game.provided;

/** 서버 틱 하나를 처리한다 */
public interface GameTicker {

    /** 지금 시각으로 틱 하나(배치 맞추기·행동·다시 차기·시체 사라짐·재등장)를 처리한다. 게임 락 안에서 돈다 */
    void tick();
}
