package mudmk3.domain.shared;

import java.util.Arrays;
import java.util.Optional;

/** 오행. 선언 순서가 오행 순서(금 목 수 화 토)다 */
public enum Element {
    METAL("금"),
    WOOD("목"),
    WATER("수"),
    FIRE("화"),
    EARTH("토");

    private final String koreanName;

    Element(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 한글 이름(금·목·수·화·토)에 맞는 오행. 맞는 것이 없으면 비어 있다 */
    public static Optional<Element> fromKorean(String name) {
        return Arrays.stream(values())
                .filter(element -> element.koreanName.equals(name))
                .findFirst();
    }
}
