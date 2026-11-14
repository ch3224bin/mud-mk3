package mudmk3.application.cultivation.provided;

/** 옥간·서책을 읽어 공법을 익힌다 */
public interface TechniqueReader {

    /**
     * 그 종류의 물건 하나를 읽는다. 물건이 소지품에 있는지는 부르는 쪽이 확인한다.
     * 검사 순서: 옥간·서책인가 → 담긴 공법 → 이미 익힘 → 시작 경지 → (오행공법만) 영근 → 1성 자질 조건.
     * 처음 걸린 것만 돌려주고 아무것도 바꾸지 않는다. 성공하면 한 트랜잭션에서 물건 1개를 빼고 1성으로 익히며,
     * 범인이 시작 경지 범인인 공법을 읽으면 연기 1층이 된다(경지 알림 이벤트는 내지 않는다)
     */
    ReadResult read(Long characterId, Long itemTypeId);
}
