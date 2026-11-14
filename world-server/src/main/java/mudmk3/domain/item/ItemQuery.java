package mudmk3.domain.item;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 명령에서 물건을 가리키는 이름과 번호. 번호 0은 번호 없음이다({@code 2.가죽} → {@code (가죽, 2)}).
 * 이름이 영석 등급 낱말 하나면 그 영석의 전체 이름으로 바꾼다({@code 하품} → {@code 하품 영석})
 */
public record ItemQuery(String name, int ordinal) {

    private static final Pattern ORDINAL_PATTERN = Pattern.compile("^(\\d{1,3})\\.(.+)$");

    public ItemQuery {
        requireNonNull(name);
    }

    public static ItemQuery parse(String text) {
        requireNonNull(text);
        Matcher matcher = ORDINAL_PATTERN.matcher(text);
        if (matcher.matches()) {
            int ordinal = Integer.parseInt(matcher.group(1));
            if (ordinal >= 1) return new ItemQuery(fullName(matcher.group(2)), ordinal);
        }
        return new ItemQuery(fullName(text), 0);
    }

    public boolean hasOrdinal() {
        return ordinal > 0;
    }

    private static String fullName(String name) {
        return SpiritStoneGrade.fromWord(name).map(SpiritStoneGrade::itemName).orElse(name);
    }
}
