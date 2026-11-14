package mudmk3.domain.character;

import mudmk3.domain.shared.Element;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * 캐릭터의 영근: 오행 1~5개(겹침 없음)와 각 자질. 엔트리는 오행 순으로 갖는다.
 * 이름은 개수 1 천영근, 2~3 진영근, 4~5 위영근이다
 */
public record SpiritRoot(List<SpiritRootEntry> entries) {

    public static final int MAX_ELEMENTS = 5;

    public SpiritRoot {
        requireNonNull(entries);
        if (entries.isEmpty() || entries.size() > MAX_ELEMENTS) {
            throw new IllegalArgumentException("영근은 오행 1~5개입니다: " + entries.size());
        }
        Set<Element> elements = EnumSet.noneOf(Element.class);
        for (SpiritRootEntry entry : entries) {
            if (!elements.add(requireNonNull(entry).element())) {
                throw new IllegalArgumentException("영근에 같은 오행이 두 번 있습니다: " + entry.element().koreanName());
            }
        }
        entries = entries.stream().sorted(Comparator.comparing(SpiritRootEntry::element)).toList();
    }

    public static SpiritRoot of(List<SpiritRootEntry> entries) {
        return new SpiritRoot(entries);
    }

    /**
     * 공백으로 나눈 낱말이 모두 오행 이름이고 1~5개, 겹침 없음일 때만 값이 있다.
     * 처음 자질은 오행마다 {@code 100.0 / 개수}
     */
    public static Optional<SpiritRoot> parse(String input) {
        String trimmed = requireNonNull(input).strip();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        String[] words = trimmed.split("\\s+");
        if (words.length > MAX_ELEMENTS) {
            return Optional.empty();
        }
        Set<Element> elements = EnumSet.noneOf(Element.class);
        for (String word : words) {
            Optional<Element> element = Element.fromKorean(word);
            if (element.isEmpty() || !elements.add(element.get())) {
                return Optional.empty();
            }
        }
        double aptitude = 100.0 / elements.size();
        return Optional.of(new SpiritRoot(elements.stream()
                .map(element -> new SpiritRootEntry(element, aptitude))
                .toList()));
    }

    /** {@code 목·화 진영근}, {@code 화 천영근}, {@code 금·목·수·화 위영근} */
    public String name() {
        String elements = entries.stream()
                .map(entry -> entry.element().koreanName())
                .collect(Collectors.joining("·"));
        return elements + " " + kindName();
    }

    /** {@code 목 50, 화 50} — 자질은 버린 정수 */
    public String summary() {
        return entries.stream()
                .map(entry -> entry.element().koreanName() + " " + (int) Math.floor(entry.aptitude()))
                .collect(Collectors.joining(", "));
    }

    public boolean has(Element element) {
        return entries.stream().anyMatch(entry -> entry.element() == element);
    }

    /** 그 오행의 자질(소수 그대로). 그 오행이 영근에 없으면 0 */
    public double aptitude(Element element) {
        return entries.stream()
                .filter(entry -> entry.element() == element)
                .mapToDouble(SpiritRootEntry::aptitude)
                .findFirst()
                .orElse(0);
    }

    private String kindName() {
        if (entries.size() == 1) {
            return "천영근";
        }
        return entries.size() <= 3 ? "진영근" : "위영근";
    }
}
