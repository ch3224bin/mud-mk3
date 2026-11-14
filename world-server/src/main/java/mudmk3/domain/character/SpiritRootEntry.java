package mudmk3.domain.character;

import mudmk3.domain.shared.Element;

import static java.util.Objects.requireNonNull;

/** 영근 하나: 오행과 그 자질(소수 그대로 저장하고 화면에는 버려서 보인다) */
public record SpiritRootEntry(Element element, double aptitude) {

    public SpiritRootEntry {
        requireNonNull(element);
        if (!Double.isFinite(aptitude) || aptitude < 0) {
            throw new IllegalArgumentException("영근 자질은 0 이상이어야 합니다: " + aptitude);
        }
    }
}
