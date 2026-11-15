package mudmk3.domain.combat;

/**
 * 몹이 교전 상대 하나에게 쌓은 기록: 위협, 그 상대가 준 피해(층을 거치기 전 위력의 합), 처음 피해를 준 순번(아직 없으면 −1).
 * 처치자는 피해가 가장 많은 쪽, 같으면 순번이 작은 쪽이다(룰북 처치 보상 귀속)
 */
public record Engagement(double threat, double dealt, long firstDealtSeq) {

    static final long NOT_DEALT = -1;

    static Engagement start(double threat) {
        return new Engagement(threat, 0, NOT_DEALT);
    }

    /** 위력만큼 위협과 피해를 더한다. 처음이면 순번을 적는다 */
    Engagement add(double power, long seq) {
        return new Engagement(threat + power, dealt + power, hasDealt() ? firstDealtSeq : seq);
    }

    Engagement withThreatAtLeast(double minimum) {
        return new Engagement(Math.max(threat, minimum), dealt, firstDealtSeq);
    }

    public boolean hasDealt() {
        return firstDealtSeq != NOT_DEALT;
    }
}
