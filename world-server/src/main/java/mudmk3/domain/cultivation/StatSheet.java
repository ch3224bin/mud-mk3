package mudmk3.domain.cultivation;

import mudmk3.domain.character.QiBlood;
import mudmk3.domain.character.SpiritRoot;
import mudmk3.domain.character.SpiritRootEntry;
import mudmk3.domain.shared.Element;
import mudmk3.domain.shared.Realm;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * 캐릭터 스탯 최대치와 영근 자질, 현재 기혈. 최대치·자질은 저장하지 않고 경지·영근·익힌 공법으로 계산하고,
 * 현재 기혈은 저장된 깎인 기혈로 구한다(기혈 밖의 현재 수치는 최대치와 같다).
 * 자질은 영근의 오행만 오행 순으로 갖는다(소수 그대로)
 */
public record StatSheet(int qiBlood, double qiBloodCurrent, int mana, int sense, int essence, int spirit, int body,
                        int strength,
                        Map<Element, Double> aptitudes) {

    public StatSheet {
        aptitudes = Collections.unmodifiableMap(aptitudes.isEmpty()
                ? new EnumMap<>(Element.class)
                : new EnumMap<>(aptitudes));
    }

    /**
     * 공법 X = 익힌 공법마다 1성~지금 단계 증가의 합. 육신·근력·영력·의식·정혈 = 경지 값 + 공법 값, 기혈 = 육신 × 10,
     * 법력 = 영력이 0이면 0, 아니면 영력 × 5 + 공법 법력. 영근 자질 = 처음 자질 + 공법 속성에 그 오행이 든 익힌 공법의
     * 영근 자질 증가 합. 영근을 아직 정하지 않았으면({@code root} null) 자질은 비어 있다. 현재 기혈 =
     * {@link QiBlood#current}(깎인 기혈, 기혈)
     */
    public static StatSheet of(Realm realm, @Nullable SpiritRoot root, List<CharacterTechnique> learned,
                               double qiBloodLost) {
        requireNonNull(realm);
        StageBonusSum total = StageBonusSum.ZERO;
        Map<Element, Double> aptitudes = new EnumMap<>(Element.class);
        if (root != null) {
            for (SpiritRootEntry entry : root.entries()) {
                aptitudes.put(entry.element(), entry.aptitude());
            }
        }
        for (CharacterTechnique technique : learned) {
            StageBonusSum sum = technique.bonusSum();
            total = total.plus(sum);
            for (Element element : technique.getTechnique().getElements()) {
                aptitudes.computeIfPresent(element, (key, aptitude) -> aptitude + sum.aptitudeGain());
            }
        }
        int body = realm.body() + total.body();
        int strength = realm.strength() + total.strength();
        int spirit = realm.spirit() + total.spirit();
        int sense = realm.senseEssence() + total.sense();
        int essence = realm.senseEssence() + total.essence();
        int mana = spirit == 0 ? 0 : spirit * 5 + total.mana();
        int qiBlood = body * 10;
        return new StatSheet(qiBlood, QiBlood.current(qiBloodLost, qiBlood), mana, sense, essence, spirit,
                body, strength, aptitudes);
    }

    /** 그 오행의 자질(소수 그대로). 영근에 없으면 0 */
    public double aptitude(Element element) {
        return aptitudes.getOrDefault(element, 0.0);
    }
}
