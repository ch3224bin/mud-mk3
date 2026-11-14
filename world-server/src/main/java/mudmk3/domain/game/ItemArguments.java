package mudmk3.domain.game;

import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * 물건 명령의 인자: 물건 이름(번호·영석 등급 낱말 포함)과 수량(D46).
 * 마지막 낱말이 {@code 전부} 면 모두, 수면 수량(1~999,999,999, 0이나 10자리 이상은 잘못된 수량), 나머지가 이름이다.
 * 이름이 비면 {@code item} 은 null, 수량을 빼면 1개
 */
public record ItemArguments(@Nullable ItemQuery item, Amount amount) {

    private static final Pattern NUMBER = Pattern.compile("^\\d+$");
    private static final String ALL_WORD = "전부";
    private static final int MAX_DIGITS = 9;

    public ItemArguments {
        requireNonNull(amount);
    }

    public static ItemArguments parse(String text) {
        String normalized = ItemNames.normalize(requireNonNull(text));
        if (normalized.isEmpty()) return new ItemArguments(null, Amount.one());

        int space = normalized.lastIndexOf(' ');
        String lastWord = normalized.substring(space + 1);
        String rest = space < 0 ? "" : normalized.substring(0, space);

        Amount amount;
        String name;
        if (lastWord.equals(ALL_WORD)) {
            amount = new Amount.All();
            name = rest;
        } else if (NUMBER.matcher(lastWord).matches()) {
            amount = countOf(lastWord);
            name = rest;
        } else {
            amount = Amount.one();
            name = normalized;
        }
        return new ItemArguments(name.isEmpty() ? null : ItemQuery.parse(name), amount);
    }

    /** 수량을 적었는지(마지막 낱말이 수나 {@code 전부}). 수량을 빼면 다른 뜻인 명령({@code 값})이 쓴다 */
    public static boolean hasAmount(String text) {
        String normalized = ItemNames.normalize(requireNonNull(text));
        if (normalized.indexOf(' ') < 0) return false;
        String lastWord = normalized.substring(normalized.lastIndexOf(' ') + 1);
        return lastWord.equals(ALL_WORD) || NUMBER.matcher(lastWord).matches();
    }

    private static Amount countOf(String digits) {
        String significant = digits.replaceFirst("^0+", "");
        if (significant.isEmpty() || significant.length() > MAX_DIGITS) return new Amount.Invalid();
        return new Amount.Count(Integer.parseInt(significant));
    }
}
