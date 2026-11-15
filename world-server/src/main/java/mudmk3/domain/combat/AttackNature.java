package mudmk3.domain.combat;

import mudmk3.domain.shared.Element;

import java.util.List;

/**
 * 공격의 성질: 신통 속성(오행)과 연체 신통인지. 받는 쪽 배율({@link Damage#receiverMultiplier})이 본다.
 * 5차 임시 공격·몹 공격은 {@link #NONE}
 */
public record AttackNature(List<Element> elements, boolean bodyRefining) {

    /** 속성 없는 오행 아닌 공격(5차 임시 공격·몹 공격) */
    public static final AttackNature NONE = new AttackNature(List.of(), false);

    public AttackNature {
        elements = List.copyOf(elements);
    }
}
